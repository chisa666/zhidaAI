package chisa.zhida.config;

import chisa.zhida.tools.QqTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers capability tools for both Spring AI Tool Calling and the MCP server starter. */
@Configuration
public class ToolConfig {
    @Bean
    public QqTool qqTool() { return new QqTool(); }

    @Bean
    public ToolCallbackProvider qqTools(QqTool qqTool) {
        return MethodToolCallbackProvider.builder().toolObjects(qqTool).build();
    }
}
