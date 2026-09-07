package chisa.zhida.knowledge;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeFileUploadedListener {

    private final KnowledgeService knowledgeService;

    public KnowledgeFileUploadedListener(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Async("eventTaskExecutor")
    @EventListener
    public void handle(KnowledgeFileUploadedEvent event) {
        knowledgeService.vectorize(event.fileId(), event.filePath(), event.fileName());
    }
}
