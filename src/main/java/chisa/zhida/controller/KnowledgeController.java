package chisa.zhida.controller;

import chisa.zhida.common.PageResponse;
import chisa.zhida.common.Response;
import chisa.zhida.knowledge.KnowledgeService;
import chisa.zhida.knowledge.MdFileView;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/customer-service")
public class KnowledgeController {
    private final KnowledgeService knowledge;
    private final chisa.zhida.ai.AiModelService ai;
    private final String model;
    public KnowledgeController(KnowledgeService knowledge, chisa.zhida.ai.AiModelService ai,
                               @org.springframework.beans.factory.annotation.Value("${zhida.ollama.model:qwen3:1.7b}") String model) { this.knowledge = knowledge; this.ai = ai; this.model = model; }

    @PostMapping(value = "/md/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<MdFileView> upload(@RequestPart("file") MultipartFile file, @RequestParam(required = false) String remark) { return knowledge.upload(file, remark); }
    @PostMapping("/md/list") public PageResponse<MdFileView> list(@RequestBody(required = false) MdListRequest request) { return knowledge.list(request == null ? 1 : request.current(), request == null ? 20 : request.size(), request == null ? null : request.keyword()); }
    @GetMapping("/md/list") public PageResponse<MdFileView> listGet(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size, @RequestParam(required = false) String keyword) { return knowledge.list(current, size, keyword); }
    @GetMapping("/md/{id}") public String read(@PathVariable long id) { return knowledge.read(id); }
    @PutMapping("/md/{id}") public Response<MdFileView> update(@PathVariable long id, @RequestBody MdUpdateRequest request) { return knowledge.update(id, request.fileName(), request.remark()); }
    @PostMapping("/md/update") public Response<MdFileView> updatePost(@RequestBody MdUpdateRequest request) { return knowledge.update(request.id(), request.fileName(), request.remark()); }
    @DeleteMapping("/md/{id}") public Response<Void> delete(@PathVariable long id) { return knowledge.delete(id); }
    @PostMapping("/md/delete") public Response<Void> deletePost(@RequestBody MdDeleteRequest request) { return knowledge.delete(request.id()); }

    @PostMapping(value = "/chat/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public reactor.core.publisher.Flux<chisa.zhida.chat.AiResponse> chat(@RequestBody CustomerChatRequest request) {
        String context = "你是智答ai智能客服，作者是 chisa。只能根据知识库回答；知识库没有涉及的问题请明确说暂时无法回答。\n\n知识库：\n" + knowledge.contextFor(request.message());
        return ai.stream(request.message(), request.modelName() == null ? model : request.modelName(), request.temperature() == null ? 0.7 : request.temperature(), context, java.util.List.of());
    }
    @GetMapping(value = "/chat/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public reactor.core.publisher.Flux<chisa.zhida.chat.AiResponse> chatGet(@RequestParam String message) { return chat(new CustomerChatRequest(message, null, 0.7)); }

    public record MdListRequest(long current, long size, String keyword) {}
    public record MdUpdateRequest(long id, String fileName, String remark) {}
    public record MdDeleteRequest(long id) {}
    public record CustomerChatRequest(String message, String modelName, Double temperature) {}
}
