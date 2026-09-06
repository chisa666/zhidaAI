package chisa.zhida.file;

import chisa.zhida.common.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ChunkUploadService {
    private final JdbcTemplate jdbc;
    private final Path root;
    public ChunkUploadService(JdbcTemplate jdbc, @Value("${zhida.storage-path:./data}") String storagePath) {
        this.jdbc = jdbc; this.root = Paths.get(storagePath).toAbsolutePath().resolve("uploads");
        try { Files.createDirectories(root); } catch (IOException ignored) { }
    }

    public Response<CheckResult> check(String md5, String fileName, long fileSize, int chunkCount) {
        if (md5 == null || md5.isBlank() || fileName == null || fileName.isBlank() || fileSize < 0 || chunkCount < 1) {
            return Response.fail("文件检查参数不完整");
        }
        List<FileRecord> records = jdbc.query("SELECT file_path, status, total_chunks FROM t_ai_customer_service_file_storage WHERE file_md5 = ?",
                (rs, i) -> new FileRecord(rs.getString("file_path"), rs.getInt("status"), rs.getInt("total_chunks")), md5);
        if (records.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            jdbc.update("INSERT INTO t_ai_customer_service_file_storage(file_md5, file_name, file_size, total_chunks, uploaded_chunks, status, create_time, update_time) VALUES (?, ?, ?, ?, 0, 0, ?, ?)",
                    md5, safeName(fileName), fileSize, chunkCount, now, now);
        }
        FileRecord record = records.isEmpty() ? null : records.get(0);
        boolean fast = record != null && record.status() == 2 && record.filePath() != null && Files.exists(Paths.get(record.filePath()));
        Path dir = root.resolve(md5 == null ? "unknown" : md5);
        List<Integer> uploaded = jdbc.query("SELECT chunk_index FROM t_file_chunk_info WHERE file_md5 = ? ORDER BY chunk_index",
                (rs, i) -> rs.getInt(1), md5);
        if (uploaded.isEmpty() && Files.exists(dir)) {
            try (var paths = Files.list(dir)) {
                uploaded = paths.filter(p -> p.getFileName().toString().endsWith(".part"))
                        .map(p -> p.getFileName().toString().replace(".part", ""))
                        .filter(s -> s.matches("\\d+"))
                        .map(Integer::parseInt).sorted().toList();
            } catch (IOException ignored) { }
        }
        return Response.success(new CheckResult(fast, uploaded.size(), chunkCount, uploaded));
    }

    public Response<Void> upload(String md5, int chunkIndex, MultipartFile chunk) {
        if (md5 == null || md5.isBlank() || chunk == null || chunk.isEmpty()) return Response.fail("分片参数不完整");
        try {
            if (chunkIndex < 0) return Response.fail("分片序号不能小于 0");
            Path dir = root.resolve(md5); Files.createDirectories(dir);
            Path target = dir.resolve(chunkIndex + ".part");
            chunk.transferTo(target);
            jdbc.update("INSERT INTO t_file_chunk_info(file_md5, chunk_index, chunk_size, storage_path, create_time) VALUES (?, ?, ?, ?, ?) ON CONFLICT (file_md5, chunk_index) DO UPDATE SET chunk_size = EXCLUDED.chunk_size, storage_path = EXCLUDED.storage_path",
                    md5, chunkIndex, chunk.getSize(), target.toString(), LocalDateTime.now());
            jdbc.update("UPDATE t_ai_customer_service_file_storage SET uploaded_chunks = (SELECT COUNT(*) FROM t_file_chunk_info WHERE file_md5 = ?), update_time = ? WHERE file_md5 = ?",
                    md5, LocalDateTime.now(), md5);
            return Response.success();
        } catch (IOException e) { return Response.fail("分片保存失败"); }
    }

    public Response<MergeResult> merge(String md5, String fileName, int chunkCount) {
        try {
            if (md5 == null || md5.isBlank() || fileName == null || fileName.isBlank() || chunkCount < 1) return Response.fail("合并参数不完整");
            Path dir = root.resolve(md5); if (!Files.exists(dir)) return Response.fail("没有找到上传分片");
            List<Integer> uploaded = jdbc.query("SELECT chunk_index FROM t_file_chunk_info WHERE file_md5 = ? ORDER BY chunk_index", (rs, i) -> rs.getInt(1), md5);
            if (uploaded.size() != chunkCount) return Response.fail("分片数量不完整");
            Path target = root.resolve(md5 + "-" + safeName(fileName));
            try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (int i = 0; i < chunkCount; i++) {
                    Path part = dir.resolve(i + ".part"); if (!Files.exists(part)) return Response.fail("分片 " + i + " 尚未上传");
                    Files.copy(part, out);
                }
            }
            Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) { } });
            long size = Files.size(target);
            jdbc.update("UPDATE t_ai_customer_service_file_storage SET file_name = ?, file_path = ?, file_size = ?, total_chunks = ?, uploaded_chunks = ?, status = 2, update_time = ? WHERE file_md5 = ?",
                    safeName(fileName), target.toString(), size, chunkCount, chunkCount, LocalDateTime.now(), md5);
            jdbc.update("DELETE FROM t_file_chunk_info WHERE file_md5 = ?", md5);
            return Response.success(new MergeResult(target.toString(), size));
        } catch (IOException e) { return Response.fail("文件合并失败"); }
    }

    private record FileRecord(String filePath, int status, int totalChunks) {}
    private String safeName(String name) { return Objects.requireNonNullElse(name, "upload.bin").replaceAll("[^a-zA-Z0-9._-]", "_"); }
    public record CheckResult(boolean fastUpload, long uploadedChunks, int chunkCount, List<Integer> uploadedChunkIndexes) {}
    public record MergeResult(String path, long size) {}
}
