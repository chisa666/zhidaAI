package chisa.zhida.advisor;

import chisa.zhida.chat.NetworkSearchService;
import chisa.zhida.chat.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Searches SearXNG, fetches pages concurrently, and rebuilds the model prompt. */
public final class NetworkSearchAdvisor implements StreamAdvisor {
    private static final PromptTemplate TEMPLATE = new PromptTemplate("""
            ## 用户问题
            {question}

            ## 联网搜索上下文
            ---------------------
            {context}
            ---------------------

            请综合上下文回答用户问题。不要编造；上下文不足时明确说明。关键信息后标注来源编号和链接。
            """);

    private final NetworkSearchService searchService;

    public NetworkSearchAdvisor(NetworkSearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        UserMessage user = request.prompt().getUserMessage();
        String question = user == null ? "" : user.getText();
        List<SearchResult> pages = searchService.fetchPages(question, 7, TimeUnit.SECONDS).join();
        String context = buildContext(pages);
        String enhancedText = TEMPLATE.render(Map.of("question", question, "context", context));

        List<Message> messages = new ArrayList<>(request.prompt().getSystemMessages());
        messages.add(new UserMessage(enhancedText));
        Prompt enhanced = new Prompt(messages, request.prompt().getOptions());
        return chain.nextStream(request.mutate().prompt(enhanced).build());
    }

    private String buildContext(List<SearchResult> pages) {
        if (pages == null || pages.isEmpty()) return "暂无可用联网搜索结果";
        StringBuilder result = new StringBuilder();
        int index = 1;
        for (SearchResult page : pages) {
            if (StringUtils.isBlank(page.content())) continue;
            String content = page.content();
            if (content.length() > 4000) content = content.substring(0, 4000);
            result.append("### 来源 ").append(index++).append("\n")
                    .append("标题：").append(page.title()).append("\n")
                    .append("链接：").append(page.url()).append("\n")
                    .append("正文：").append(content).append("\n\n");
        }
        return result.isEmpty() ? "暂无可用联网搜索结果" : result.toString();
    }

    @Override
    public int getOrder() { return 1; }

    @Override
    public String getName() { return getClass().getSimpleName(); }
}
