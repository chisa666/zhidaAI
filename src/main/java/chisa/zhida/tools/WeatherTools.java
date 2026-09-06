package chisa.zhida.tools;

import org.springframework.ai.tool.annotation.Tool;

/** Deterministic weather tool used by the Tool Calling capability. */
public class WeatherTools {
    @Tool(description = "获取当日的天气情况，时间参数需为 ISO-8601 格式")
    public String getWeather(String time) {
        return "今天天气晴朗，最低温度 18℃，最高温度 38℃（示例工具数据）";
    }
}
