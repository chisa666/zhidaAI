package chisa.zhida.ai;

import chisa.zhida.chat.AiResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** Selects the real Spring AI cloud model configured by the active profile. */
@Service
public class CloudModelService {
    private final ObjectProvider<DeepSeekChatModel> deepSeek;
    private final ObjectProvider<OpenAiChatModel> openAi;
    private final String deepSeekModel;
    private final String openAiModel;

    public CloudModelService(ObjectProvider<DeepSeekChatModel> deepSeek,
                             ObjectProvider<OpenAiChatModel> openAi,
                             @Value("${spring.ai.deepseek.chat.options.model:deepseek-chat}") String deepSeekModel,
                             @Value("${spring.ai.openai.chat.options.model:gpt-4o}") String openAiModel) {
        this.deepSeek = deepSeek; this.openAi = openAi;
        this.deepSeekModel = deepSeekModel; this.openAiModel = openAiModel;
    }

    public Flux<AiResponse> stream(String provider, String message, String model, double temperature) {
        ChatModel chatModel = switch (provider == null ? "" : provider.toLowerCase()) {
            case "deepseek" -> deepSeek.getIfAvailable();
            case "openai", "bailian", "qwen" -> openAi.getIfAvailable();
            default -> null;
        };
        if (chatModel == null) {
            return Flux.just(AiResponse.text("未配置 " + provider + " 模型，请切换对应 profile 并设置 API Key"), AiResponse.end());
        }
        String selectedModel = model == null || model.isBlank() ?
                ("deepseek".equalsIgnoreCase(provider) ? deepSeekModel : openAiModel) : model;
        ChatOptions options = switch (provider == null ? "" : provider.toLowerCase()) {
            case "openai", "bailian", "qwen" -> OpenAiChatOptions.builder()
                    .model(selectedModel).temperature(temperature).build();
            default -> ChatOptions.builder().model(selectedModel).temperature(temperature).build();
        };
        Prompt prompt = new Prompt(new UserMessage(message), options);
        return chatModel.stream(prompt).concatMap(response -> {
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return Flux.empty();
            }
            AssistantMessage output = response.getResult().getOutput();
            java.util.ArrayList<AiResponse> events = new java.util.ArrayList<>();
            Object reasoning = output.getMetadata().getOrDefault("reasoningContent", output.getMetadata().get("thinking"));
            if (reasoning != null && !String.valueOf(reasoning).isBlank()) events.add(AiResponse.reasoning(String.valueOf(reasoning)));
            if (output.getText() != null && !output.getText().isBlank()) events.add(AiResponse.text(output.getText()));
            return Flux.fromIterable(events);
        }).concatWith(Flux.just(AiResponse.end()));
    }

    public String generate(String provider, String message, String model) {
        StringBuilder answer = new StringBuilder();
        stream(provider, message, model, 0.7).filter(item -> item.v() != null).doOnNext(item -> answer.append(item.v())).blockLast();
        return answer.toString();
    }
}
