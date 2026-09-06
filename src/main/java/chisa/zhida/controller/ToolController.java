package chisa.zhida.controller;

import chisa.zhida.tools.DateTimeTools;
import chisa.zhida.tools.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** Spring AI Tool Calling demonstration using the local Ollama model. */
@RestController
@RequestMapping("/api/tools")
public class ToolController {
    private final ChatClient chatClient;

    public ToolController(OllamaChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(@RequestParam(defaultValue = "今天是几号，天气怎么样？") String message) {
        return chatClient.prompt()
                .tools(new DateTimeTools(), new WeatherTools())
                .options(OllamaChatOptions.builder().numGPU(0).numCtx(2048).numPredict(256).disableThinking())
                .system("你是智答ai，作者是 chisa。需要实时日期或天气时必须调用工具。")
                .user(message)
                .stream()
                .content();
    }
}
