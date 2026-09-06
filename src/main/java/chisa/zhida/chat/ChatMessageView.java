package chisa.zhida.chat;

public record ChatMessageView(Long id, String chatId, String role, String content, String reasoning, String createTime) {}
