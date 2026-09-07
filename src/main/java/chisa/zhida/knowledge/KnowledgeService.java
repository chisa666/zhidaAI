package chisa.zhida.knowledge;

import chisa.zhida.common.PageResponse;
import chisa.zhida.common.Response;
import chisa.zhida.document.DocumentReaderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeService {
    private final JdbcTemplate jdbc;
    private final Path root;
    private final ObjectProvider<VectorStore> vectorStores;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentReaderService documentReaderService;

    public KnowledgeService(JdbcTemplate jdbc, ObjectProvider<VectorStore> vectorStores,
                            @Value("${zhida.storage-path:./data}") String storagePath,
                            ApplicationEventPublisher eventPublisher,
                            DocumentReaderService documentReaderService) {
        this.jdbc = jdbc;
        this.vectorStores = vectorStores;
        this.eventPublisher = eventPublisher;
        this.documentReaderService = documentReaderService;
        this.root = Paths.get(storagePath).toAbsolutePath().resolve("knowledge");
        try { Files.createDirectories(root); } catch (IOException ignored) { }
    }

    public Response<MdFileView> upload(MultipartFile file, String remark) {
        if (file == null || file.isEmpty()) return Response.fail("请选择知识库文件");
        String name = file.getOriginalFilename() == null ? "knowledge.md" : file.getOriginalFilename();
        if (!isSupported(name)) return Response.fail("仅支持 txt、json、md、html、pdf、doc、docx、ppt、pptx 文件");
        try {
            byte[] bytes = file.getBytes();
            String md5 = md5(bytes);
            Path target = root.resolve(UUID.randomUUID() + "-" + safeName(name));
            Files.write(target, bytes);
            LocalDateTime now = LocalDateTime.now();
            long id = insert(md5, name, target.toString(), bytes.length, remark, now);
            eventPublisher.publishEvent(new KnowledgeFileUploadedEvent(id, target.toString(), name));
            return Response.success(find(id));
        } catch (Exception e) { return Response.fail("文件保存失败"); }
    }

    public PageResponse<MdFileView> list(long current, long size, String keyword) {
        current = Math.max(1, current); size = Math.min(Math.max(1, size), 100);
        long offset = (current - 1) * size;
        String like = keyword == null ? "" : keyword.trim();
        List<MdFileView> records = jdbc.query("SELECT id, original_file_name, file_size, status, create_time, update_time, remark FROM t_ai_customer_service_md_storage WHERE original_file_name LIKE ? ORDER BY create_time DESC LIMIT ? OFFSET ?",
                (rs, i) -> new MdFileView(rs.getLong("id"), rs.getString("original_file_name"), formatSize(rs.getLong("file_size")),
                        rs.getInt("status"), rs.getTimestamp("create_time").toLocalDateTime().toString(),
                        rs.getTimestamp("update_time").toLocalDateTime().toString(), rs.getString("remark")), "%" + like + "%", size, offset);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM t_ai_customer_service_md_storage WHERE original_file_name LIKE ?", Long.class, "%" + like + "%");
        return PageResponse.of(records, total, current, size);
    }

    public Response<MdFileView> update(long id, String fileName, String remark) {
        if (fileName == null || fileName.isBlank()) return Response.fail("文件名不能为空");
        int changed = jdbc.update("UPDATE t_ai_customer_service_md_storage SET original_file_name = ?, remark = ?, update_time = ? WHERE id = ?",
                fileName.trim(), remark, LocalDateTime.now(), id);
        return changed == 0 ? Response.fail("文件不存在") : Response.success(find(id));
    }

    public Response<Void> delete(long id) {
        List<String> paths = jdbc.query("SELECT storage_path FROM t_ai_customer_service_md_storage WHERE id = ?", (rs, i) -> rs.getString(1), id);
        if (paths.isEmpty()) return Response.fail("文件不存在");
        try { Files.deleteIfExists(Paths.get(paths.get(0))); } catch (IOException ignored) { }
        try {
            VectorStore vectorStore = vectorStores.getIfAvailable();
            if (vectorStore != null) {
                vectorStore.delete(new FilterExpressionBuilder().eq("mdStorageId", id).build());
            }
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(KnowledgeService.class)
                    .warn("Failed to delete vector records for knowledge file {}", id, e);
        }
        jdbc.update("DELETE FROM t_ai_customer_service_md_storage WHERE id = ?", id);
        return Response.success();
    }

    public String contextFor(String question) {
        try {
            VectorStore vectorStore = vectorStores.getIfAvailable();
            if (vectorStore != null && question != null && !question.isBlank()) {
                List<Document> matches = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(question)
                        .topK(5)
                        .build());
                if (matches != null && !matches.isEmpty()) {
                    return matches.stream().map(Document::getText)
                            .filter(text -> text != null && !text.isBlank())
                            .reduce((left, right) -> left + "\n---\n" + right)
                            .orElse("");
                }
            }
        } catch (RuntimeException e) {
            // Keep chat available when vector search is unavailable, while preserving the cause in logs.
            org.slf4j.LoggerFactory.getLogger(KnowledgeService.class)
                    .warn("Knowledge vector search failed; falling back to local files", e);
        }
        String like = "%" + (question == null ? "" : question.trim()) + "%";
        List<String> paths = jdbc.query("SELECT storage_path FROM t_ai_customer_service_md_storage WHERE status = 2 OR status = 0 ORDER BY update_time DESC LIMIT 10",
                (rs, i) -> rs.getString(1));
        StringBuilder result = new StringBuilder();
        for (String path : paths) {
            try {
                String content = Files.readString(Paths.get(path), StandardCharsets.UTF_8);
                if (content.toLowerCase().contains(question == null ? "" : question.toLowerCase()) || result.isEmpty()) {
                    result.append(content, 0, Math.min(content.length(), 8000)).append("\n---\n");
                }
            } catch (IOException ignored) { }
        }
        return result.toString();
    }

    public String read(long id) {
        List<String> paths = jdbc.query("SELECT storage_path FROM t_ai_customer_service_md_storage WHERE id = ?", (rs, i) -> rs.getString(1), id);
        if (paths.isEmpty()) return "";
        try { return Files.readString(Paths.get(paths.get(0)), StandardCharsets.UTF_8); } catch (IOException e) { return ""; }
    }

    private long insert(String md5, String name, String path, long size, String remark, LocalDateTime now) {
        var holder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("INSERT INTO t_ai_customer_service_md_storage(md5, original_file_name, storage_path, file_size, status, remark, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new String[]{"id"});
            ps.setString(1, md5); ps.setString(2, name); ps.setString(3, path); ps.setLong(4, size); ps.setInt(5, 0); ps.setString(6, remark); ps.setObject(7, now); ps.setObject(8, now); return ps;
        }, holder);
        Object id = holder.getKeys() == null ? null : holder.getKeys().get("id");
        if (!(id instanceof Number number)) throw new IllegalStateException("数据库未返回问答文件 id");
        return number.longValue();
    }

    public void vectorize(long id, String filePath, String originalName) {
        try {
            Path file = Paths.get(filePath);
            VectorStore vectorStore = vectorStores.getIfAvailable();
            if (vectorStore == null) throw new IllegalStateException("VectorStore 未初始化");
            jdbc.update("UPDATE t_ai_customer_service_md_storage SET status = 1, update_time = ? WHERE id = ?", LocalDateTime.now(), id);
            List<Document> documents = documentReaderService.read(file);
            documents.forEach(document -> {
                document.getMetadata().put("mdStorageId", id);
                document.getMetadata().put("fileName", originalName);
            });
            if (!documents.isEmpty()) vectorStore.add(documents);
            jdbc.update("UPDATE t_ai_customer_service_md_storage SET status = 2, update_time = ? WHERE id = ?", LocalDateTime.now(), id);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(KnowledgeService.class)
                    .error("Knowledge vectorization failed for file {} (id={})", originalName, id, e);
            jdbc.update("UPDATE t_ai_customer_service_md_storage SET status = 3, update_time = ? WHERE id = ?", LocalDateTime.now(), id);
        }
    }

    private boolean isSupported(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".json")
                || lower.endsWith(".md") || lower.endsWith(".markdown")
                || lower.endsWith(".html") || lower.endsWith(".htm")
                || lower.endsWith(".pdf") || lower.endsWith(".doc")
                || lower.endsWith(".docx") || lower.endsWith(".ppt")
                || lower.endsWith(".pptx");
    }

    private MdFileView find(long id) { return listById(id); }
    private MdFileView listById(long id) {
        return jdbc.queryForObject("SELECT id, original_file_name, file_size, status, create_time, update_time, remark FROM t_ai_customer_service_md_storage WHERE id = ?",
                (rs, i) -> new MdFileView(rs.getLong("id"), rs.getString("original_file_name"), formatSize(rs.getLong("file_size")), rs.getInt("status"), rs.getTimestamp("create_time").toLocalDateTime().toString(), rs.getTimestamp("update_time").toLocalDateTime().toString(), rs.getString("remark")), id);
    }

    private String safeName(String name) { return name.replaceAll("[^a-zA-Z0-9._-]", "_"); }
    private String formatSize(long value) { if (value < 1024) return value + " B"; if (value < 1024 * 1024) return String.format("%.2f KB", value / 1024d); return String.format("%.2f MB", value / 1024d / 1024d); }
    private String md5(byte[] bytes) throws Exception { var digest = MessageDigest.getInstance("MD5"); var result = digest.digest(bytes); var out = new StringBuilder(); for (byte b : result) out.append(String.format("%02x", b)); return out.toString(); }
}
