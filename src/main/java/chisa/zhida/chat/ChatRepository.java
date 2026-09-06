package chisa.zhida.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ChatRepository {
    private final JdbcTemplate jdbc;
    public ChatRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void createChat(String uuid, String summary) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("INSERT INTO t_chat(uuid, summary, create_time, update_time) VALUES (?, ?, ?, ?)", uuid, summary, now, now);
    }

    public boolean chatExists(String uuid) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT COUNT(*) > 0 FROM t_chat WHERE uuid = ?", Boolean.class, uuid));
    }

    public void renameChat(String uuid, String summary) {
        jdbc.update("UPDATE t_chat SET summary = ?, update_time = ? WHERE uuid = ?", summary, LocalDateTime.now(), uuid);
    }

    public void deleteChat(String uuid) {
        jdbc.update("DELETE FROM t_chat_message WHERE chat_id = ?", uuid);
        jdbc.update("DELETE FROM t_chat WHERE uuid = ?", uuid);
    }

    public List<ChatSummary> listChats(long current, long size) {
        long offset = Math.max(0, (current - 1) * size);
        return jdbc.query("SELECT uuid, summary, create_time, update_time FROM t_chat ORDER BY update_time DESC LIMIT ? OFFSET ?",
                (rs, i) -> new ChatSummary(rs.getString("uuid"), rs.getString("summary"),
                        rs.getTimestamp("create_time").toLocalDateTime().toString(),
                        rs.getTimestamp("update_time").toLocalDateTime().toString()), size, offset);
    }

    public long countChats() { return jdbc.queryForObject("SELECT COUNT(*) FROM t_chat", Long.class); }

    public Long saveMessage(String chatId, String role, String content, String reasoning) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO t_chat_message(chat_id, role, content, reasoning_content, create_time) VALUES (?, ?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setString(1, chatId); ps.setString(2, role); ps.setString(3, content);
            ps.setString(4, reasoning); ps.setObject(5, LocalDateTime.now());
            return ps;
        }, holder);
        // PostgreSQL may return the complete inserted row even when only the id
        // was requested. Reading getKey() in that case throws because multiple
        // columns are present in the key holder.
        if (holder.getKeys() == null) return null;
        Object id = holder.getKeys().get("id");
        return id instanceof Number number ? number.longValue() : null;
    }

    public List<ChatMessageView> listMessages(String chatId, long current, long size) {
        long offset = Math.max(0, (current - 1) * size);
        return jdbc.query("SELECT id, chat_id, role, content, reasoning_content AS reasoning, create_time FROM t_chat_message WHERE chat_id = ? ORDER BY create_time ASC LIMIT ? OFFSET ?",
                (rs, i) -> new ChatMessageView(rs.getLong("id"), rs.getString("chat_id"), rs.getString("role"),
                        rs.getString("content"), rs.getString("reasoning"), rs.getTimestamp("create_time").toLocalDateTime().toString()),
                chatId, size, offset);
    }

    public long countMessages(String chatId) { return jdbc.queryForObject("SELECT COUNT(*) FROM t_chat_message WHERE chat_id = ?", Long.class, chatId); }

    public List<ChatMessageView> recentMessages(String chatId, int limit) {
        return jdbc.query("SELECT id, chat_id, role, content, reasoning_content AS reasoning, create_time FROM t_chat_message WHERE chat_id = ? ORDER BY create_time DESC LIMIT ?",
                (rs, i) -> new ChatMessageView(rs.getLong("id"), rs.getString("chat_id"), rs.getString("role"),
                        rs.getString("content"), rs.getString("reasoning"), rs.getTimestamp("create_time").toLocalDateTime().toString()),
                chatId, limit).reversed();
    }
}
