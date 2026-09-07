package chisa.zhida.knowledge;

/** Published after a knowledge file is safely stored and recorded. */
public record KnowledgeFileUploadedEvent(long fileId, String filePath, String fileName) {
}
