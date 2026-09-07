package chisa.zhida.tools;

import org.springframework.ai.tool.annotation.Tool;

import java.time.LocalDateTime;

/** Tool Calling example used by the application. */
public class DateTimeTools {
    @Tool(description = "获取当前日期和时间")
    public String getCurrentDateTime() {
        return LocalDateTime.now().toString();
    }
}
