package chisa.zhida.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank(message = "用户消息不能为空") String message) {}
