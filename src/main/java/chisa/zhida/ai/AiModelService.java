package chisa.zhida.ai;

import chisa.zhida.chat.AiResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 基于 Spring AI OllamaChatModel 的模型调用服务。
 * 模型请求、流式响应和模型参数均由 Spring AI 负责处理。
 */
@Service
public class AiModelService {
    private final OllamaChatModel chatModel;
    private final String defaultModel;

    public AiModelService(OllamaChatModel chatModel,
                          @Value("${zhida.ollama.model:qwen3:1.7b}") String defaultModel) {
        this.chatModel = chatModel;
        this.defaultModel = defaultModel;
    }

    public Flux<AiResponse> stream(String message, String model, double temperature, String context,
                                   List<Map<String, String>> history) {
        String selectedModel = StringUtils.defaultIfBlank(model, defaultModel);
        String promptText = buildPrompt(message, context, history);
        Prompt prompt = new Prompt(new UserMessage(promptText), OllamaChatOptions.builder()
                .model(selectedModel)
                .temperature(temperature)
                .numGPU(0)
                .numCtx(2048)
                .numPredict(512)
                .build());

        return chatModel.stream(prompt)
                .<AiResponse>handle((response, sink) -> {
                    if (response == null || response.getResult() == null) {
                        return;
                    }
                    AssistantMessage output = response.getResult().getOutput();
                    if (output == null) {
                        return;
                    }
                    String reasoning = metadataText(output, "reasoningContent");
                    if (StringUtils.isBlank(reasoning)) {
                        reasoning = metadataText(output, "thinking");
                    }
                    if (StringUtils.isNotBlank(reasoning)) {
                        sink.next(AiResponse.reasoning(reasoning));
                    }
                    if (StringUtils.isNotBlank(output.getText())) {
                        sink.next(AiResponse.text(output.getText()));
                    }
                })
                .concatWith(Flux.just(AiResponse.end()));
    }

    public String generate(String message, String model, double temperature, String context) {
        StringBuilder answer = new StringBuilder();
        stream(message, model, temperature, context, List.of())
                .filter(response -> response.v() != null)
                .doOnNext(response -> answer.append(response.v()))
                .blockLast();
        return answer.toString();
    }

    private String buildPrompt(String message, String context, List<Map<String, String>> history) {
        StringBuilder prompt = new StringBuilder();
        if (StringUtils.isNotBlank(context)) {
            prompt.append("系统提示：\n").append(context).append("\n\n");
        }
        if (history != null && !history.isEmpty()) {
            prompt.append("历史消息：\n");
            history.forEach(item -> prompt.append(item.getOrDefault("role", "user"))
                    .append(": ").append(item.getOrDefault("content", "")).append("\n"));
            prompt.append("\n");
        }
        prompt.append(message);
        return prompt.toString();
    }

    private String metadataText(AssistantMessage message, String key) {
        Object value = message.getMetadata().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
