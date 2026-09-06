package chisa.zhida.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;

/** Structured output examples using Spring AI's output converters. */
@RestController
@Profile("ollama")
@RequestMapping("/api/lab/structured")
public class StructuredOutputController {
    private final ChatClient chatClient;

    public StructuredOutputController(ChatClient chatClient) { this.chatClient = chatClient; }

    @GetMapping("/actor-films")
    public ActorFilmography actorFilms(@RequestParam(defaultValue = "周星驰") String name,
                                       @RequestParam(defaultValue = "structured-actor") String chatId) {
        BeanOutputConverter<ActorFilmography> converter = new BeanOutputConverter<>(ActorFilmography.class);
        String prompt = "请为演员 " + name + " 生成5部代表作，只输出 JSON，不要解释。\n" + converter.getFormat();
        return chatClient.prompt().user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().entity(converter);
    }

    @GetMapping("/language-info")
    public Map<String, Object> languageInfo(@RequestParam(defaultValue = "Java") String language,
                                            @RequestParam(defaultValue = "structured-language") String chatId) {
        MapOutputConverter converter = new MapOutputConverter();
        String prompt = "请提供编程语言 " + language + " 的 name、popularity、features、releaseYear，直接输出 JSON。\n" + converter.getFormat();
        return chatClient.prompt().user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().entity(converter);
    }

    @GetMapping("/city-list")
    public List<String> cityList(@RequestParam(defaultValue = "中国") String country,
                                 @RequestParam(defaultValue = "structured-city") String chatId) {
        ListOutputConverter converter = new ListOutputConverter();
        String prompt = "请列出 " + country + " 的5个主要城市，只输出 JSON 字符串数组。\n" + converter.getFormat();
        return chatClient.prompt().user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().entity(converter);
    }

    public record ActorFilmography(String actor, List<String> movies) {}
}
