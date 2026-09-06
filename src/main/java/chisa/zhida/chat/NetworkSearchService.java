package chisa.zhida.chat;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

/** SearXNG results + concurrent page fetching + Jsoup text extraction. */
@Service
public class NetworkSearchService {
    private final OkHttpClient client;
    private final JsonMapper mapper;
    private final String url;
    private final int count;
    private final ThreadPoolTaskExecutor httpExecutor;
    private final ThreadPoolTaskExecutor processingExecutor;

    public NetworkSearchService(OkHttpClient client, JsonMapper mapper,
                                @Value("${searxng.url:${zhida.searxng.base-url:http://127.0.0.1:8888}/search}") String url,
                                @Value("${searxng.count:3}") int count,
                                @Qualifier("httpRequestExecutor") ThreadPoolTaskExecutor httpExecutor,
                                @Qualifier("resultProcessingExecutor") ThreadPoolTaskExecutor processingExecutor) {
        this.client = client; this.mapper = mapper; this.url = url; this.count = count;
        this.httpExecutor = httpExecutor; this.processingExecutor = processingExecutor;
    }

    public List<SearchResult> search(String query) {
        HttpUrl target = HttpUrl.parse(url).newBuilder().addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .addQueryParameter("engines", "bing,quark,sogou,360search").build();
        try (okhttp3.Response response = client.newCall(new Request.Builder().url(target).get().build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) return List.of();
            JsonNode nodes = mapper.readTree(response.body().string()).path("results");
            return StreamSupport.stream(nodes.spliterator(), false).map(n -> new SearchResult(
                    n.path("url").asText(), n.path("score").asDouble(0), n.path("title").asText(), n.path("content").asText()))
                    .sorted(Comparator.comparingDouble(SearchResult::score).reversed()).limit(count).toList();
        } catch (Exception e) { return List.of(); }
    }

    public CompletableFuture<List<SearchResult>> fetchPages(String query, long timeout, TimeUnit unit) {
        List<SearchResult> results = search(query);
        List<CompletableFuture<SearchResult>> futures = results.stream().map(result -> CompletableFuture
                .supplyAsync(() -> result.withContent(fetchText(result.url())), httpExecutor)
                .completeOnTimeout(result.withContent(""), timeout, unit)
                .exceptionally(error -> result.withContent(""))).toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(v -> futures.stream().map(CompletableFuture::join).toList(), processingExecutor);
    }

    private String fetchText(String target) {
        if (target == null || target.isBlank()) return "";
        try (okhttp3.Response response = client.newCall(new Request.Builder().url(target)
                .header("User-Agent", "Mozilla/5.0").header("Accept", "text/html").get().build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) return "";
            return Jsoup.parse(response.body().string()).text();
        } catch (Exception e) { return ""; }
    }
}
