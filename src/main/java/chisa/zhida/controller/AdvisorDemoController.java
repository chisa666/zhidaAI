package chisa.zhida.controller;

import chisa.zhida.advisor.CustomStreamLoggerAdvisor;
import chisa.zhida.advisor.NetworkSearchAdvisor;
import chisa.zhida.chat.AiResponse;
import chisa.zhida.chat.NetworkSearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** Direct verification endpoint for the capability's custom streaming Advisor chain. */
@RestController
@Profile("ollama")
@RequestMapping("/api/lab/advisor")
public class AdvisorDemoController {
    private final ChatClient chatClient;
    private final NetworkSearchService networkSearchService;

    public AdvisorDemoController(ChatClient chatClient, NetworkSearchService networkSearchService) {
        this.chatClient = chatClient;
        this.networkSearchService = networkSearchService;
    }

    @GetMapping(value = "/network/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AiResponse> network(@RequestParam String message,
                                    @RequestParam(defaultValue = "advisor-demo") String chatId) {
        return chatClient.prompt()
                .system("你是智答ai，作者是 chisa。联网信息必须优先使用搜索上下文。")
                .user(message)
                .options(OllamaChatOptions.builder().numGPU(0).numCtx(2048).numPredict(256).disableThinking())
                .advisors(new NetworkSearchAdvisor(networkSearchService), new CustomStreamLoggerAdvisor())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream().chatClientResponse()
                .<AiResponse>handle((response, sink) -> {
                    if (response.chatResponse() == null || response.chatResponse().getResult() == null) return;
                    var output = response.chatResponse().getResult().getOutput();
                    if (output == null) return;
                    Object reasoning = output.getMetadata().getOrDefault("reasoningContent", output.getMetadata().get("thinking"));
                    if (reasoning != null && !reasoning.toString().isBlank()) sink.next(AiResponse.reasoning(reasoning.toString()));
                    if (output.getText() != null && !output.getText().isBlank()) sink.next(AiResponse.text(output.getText()));
                })
                .concatWith(Flux.just(AiResponse.end()));
    }
}
