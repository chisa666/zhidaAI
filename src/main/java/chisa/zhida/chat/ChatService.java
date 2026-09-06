package chisa.zhida.chat;

import chisa.zhida.advisor.CustomChatMemoryAdvisor;
import chisa.zhida.advisor.CustomStreamLoggerAndMessage2DBAdvisor;
import chisa.zhida.advisor.NetworkSearchAdvisor;
import chisa.zhida.common.PageResponse;
import chisa.zhida.common.Response;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
public class ChatService {
    private final ChatRepository repository;
    private final ChatClient chatClient;
    private final NetworkSearchService networkSearchService;
    private final TransactionTemplate transactionTemplate;
    private final String defaultModel;

    public ChatService(ChatRepository repository, OllamaChatModel chatModel,
                       NetworkSearchService networkSearchService, TransactionTemplate transactionTemplate,
                       @Value("${zhida.ollama.model:qwen3:1.7b}") String defaultModel) {
        this.repository = repository;
        this.chatClient = ChatClient.create(chatModel);
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

        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .system("你是智答ai，作者是 chisa。回答要准确、简洁，支持 Markdown。")
                .user(request.message())
                .options(OllamaChatOptions.builder()
                        .model(request.modelOrDefault(defaultModel))
                        .temperature(request.temperatureOrDefault(0.7))
                        .numGPU(0).numCtx(2048).numPredict(512).disableThinking());

        if (Boolean.TRUE.equals(request.networkSearch())) {
            spec.advisors(new NetworkSearchAdvisor(networkSearchService));
        } else {
            spec.advisors(new CustomChatMemoryAdvisor(repository, finalChatId, 50));
        }
        spec.advisors(new CustomStreamLoggerAndMessage2DBAdvisor(repository, transactionTemplate,
                finalChatId, request.message()));

        return spec.stream().chatClientResponse()
                .<AiResponse>handle((response, sink) -> {
                    if (response.chatResponse() == null || response.chatResponse().getResult() == null) return;
                    AssistantMessage output = response.chatResponse().getResult().getOutput();
                    if (output == null) return;
                    Object reasoning = output.getMetadata().getOrDefault("reasoningContent", output.getMetadata().get("thinking"));
                    if (reasoning != null && !reasoning.toString().isBlank()) sink.next(AiResponse.reasoning(reasoning.toString()));
                    if (output.getText() != null && !output.getText().isBlank()) sink.next(AiResponse.text(output.getText()));
                })
                .concatWith(Flux.just(AiResponse.end()));
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
