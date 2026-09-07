package chisa.zhida.chat;

/** Search result model used by the CompletableFuture/Jsoup application pipeline. */
public record SearchResult(String url, double score, String title, String content) {
    public SearchResult withContent(String value) { return new SearchResult(url, score, title, value); }
}
