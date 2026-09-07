package chisa.zhida.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ChatRepository {
    private final ChatMapper chatMapper;
    private final ChatMessageMapper messageMapper;
    public ChatRepository(ChatMapper chatMapper, ChatMessageMapper messageMapper) {
        this.chatMapper = chatMapper;
        this.messageMapper = messageMapper;
    }

    public void createChat(String uuid, String summary) {
        LocalDateTime now = LocalDateTime.now();
        ChatEntity entity = new ChatEntity();
        entity.setUuid(uuid); entity.setSummary(summary);
        entity.setCreateTime(now); entity.setUpdateTime(now);
        chatMapper.insert(entity);
    }

    public boolean chatExists(String uuid) {
        return chatMapper.selectCount(new LambdaQueryWrapper<ChatEntity>().eq(ChatEntity::getUuid, uuid)) > 0;
    }

    public void renameChat(String uuid, String summary) {
        ChatEntity entity = new ChatEntity();
        entity.setSummary(summary); entity.setUpdateTime(LocalDateTime.now());
        chatMapper.update(entity, new LambdaUpdateWrapper<ChatEntity>().eq(ChatEntity::getUuid, uuid));
    }

    public void deleteChat(String uuid) {
        messageMapper.delete(new LambdaQueryWrapper<ChatMessageEntity>().eq(ChatMessageEntity::getChatId, uuid));
        chatMapper.delete(new LambdaQueryWrapper<ChatEntity>().eq(ChatEntity::getUuid, uuid));
    }

    public List<ChatSummary> listChats(long current, long size) {
        Page<ChatEntity> page = chatMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<ChatEntity>().orderByDesc(ChatEntity::getUpdateTime));
        return page.getRecords().stream().map(entity -> new ChatSummary(entity.getUuid(), entity.getSummary(),
                entity.getCreateTime().toString(), entity.getUpdateTime().toString())).toList();
    }

    public long countChats() { return chatMapper.selectCount(null); }

    public Long saveMessage(String chatId, String role, String content, String reasoning) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setChatId(chatId); entity.setRole(role); entity.setContent(content);
        entity.setReasoningContent(reasoning); entity.setCreateTime(LocalDateTime.now());
        messageMapper.insert(entity);
        return entity.getId();
    }

    public List<ChatMessageView> listMessages(String chatId, long current, long size) {
        Page<ChatMessageEntity> page = messageMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getChatId, chatId)
                        .orderByDesc(ChatMessageEntity::getCreateTime));
        return page.getRecords().stream().map(this::toView).toList();
    }

    public long countMessages(String chatId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getChatId, chatId));
    }

    public List<ChatMessageView> recentMessages(String chatId, int limit) {
        Page<ChatMessageEntity> page = messageMapper.selectPage(new Page<>(1, limit, false),
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getChatId, chatId)
                        .orderByDesc(ChatMessageEntity::getCreateTime));
        return page.getRecords().stream().map(this::toView).toList().reversed();
    }

    private ChatMessageView toView(ChatMessageEntity entity) {
        return new ChatMessageView(entity.getId(), entity.getChatId(), entity.getRole(),
                entity.getContent(), entity.getReasoningContent(), entity.getCreateTime().toString());
    }
}
