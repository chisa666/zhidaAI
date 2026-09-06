package chisa.zhida.advisor;

import chisa.zhida.chat.ChatMessageView;
import chisa.zhida.chat.ChatRepository;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/** Loads the latest PostgreSQL conversation messages before a streaming model call. */
public final class CustomChatMemoryAdvisor implements StreamAdvisor {
    private final ChatRepository repository;
    private final String chatId;
    private final int limit;

    public CustomChatMemoryAdvisor(ChatRepository repository, String chatId, int limit) {
        this.repository = repository;
        this.chatId = chatId;
        this.limit = limit;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        List<Message> messages = new ArrayList<>();
        Prompt original = request.prompt();
        messages.addAll(original.getSystemMessages());
        for (ChatMessageView item : repository.recentMessages(chatId, limit)) {
            if ("user".equalsIgnoreCase(item.role())) {
                messages.add(new UserMessage(item.content()));
            } else if ("assistant".equalsIgnoreCase(item.role())) {
                messages.add(new AssistantMessage(item.content()));
            }
        }
        original.getUserMessages().stream().reduce((left, right) -> right).ifPresent(messages::add);
        Prompt processed = new Prompt(messages, original.getOptions());
        return chain.nextStream(request.mutate().prompt(processed).build());
    }

    @Override
    public int getOrder() { return 2; }

    @Override
    public String getName() { return getClass().getSimpleName(); }
}
