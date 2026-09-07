package chisa.zhida.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {
    private final String model;

    public HomeController(@Value("${zhida.ollama.model:qwen3:1.7b}") String model) {
        this.model = model;
    }

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "success", true,
                "name", "智答ai",
                "author", "chisa",
                "model", model,
                "api", "/api/health"
        );
    }
}