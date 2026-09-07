package chisa.zhida.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeControllerTest {
    @Test
    void rootReturnsApplicationStatus() {
        var result = new HomeController("qwen3:1.7b").home();

        assertTrue((Boolean) result.get("success"));
        assertEquals("智答ai", result.get("name"));
        assertEquals("chisa", result.get("author"));
        assertEquals("qwen3:1.7b", result.get("model"));
        assertEquals("/api/health", result.get("api"));
    }
}