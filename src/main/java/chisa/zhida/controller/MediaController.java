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
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Capability media examples. DashScope calls are enabled only when an API key is configured. */
@RestController
public class MediaController {
    private final String apiKey;
    private final Path mediaRoot;
    private final java.util.Map<String, OpenAiChatModel> openAiModels;

    public MediaController(@Value("${zhida.aliyun.api-key:}") String apiKey,
                            @Value("${zhida.storage-path:./data}") String storagePath,
                            java.util.Map<String, OpenAiChatModel> openAiModels) {
        this.apiKey = apiKey;
        this.mediaRoot = Path.of(storagePath).toAbsolutePath().resolve("media");
        this.openAiModels = openAiModels;
        try { Files.createDirectories(mediaRoot); } catch (IOException ignored) { }
    }

    @GetMapping("/v10/ai/text2img")
    public Map<String, Object> text2Image(@RequestParam String prompt) {
        if (apiKey.isBlank()) return Map.of("success", false, "message", "未配置 DashScope API Key");
        ImageGenerationMessage message = ImageGenerationMessage.builder().role("user")
                .content(Collections.singletonList(Collections.singletonMap("text", prompt))).build();
        ImageGenerationParam param = ImageGenerationParam.builder().apiKey(apiKey).model("wan2.6-t2i")
                .n(1).size("1280*1280").negativePrompt("").promptExtend(true).watermark(false)
                .messages(Collections.singletonList(message)).build();
        try {
            ImageGenerationResult result = new ImageGeneration().call(param);
            Map<String, Object> response = resultMap(result.getRequestId(), result.getCode(), result.getMessage());
            String remoteUrl = firstUrl(JsonUtils.toJson(result));
            response.put("remoteUrl", remoteUrl);
            if (remoteUrl != null) response.putAll(saveRemote(remoteUrl, "result-image-" + System.currentTimeMillis() + ".png"));
            return response;
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @GetMapping("/v11/ai/text2audio")
    public Map<String, Object> text2Audio(@RequestParam String prompt) {
        if (apiKey.isBlank()) return Map.of("success", false, "message", "未配置 DashScope API Key");
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(SpeechSynthesisParam.builder()
                .apiKey(apiKey).model("cosyvoice-v3-flash").voice("longanhuan_v3").build(), null);
        try {
            ByteBuffer audio = synthesizer.call(prompt);
            if (audio == null) return Map.of("success", false, "message", "模型未返回音频");
            Path target = mediaRoot.resolve("result-audio-" + System.currentTimeMillis() + ".mp3");
            byte[] bytes = new byte[audio.remaining()]; audio.get(bytes); Files.write(target, bytes);
            return mediaMap(true, target, synthesizer.getLastRequestId());
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        } finally {
            try { synthesizer.getDuplexApi().close(1000, "bye"); } catch (Exception ignored) { }
        }
    }

    @GetMapping("/v12/ai/text2video")
    public Map<String, Object> text2Video(@RequestParam String prompt,
                             @RequestParam(required = false) String imagePath) {
        if (apiKey.isBlank()) return Map.of("success", false, "message", "未配置 DashScope API Key");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("prompt_extend", true);
        try {
            VideoSynthesisParam param = VideoSynthesisParam.builder().apiKey(apiKey).model("wanx2.1-i2v-plus")
                    .prompt(prompt).imgUrl(imagePath == null || imagePath.isBlank() ? null : Path.of(imagePath).toUri().toString())
                    .extraInputs(parameters).build();
            VideoSynthesisResult result = new VideoSynthesis().call(param);
            Map<String, Object> response = resultMap(result.getRequestId(), result.getCode(), result.getMessage());
            if (result.getOutput() != null) {
                response.put("taskId", result.getOutput().getTaskId());
                response.put("taskStatus", result.getOutput().getTaskStatus());
                String remoteUrl = result.getOutput().getVideoUrl(); response.put("remoteUrl", remoteUrl);
                if (remoteUrl != null && !remoteUrl.isBlank()) response.putAll(saveRemote(remoteUrl, "result-video-" + System.currentTimeMillis() + ".mp4"));
            }
            return response;
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @GetMapping("/v10/ai/media/{fileName:.+}")
    public ResponseEntity<Resource> media(@org.springframework.web.bind.annotation.PathVariable String fileName) {
        Path target = mediaRoot.resolve(fileName).normalize();
        if (!target.startsWith(mediaRoot) || !Files.isRegularFile(target)) return ResponseEntity.notFound().build();
        String contentType;
        try { contentType = Files.probeContentType(target); } catch (IOException e) { contentType = null; }
        Resource resource = new FileSystemResource(target);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType)).body(resource);
    }

    @GetMapping(value = "/v9/ai/generateStream", produces = "text/event-stream")
    public Flux<String> multimodal(@RequestParam String message,
                                   @RequestParam(required = false) String imagePath) {
        OpenAiChatModel chatModel = openAiModels.getOrDefault("aliyunChatModel", openAiModels.get("zhipuChatModel"));
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

    private Map<String, Object> resultMap(String requestId, String code, String message) {
        Map<String, Object> map = new LinkedHashMap<>(); map.put("success", code == null || code.isBlank());
        map.put("requestId", requestId); if (code != null) map.put("code", code); if (message != null) map.put("message", message); return map;
    }

    private Map<String, Object> mediaMap(boolean success, Path target, String requestId) {
        Map<String, Object> map = new LinkedHashMap<>(); map.put("success", success); map.put("path", target.toString());
        map.put("url", "/v10/ai/media/" + target.getFileName()); map.put("requestId", requestId); return map;
    }

    private String firstUrl(String json) {
        Matcher matcher = Pattern.compile("https?://[^\\\"\\\\]+", Pattern.CASE_INSENSITIVE).matcher(json);
        return matcher.find() ? matcher.group().replace("\\\\/", "/") : null;
    }

    private Map<String, Object> saveRemote(String remoteUrl, String fileName) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(remoteUrl)).GET().build();
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) return Map.of("downloadStatus", response.statusCode());
        Path target = mediaRoot.resolve(fileName); Files.write(target, response.body()); return mediaMap(true, target, null);
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
