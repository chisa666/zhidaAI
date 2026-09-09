package chisa.zhida.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/** MCP client entry point; external stdio servers are opt-in through configuration. */
@RestController
@Profile("ollama")
@RequestMapping("/api/mcp")
public class McpController {
    private final ChatClient chatClient;
    private final List<ToolCallbackProvider> providers;
    private final boolean mcpEnabled;

    public McpController(ChatClient chatClient, ObjectProvider<ToolCallbackProvider> providers,
                         @org.springframework.beans.factory.annotation.Value("${spring.ai.mcp.client.enabled:false}") boolean mcpEnabled) {
        this.chatClient = chatClient;
        this.providers = providers.orderedStream().toList();
        this.mcpEnabled = mcpEnabled;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        int tools = providers.stream().mapToInt(provider -> provider.getToolCallbacks().length).sum();
        return Map.of("success", true, "providers", providers.size(), "tools", tools,
                "amapEnabled", mcpEnabled);
    }

    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(@RequestParam String message,
                                       @RequestParam(defaultValue = "mcp-demo") String chatId) {
        List<ToolCallback> callbacks = providers.stream()
                .flatMap(provider -> java.util.Arrays.stream(provider.getToolCallbacks()))
                .toList();
        return chatClient.prompt()
                .system("你是智答ai。需要外部信息时优先调用已注册的 MCP 工具。")
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(callbacks)
                .stream()
                .content();
    }
}
