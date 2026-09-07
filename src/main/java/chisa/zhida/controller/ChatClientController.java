package chisa.zhida.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

/** ChatClient facade corresponding to the application's synchronous and streaming examples. */
@RestController
@Profile("ollama")
@RequestMapping("/api/lab/chat-client")
public class ChatClientController {
    private final ChatClient chatClient;

    public ChatClientController(ChatClient chatClient) { this.chatClient = chatClient; }

    @GetMapping("/generate")
    public String generate(@RequestParam(defaultValue = "你是谁？") String message,
                           @RequestParam(defaultValue = "lab-chat-client") String chatId) {
        return chatClient.prompt().user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().content();
    }

    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(@RequestParam(defaultValue = "你是谁？") String message,
                                       @RequestParam(defaultValue = "lab-chat-client") String chatId) {
        return chatClient.prompt().user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream().content();
    }
}
