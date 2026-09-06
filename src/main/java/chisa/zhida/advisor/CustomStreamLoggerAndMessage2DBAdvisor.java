package chisa.zhida.advisor;

import chisa.zhida.chat.ChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/** Logs a streaming answer and stores the user/assistant pair in one transaction. */
public final class CustomStreamLoggerAndMessage2DBAdvisor implements StreamAdvisor {
    private static final Logger log = LoggerFactory.getLogger(CustomStreamLoggerAndMessage2DBAdvisor.class);

    private final ChatRepository repository;
    private final TransactionTemplate transactionTemplate;
    private final String chatId;
    private final String userMessage;

    public CustomStreamLoggerAndMessage2DBAdvisor(ChatRepository repository,
                                                   TransactionTemplate transactionTemplate,
                                                   String chatId,
                                                   String userMessage) {
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
        this.chatId = chatId;
        this.userMessage = userMessage;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        AtomicReference<StringBuilder> fullContent = new AtomicReference<>(new StringBuilder());
        AtomicReference<String> fullReasoning = new AtomicReference<>("");
        return chain.nextStream(request)
                .doOnNext(response -> collect(response, fullContent, fullReasoning))
                .doOnComplete(() -> {
                    String answer = fullContent.get().toString();
                    String reasoning = fullReasoning.get();
                    log.info("\n==== 智答ai FULL Reasoning RESPONSE ====\n{}\n=====================================", reasoning);
                    log.info("\n==== 智答ai FULL AI RESPONSE ====\n{}\n===============================", answer);
                    transactionTemplate.executeWithoutResult(status -> {
                        repository.saveMessage(chatId, "user", userMessage, null);
                        repository.saveMessage(chatId, "assistant", answer, reasoning.isBlank() ? null : reasoning);
                    });
                })
                .doOnError(error -> log.error("## 智答ai 流式回答异常，已收集回答: {}", fullContent.get(), error));
    }

    private void collect(ChatClientResponse response, AtomicReference<StringBuilder> content,
                         AtomicReference<String> reasoning) {
        if (response.chatResponse() == null || response.chatResponse().getResult() == null) return;
        AssistantMessage output = response.chatResponse().getResult().getOutput();
        if (output == null) return;
        if (output.getText() != null) content.get().append(output.getText());
        Object value = output.getMetadata().getOrDefault("reasoningContent", output.getMetadata().get("thinking"));
        if (value != null && !value.toString().isBlank()) {
            String current = value.toString();
            String previous = reasoning.get();
            reasoning.set(current.startsWith(previous) ? current : previous + current);
        }
    }

    @Override
    public int getOrder() { return 99; }

    @Override
    public String getName() { return getClass().getSimpleName(); }
}
