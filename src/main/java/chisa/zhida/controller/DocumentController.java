package chisa.zhida.controller;

import chisa.zhida.document.DocumentReaderService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;

/** Document reader endpoints for text, JSON, Markdown, HTML, PDF, Word and PPT. */
@RestController
@RequestMapping("/api/read")
public class DocumentController {
    private final DocumentReaderService readers;

    public DocumentController(DocumentReaderService readers) { this.readers = readers; }

    @GetMapping
    public List<Document> read(@RequestParam String path) {
        return readers.read(Path.of(path));
    }
}
