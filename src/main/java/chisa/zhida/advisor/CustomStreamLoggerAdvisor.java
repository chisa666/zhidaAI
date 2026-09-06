package chisa.zhida.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/** Aggregates and logs every streamed answer, matching the capability's custom logger. */
public final class CustomStreamLoggerAdvisor implements StreamAdvisor {
    private static final Logger log = LoggerFactory.getLogger(CustomStreamLoggerAdvisor.class);

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        AtomicReference<StringBuilder> fullContent = new AtomicReference<>(new StringBuilder());
        return chain.nextStream(request)
                .doOnNext(response -> {
                    if (response.chatResponse() == null || response.chatResponse().getResult() == null) return;
                    AssistantMessage output = response.chatResponse().getResult().getOutput();
                    if (output == null) return;
                    String chunk = output.getText();
                    if (chunk != null && !chunk.isBlank()) {
                        log.info("## 智答ai chunk: {}", chunk);
                        fullContent.get().append(chunk);
                    }
                })
                .doOnComplete(() -> log.info("\n==== 智答ai FULL AI RESPONSE ====\n{}\n===============================",
                        fullContent.get()))
                .doOnError(error -> log.error("## 智答ai 流式回答异常，已收集内容: {}", fullContent.get(), error));
    }

    @Override
    public int getOrder() { return 99; }

    @Override
    public String getName() { return getClass().getSimpleName(); }
}
