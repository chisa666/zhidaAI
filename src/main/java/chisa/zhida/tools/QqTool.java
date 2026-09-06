package chisa.zhida.tools;

import org.springframework.ai.tool.annotation.Tool;

import java.util.Map;

/** Local MCP server tool from the capability, kept deterministic for offline development. */
public class QqTool {
    @Tool(description = "根据 QQ 号获取 QQ 信息")
    public Map<String, Object> getQqInfo(String qq) {
        String value = qq == null ? "" : qq.trim();
        return Map.of("qq", value, "nickname", "智答用户", "source", "zhida-ai MCP demo");
    }
}
