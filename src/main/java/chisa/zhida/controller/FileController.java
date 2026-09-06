package chisa.zhida.controller;

import chisa.zhida.common.Response;
import chisa.zhida.file.ChunkUploadService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
public class FileController {
    private final ChunkUploadService chunks;
    public FileController(ChunkUploadService chunks) { this.chunks = chunks; }
    @PostMapping("/check") public Response<ChunkUploadService.CheckResult> check(@RequestBody CheckRequest request) { return chunks.check(request.md5(), request.fileName(), request.fileSize(), request.chunkCount()); }
    @PostMapping(value = "/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) public Response<Void> upload(@RequestParam String md5, @RequestParam int chunkIndex, @RequestPart MultipartFile chunk) { return chunks.upload(md5, chunkIndex, chunk); }
    @PostMapping("/merge") public Response<ChunkUploadService.MergeResult> merge(@RequestBody MergeRequest request) { return chunks.merge(request.md5(), request.fileName(), request.chunkCount()); }
    public record CheckRequest(String md5, String fileName, long fileSize, int chunkCount) {}
    public record MergeRequest(String md5, String fileName, int chunkCount) {}
}
