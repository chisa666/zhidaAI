package chisa.zhida.chat;

import chisa.zhida.advisor.CustomChatMemoryAdvisor;
import chisa.zhida.advisor.CustomStreamLoggerAndMessage2DBAdvisor;
import chisa.zhida.advisor.NetworkSearchAdvisor;
import chisa.zhida.common.PageResponse;
import chisa.zhida.common.Response;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
public class ChatService {
    private final ChatRepository repository;
    private final OllamaChatModel ollamaModel;
    private final ObjectProvider<OpenAiChatModel> openAiModels;
    private final NetworkSearchService networkSearchService;
    private final TransactionTemplate transactionTemplate;
    private final String defaultModel;

    public ChatService(ChatRepository repository,
                       OllamaChatModel ollamaModel,
                       ObjectProvider<OpenAiChatModel> openAiModels,
                       NetworkSearchService networkSearchService,
                       TransactionTemplate transactionTemplate,
                       @Value("${zhida.ollama.model:qwen3:1.7b}") String defaultModel) {
        this.repository = repository;
        this.ollamaModel = ollamaModel;
        this.openAiModels = openAiModels;
        this.networkSearchService = networkSearchService;
        this.transactionTemplate = transactionTemplate;
        this.defaultModel = defaultModel;
    }

    public Response<ChatSummary> newChat(String message) {
        String uuid = UUID.randomUUID().toString();
        String summary = truncate(message, 36);
        repository.createChat(uuid, summary);
        return Response.success(new ChatSummary(uuid, summary, null, null));
    }

    public Response<Void> rename(String chatId, String summary) {
        if (!repository.chatExists(chatId)) return Response.fail("对话不存在");
        repository.renameChat(chatId, truncate(summary, 80));
        return Response.success();
    }

    public Response<Void> delete(String chatId) {
        repository.deleteChat(chatId);
        return Response.success();
    }

    public PageResponse<ChatSummary> list(long current, long size) {
        current = Math.max(1, current); size = Math.min(Math.max(1, size), 100);
        return PageResponse.of(repository.listChats(current, size), repository.countChats(), current, size);
    }

    public PageResponse<ChatMessageView> messages(String chatId, long current, long size) {
        current = Math.max(1, current); size = Math.min(Math.max(1, size), 100);
        return PageResponse.of(repository.listMessages(chatId, current, size), repository.countMessages(chatId), current, size);
    }

    public Flux<AiResponse> completion(CompletionRequest request) {
        String chatId = request.chatId();
        if (chatId == null || chatId.isBlank() || !repository.chatExists(chatId)) {
            chatId = UUID.randomUUID().toString();
            repository.createChat(chatId, truncate(request.message(), 36));
        }
        final String finalChatId = chatId;
        String selectedModel = request.modelOrDefault(defaultModel);
        boolean cloud = isCloudModel(selectedModel);
        ChatClient chatClient;
        ChatClient.ChatClientRequestSpec spec;
        if (cloud) {
            OpenAiChatModel model = openAiModels.getIfAvailable();
            if (model == null) {
                return Flux.just(AiResponse.text("阿里云模型未启用。请在 IDEA 环境变量中设置 ZHIDA_ALIYUN_ENABLED=true、OPENAI_API_KEY 和 OPENAI_BASE_URL。"), AiResponse.end());
            }
            chatClient = ChatClient.create(model);
            spec = chatClient.prompt()
                    .system("你是智答ai，作者是 chisa。回答要准确、简洁，支持 Markdown。")
                    .user(request.message())
                    .options(OpenAiChatOptions.builder().model(selectedModel)
                            .temperature(request.temperatureOrDefault(0.7)));
        } else {
            chatClient = ChatClient.create(ollamaModel);
            spec = chatClient.prompt()
                    .system("你是智答ai，作者是 chisa。回答要准确、简洁，支持 Markdown。")
                    .user(request.message())
                    .options(OllamaChatOptions.builder().model(selectedModel)
                            .temperature(request.temperatureOrDefault(0.7))
                            .numGPU(0).numCtx(2048).numPredict(512).disableThinking());
        }
        if (Boolean.TRUE.equals(request.networkSearch())) {
            spec.advisors(new NetworkSearchAdvisor(networkSearchService));
        } else {
            spec.advisors(new CustomChatMemoryAdvisor(repository, finalChatId, 50));
        }
        spec.advisors(new CustomStreamLoggerAndMessage2DBAdvisor(repository, transactionTemplate,
                finalChatId, request.message()));

        StringBuilder emittedReasoning = new StringBuilder();
        return spec.stream().chatClientResponse()
                .concatMap(response -> {
                    if (response.chatResponse() == null || response.chatResponse().getResult() == null) return Flux.empty();
                    AssistantMessage output = response.chatResponse().getResult().getOutput();
                    if (output == null) return Flux.empty();
                    java.util.ArrayList<AiResponse> events = new java.util.ArrayList<>();
                    Object reasoning = output.getMetadata().getOrDefault("reasoningContent", output.getMetadata().get("thinking"));
                    if (reasoning != null && !reasoning.toString().isBlank()) {
                        String currentReasoning = reasoning.toString();
                        synchronized (emittedReasoning) {
                            String previousReasoning = emittedReasoning.toString();
                            String delta = currentReasoning.startsWith(previousReasoning)
                                    ? currentReasoning.substring(previousReasoning.length())
                                    : currentReasoning;
                            emittedReasoning.setLength(0);
                            emittedReasoning.append(currentReasoning);
                            if (!delta.isBlank()) events.add(AiResponse.reasoning(delta));
                        }
                    }
                    if (output.getText() != null && !output.getText().isBlank()) events.add(AiResponse.text(output.getText()));
                    return Flux.fromIterable(events);
                })
                .concatWith(Flux.just(AiResponse.end()));
    }

    private boolean isCloudModel(String model) {
        String value = model == null ? "" : model.toLowerCase();
        return value.startsWith("qwen3.8") || value.startsWith("qwen3.5")
                || value.startsWith("qwen-image") || value.startsWith("wan")
                || value.startsWith("cosyvoice");
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
