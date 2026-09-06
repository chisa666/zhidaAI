package chisa.zhida.document;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Unified document-reading examples from capability sections 8.4 and 8.5. */
@Service
public class DocumentReaderService {
    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(1000).withMinChunkSizeChars(400)
            .withMinChunkLengthToEmbed(10).withMaxNumChunks(5000)
            .withKeepSeparator(true).build();

    public List<Document> read(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("文件不存在");
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        FileSystemResource resource = new FileSystemResource(path);
        return switch (extension(name)) {
            case "txt" -> new TextReader(resource).get();
            case "json" -> new JsonReader(resource).get();
            case "md", "markdown" -> new MarkdownDocumentReader(resource,
                    MarkdownDocumentReaderConfig.builder()
                            .withHorizontalRuleCreateDocument(true)
                            .withIncludeCodeBlock(false).withIncludeBlockquote(false)
                            .withAdditionalMetadata("fileName", path.getFileName().toString()).build()).get();
            case "html", "htm" -> new JsoupDocumentReader(resource,
                    JsoupDocumentReaderConfig.builder().selector("body")
                            .charset("UTF-8").includeLinkUrls(true)
                            .additionalMetadata("fileName", path.getFileName().toString()).build()).get();
            case "pdf" -> new PagePdfDocumentReader(resource,
                    PdfDocumentReaderConfig.builder().withPagesPerDocument(1).build()).get();
            case "doc", "docx", "ppt", "pptx" -> splitter.apply(new TikaDocumentReader(resource).get());
            default -> throw new IllegalArgumentException("暂不支持的文件类型: " + extension(name));
        };
    }

    private String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1);
    }
}
