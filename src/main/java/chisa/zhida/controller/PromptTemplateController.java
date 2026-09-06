package chisa.zhida.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

import java.util.Map;

/** PromptTemplate examples from the prompt engineering chapters. */
@RestController
@Profile("ollama")
@RequestMapping("/api/lab/prompt")
public class PromptTemplateController {
    private final ChatClient chatClient;

    public PromptTemplateController(ChatClient chatClient) { this.chatClient = chatClient; }

    @GetMapping("/template")
    public String template(@RequestParam(defaultValue = "Java") String topic,
                           @RequestParam(defaultValue = "lab-prompt") String chatId) {
        PromptTemplate template = new PromptTemplate("请用三句话介绍 {topic}，面向初学者，不要添加无关内容。");
        return chatClient.prompt().user(template.render(Map.of("topic", topic)))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().content();
    }

    @GetMapping("/role")
    public String role(@RequestParam(defaultValue = "如何学习 Spring AI？") String question,
                       @RequestParam(defaultValue = "lab-prompt-role") String chatId) {
        SystemPromptTemplate system = new SystemPromptTemplate("你是一名 {role}，回答要给出可执行步骤。");
        return chatClient.prompt()
                .system(system.render(Map.of("role", "Spring AI 实战导师")))
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }
}
