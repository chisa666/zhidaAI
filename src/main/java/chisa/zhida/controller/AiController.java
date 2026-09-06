package chisa.zhida.controller;

import chisa.zhida.ai.AiModelService;
import chisa.zhida.ai.CloudModelService;
import chisa.zhida.chat.AiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class AiController {
    private final AiModelService ai;
    private final String model;
    private final CloudModelService cloud;
    public AiController(AiModelService ai, CloudModelService cloud, @Value("${zhida.ollama.model:qwen3:1.7b}") String model) { this.ai = ai; this.cloud = cloud; this.model = model; }

    @GetMapping("/health") public java.util.Map<String, Object> health() {
        return java.util.Map.of("success", true, "name", "智答ai", "author", "chisa", "model", model);
    }

    @GetMapping("/ai/generate")
    public String generate(@RequestParam(defaultValue = "你是谁？") String message) { return ai.generate(message, model, 0.7, "你是智答ai，作者是 chisa。"); }

    @GetMapping(value = "/ai/generateStream", produces = "text/html;charset=UTF-8")
    public Flux<String> generateStream(@RequestParam(defaultValue = "你是谁？") String message) {
        return ai.stream(message, model, 0.7, "你是智答ai，作者是 chisa。", java.util.List.of()).filter(r -> r.v() != null).map(AiResponse::v);
    }

    @GetMapping("/v3/ai/generate") public String ollama(@RequestParam(defaultValue = "你是谁？") String message) { return generate(message); }
    @GetMapping(value = "/v3/ai/generateStream", produces = "text/html;charset=UTF-8") public Flux<String> ollamaStream(@RequestParam(defaultValue = "你是谁？") String message) { return generateStream(message); }
    @GetMapping("/v2/ai/generate") public String deepSeek(@RequestParam(defaultValue = "你是谁？") String message) { return cloud.generate("deepseek", message, null); }
    @GetMapping(value = "/v2/ai/generateStream", produces = "text/html;charset=UTF-8") public Flux<String> deepSeekStream(@RequestParam(defaultValue = "你是谁？") String message) { return cloud.stream("deepseek", message, null, 0.7).filter(r -> r.v() != null).map(AiResponse::v); }
    @GetMapping("/v5/ai/generate") public String openAi(@RequestParam(defaultValue = "你是谁？") String message) { return cloud.generate("openai", message, null); }
    @GetMapping(value = "/v5/ai/generateStream", produces = "text/html;charset=UTF-8") public Flux<String> openAiStream(@RequestParam(defaultValue = "你是谁？") String message) { return cloud.stream("openai", message, null, 0.7).filter(r -> r.v() != null).map(AiResponse::v); }
    @GetMapping("/v6/ai/generate") public String bailian(@RequestParam(defaultValue = "你是谁？") String message) { return cloud.generate("bailian", message, null); }
    @GetMapping(value = "/v6/ai/generateStream", produces = "text/html;charset=UTF-8") public Flux<String> bailianStream(@RequestParam(defaultValue = "你是谁？") String message) { return cloud.stream("bailian", message, null, 0.7).filter(r -> r.v() != null).map(AiResponse::v); }
}
