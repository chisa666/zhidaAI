package chisa.zhida.chat;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

/** SearXNG 搜索服务，使用项目指定的 OkHttp3 客户端。 */
@Service
public class SearchService {
    private final OkHttpClient client;
    private final JsonMapper jsonMapper;
    private final String searxngUrl;
    private final int count;

    public SearchService(OkHttpClient client, JsonMapper jsonMapper,
                         @Value("${searxng.url:${zhida.searxng.base-url:http://127.0.0.1:8888}/search}") String searxngUrl,
                         @Value("${searxng.count:50}") int count) {
        this.client = client;
        this.jsonMapper = jsonMapper;
        this.searxngUrl = searxngUrl;
        this.count = count;
    }

    public String search(String query) {
        HttpUrl httpUrl = HttpUrl.parse(searxngUrl).newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .addQueryParameter("engines", "bing,quark,sogou,360search")
                .build();
        Request request = new Request.Builder().url(httpUrl).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return "暂无可用搜索结果";
            }
            JsonNode results = jsonMapper.readTree(response.body().string()).path("results");
            record NodeWithScore(double score, JsonNode node) {}
            List<NodeWithScore> nodes = StreamSupport.stream(results.spliterator(), false)
                    .map(node -> new NodeWithScore(node.path("score").asDouble(0.0), node))
                    .sorted(Comparator.comparingDouble(NodeWithScore::score).reversed())
                    .limit(count)
                    .toList();
            StringBuilder context = new StringBuilder();
            for (NodeWithScore item : nodes) {
                JsonNode node = item.node();
                context.append("- ").append(node.path("title").asText())
                        .append("：").append(node.path("content").asText())
                        .append("（").append(node.path("url").asText()).append("）\n");
            }
            return context.isEmpty() ? "暂无可用搜索结果" : context.toString();
        } catch (Exception ignored) {
            return "联网搜索服务未启动，请运行 Docker Compose 中的 searxng 服务。";
        }
    }
}
