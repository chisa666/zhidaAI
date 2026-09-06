package chisa.zhida.controller;

import com.alibaba.dashscope.aigc.imagegeneration.ImageGeneration;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationMessage;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationParam;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesis;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisParam;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.utils.JsonUtils;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Capability media examples. DashScope calls are enabled only when an API key is configured. */
@RestController
public class MediaController {
    private final String apiKey;
    private final Path mediaRoot;
    private final ObjectProvider<OpenAiChatModel> openAiModels;

    public MediaController(@Value("${spring.ai.openai.api-key:}") String apiKey,
                            @Value("${zhida.storage-path:./data}") String storagePath,
                            ObjectProvider<OpenAiChatModel> openAiModels) {
        this.apiKey = apiKey;
        this.mediaRoot = Path.of(storagePath).toAbsolutePath().resolve("media");
        this.openAiModels = openAiModels;
        try { Files.createDirectories(mediaRoot); } catch (IOException ignored) { }
    }

    @GetMapping("/v10/ai/text2img")
    public String text2Image(@RequestParam String prompt) {
        if (apiKey.isBlank()) return JsonUtils.toJson(Map.of("success", false, "message", "未配置 DashScope API Key"));
        ImageGenerationMessage message = ImageGenerationMessage.builder().role("user")
                .content(Collections.singletonList(Collections.singletonMap("text", prompt))).build();
        ImageGenerationParam param = ImageGenerationParam.builder().apiKey(apiKey).model("wan2.6-t2i")
                .n(1).size("1280*1280").negativePrompt("").promptExtend(true).watermark(false)
                .messages(Collections.singletonList(message)).build();
        try {
            ImageGenerationResult result = new ImageGeneration().call(param);
            return JsonUtils.toJson(result);
        } catch (Exception e) {
            return JsonUtils.toJson(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/v11/ai/text2audio")
    public String text2Audio(@RequestParam String prompt) {
        if (apiKey.isBlank()) return JsonUtils.toJson(Map.of("success", false, "message", "未配置 DashScope API Key"));
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(SpeechSynthesisParam.builder()
                .apiKey(apiKey).model("cosyvoice-v3-flash").voice("longanhuan_v3").build(), null);
        try {
            ByteBuffer audio = synthesizer.call(prompt);
            if (audio == null) return JsonUtils.toJson(Map.of("success", false, "message", "模型未返回音频"));
            Path target = mediaRoot.resolve("result-audio-" + System.currentTimeMillis() + ".mp3");
            Files.write(target, audio.array());
            return JsonUtils.toJson(Map.of("success", true, "path", target.toString(),
                    "requestId", synthesizer.getLastRequestId()));
        } catch (Exception e) {
            return JsonUtils.toJson(Map.of("success", false, "message", e.getMessage()));
        } finally {
            try { synthesizer.getDuplexApi().close(1000, "bye"); } catch (Exception ignored) { }
        }
    }

    @GetMapping("/v12/ai/text2video")
    public String text2Video(@RequestParam String prompt,
                             @RequestParam(required = false) String imagePath) {
        if (apiKey.isBlank()) return JsonUtils.toJson(Map.of("success", false, "message", "未配置 DashScope API Key"));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("prompt_extend", true);
        try {
            VideoSynthesisParam param = VideoSynthesisParam.builder().apiKey(apiKey).model("wanx2.1-i2v-plus")
                    .prompt(prompt).imgUrl(imagePath == null || imagePath.isBlank() ? null : Path.of(imagePath).toUri().toString())
                    .extraInputs(parameters).build();
            VideoSynthesisResult result = new VideoSynthesis().call(param);
            return JsonUtils.toJson(result);
        } catch (Exception e) {
            return JsonUtils.toJson(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping(value = "/v9/ai/generateStream", produces = "text/event-stream")
    public Flux<String> multimodal(@RequestParam String message,
                                   @RequestParam(required = false) String imagePath) {
        OpenAiChatModel chatModel = openAiModels.getIfAvailable();
        if (chatModel == null) return Flux.just("未配置支持多模态的 OpenAI/百炼模型，请使用 dev profile 并配置 OPENAI_API_KEY");
        if (imagePath == null || imagePath.isBlank() || !Files.isRegularFile(Path.of(imagePath))) {
            return Flux.just("图片文件不存在，请通过 imagePath 指定本地图片路径");
        }
        Media image = new Media(MimeTypeUtils.IMAGE_PNG, new FileSystemResource(imagePath));
        UserMessage userMessage = UserMessage.builder().text(message).media(image)
                .metadata(Map.of("temperature", 0.7)).build();
        return chatModel.stream(new Prompt(List.of(userMessage))).mapNotNull(response -> {
            Generation generation = response.getResult();
            return generation == null || generation.getOutput() == null ? null : generation.getOutput().getText();
        });
    }

    @PostMapping(value = "/v9/ai/generateStream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> multimodalUpload(@RequestParam String message, @RequestPart MultipartFile image) {
        if (image == null || image.isEmpty()) return Flux.just("图片不能为空");
        try {
            Path target = mediaRoot.resolve("upload-" + System.currentTimeMillis() + ".png");
            image.transferTo(target);
            return multimodal(message, target.toString());
        } catch (IOException e) {
            return Flux.just("图片保存失败: " + e.getMessage());
        }
    }
}
