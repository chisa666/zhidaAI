package chisa.zhida.controller;

import chisa.zhida.ai.AiModelService;
import chisa.zhida.chat.AiResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/** Explicit Cassandra ChatMemory demo matching the capability's conversation-id flow. */
@RestController
@RequestMapping("/api/lab/chat-memory")
public class ChatMemoryController {
    private final ObjectProvider<ChatMemory> memories;
    private final AiModelService ai;
    private final String model;

    public ChatMemoryController(ObjectProvider<ChatMemory> memories, AiModelService ai,
                                @Value("${zhida.ollama.model:qwen3:1.7b}") String model) {
        this.memories = memories;
        this.ai = ai;
        this.model = model;
    }

    @GetMapping("/status")
    public Map<String, Object> status(@RequestParam(defaultValue = "demo") String conversationId) {
        ChatMemory memory = memories.getIfAvailable();
        if (memory == null) return Map.of("available", false, "message", "Cassandra ChatMemory 未初始化");
        return Map.of("available", true, "conversationId", conversationId, "messages", memory.get(conversationId).size());
    }

    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AiResponse> generateStream(@RequestParam String message,
                                            @RequestParam String conversationId) {
        ChatMemory memory = memories.getIfAvailable();
        if (memory == null) return Flux.just(AiResponse.text("Cassandra ChatMemory 未初始化"), AiResponse.end());
        List<Map<String, String>> history = memory.get(conversationId).stream()
                .map(this::toMap).toList();
        memory.add(conversationId, new UserMessage(message));
        StringBuilder answer = new StringBuilder();
        return ai.stream(message, model, 0.7, "你是智答ai，作者是 chisa。", history)
                .doOnNext(item -> { if (item.v() != null) answer.append(item.v()); })
                .doOnComplete(() -> { if (!answer.isEmpty()) memory.add(conversationId, new AssistantMessage(answer.toString())); });
    }

    private Map<String, String> toMap(Message message) {
        String role = message instanceof AssistantMessage ? "assistant" : message instanceof UserMessage ? "user" : "system";
        return Map.of("role", role, "content", message.getText());
    }
}
