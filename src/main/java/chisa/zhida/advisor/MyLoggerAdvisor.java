package chisa.zhida.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

/** Application advisor that records the request and response around synchronous calls. */
public final class MyLoggerAdvisor implements CallAdvisor {
    private static final Logger log = LoggerFactory.getLogger(MyLoggerAdvisor.class);

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("## 智答ai 请求入参: {}", request);
        ChatClientResponse response = chain.nextCall(request);
        log.info("## 智答ai 请求出参: {}", response);
        return response;
    }

    @Override
    public int getOrder() { return 1; }

    @Override
    public String getName() { return getClass().getSimpleName(); }
}
