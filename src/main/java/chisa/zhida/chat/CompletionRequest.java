package chisa.zhida.chat;

import jakarta.validation.constraints.NotBlank;

public record CompletionRequest(@NotBlank(message = "用户消息不能为空") String message,
                                String chatId,
                                String modelName,
                                Boolean networkSearch,
                                Double temperature) {
    public String modelOrDefault(String fallback) { return modelName == null || modelName.isBlank() ? fallback : modelName; }
    public double temperatureOrDefault(double fallback) { return temperature == null ? fallback : temperature; }
}
