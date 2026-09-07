package chisa.zhida.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("t_chat_message")
public class ChatMessageEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String chatId;
    private String role;
    private String content;
    private String reasoningContent;
    private LocalDateTime createTime;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChatId() { return chatId; }
    public void setChatId(String value) { this.chatId = value; }
    public String getRole() { return role; }
    public void setRole(String value) { this.role = value; }
    public String getContent() { return content; }
    public void setContent(String value) { this.content = value; }
    public String getReasoningContent() { return reasoningContent; }
    public void setReasoningContent(String value) { this.reasoningContent = value; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime value) { this.createTime = value; }
}
