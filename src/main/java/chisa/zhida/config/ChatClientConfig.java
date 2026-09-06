package chisa.zhida.config;

import chisa.zhida.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

/** Shared ChatClient configured for the local Ollama capability profile. */
@Configuration
@Profile("ollama")
public class ChatClientConfig {

    @Bean
    @ConditionalOnMissingBean(ChatClient.class)
    public ChatClient chatClient(OllamaChatModel chatModel, ObjectProvider<ChatMemory> memories) {
        List<Advisor> advisors = new ArrayList<>();
        advisors.add(new SimpleLoggerAdvisor());
        advisors.add(new MyLoggerAdvisor());
        ChatMemory memory = memories.getIfAvailable();
        if (memory != null) advisors.add(MessageChatMemoryAdvisor.builder(memory).build());
        return ChatClient.builder(chatModel)
                .defaultSystem("你是智答ai，作者是 chisa。回答准确、简洁，支持 Markdown。")
                .defaultOptions(OllamaChatOptions.builder().numGPU(0).numCtx(2048).numPredict(256).disableThinking().temperature(0.7))
                .defaultAdvisors(advisors)
                .build();
    }
}
