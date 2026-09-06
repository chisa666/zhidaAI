package chisa.zhida.controller;

import chisa.zhida.chat.ChatRequest;
import chisa.zhida.chat.ChatService;
import chisa.zhida.chat.CompletionRequest;
import chisa.zhida.chat.AiResponse;
import chisa.zhida.common.PageResponse;
import chisa.zhida.common.Response;
import chisa.zhida.chat.ChatSummary;
import chisa.zhida.chat.ChatMessageView;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService service;
    public ChatController(ChatService service) { this.service = service; }

    @PostMapping("/new") public Response<ChatSummary> newChat(@RequestBody @Valid ChatRequest request) { return service.newChat(request.message()); }
    @PostMapping("/rename") public Response<Void> rename(@RequestBody RenameRequest request) { return service.rename(request.chatId(), request.summary()); }
    @PostMapping("/delete") public Response<Void> delete(@RequestBody DeleteRequest request) { return service.delete(request.chatId()); }
    @DeleteMapping("/{chatId}") public Response<Void> deleteByPath(@PathVariable String chatId) { return service.delete(chatId); }

    @PostMapping(value = "/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AiResponse> completion(@RequestBody @Valid CompletionRequest request) { return service.completion(request); }
    @GetMapping(value = "/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AiResponse> legacyCompletion(@RequestParam String message, @RequestParam(required = false) String chatId,
                                             @RequestParam(required = false) String modelName) {
        return service.completion(new CompletionRequest(message, chatId, modelName, false, 0.7));
    }

    @PostMapping("/history") public PageResponse<ChatSummary> history(@RequestBody(required = false) PageRequest request) { return service.list(value(request == null ? null : request.current()), value(request == null ? null : request.size(), 20)); }
    @GetMapping("/history") public PageResponse<ChatSummary> historyGet(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size) { return service.list(current, size); }
    @PostMapping("/messages") public PageResponse<ChatMessageView> messages(@RequestBody MessagePageRequest request) { return service.messages(request.chatId(), request.current(), request.size()); }
    @GetMapping("/{chatId}/messages") public PageResponse<ChatMessageView> messagesGet(@PathVariable String chatId, @RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "50") long size) { return service.messages(chatId, current, size); }

    private long value(Long value) { return value == null ? 1 : value; }
    private long value(Long value, long fallback) { return value == null || value < 1 ? fallback : value; }
    public record RenameRequest(String chatId, String summary) {}
    public record DeleteRequest(String chatId) {}
    public record PageRequest(Long current, Long size) {}
    public record MessagePageRequest(String chatId, long current, long size) {}
}
