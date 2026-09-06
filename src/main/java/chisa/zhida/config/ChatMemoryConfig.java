package chisa.zhida.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring AI capability memory with a Cassandra-backed repository when Cassandra is available. */
@Configuration
public class ChatMemoryConfig {

    @Bean
    @ConditionalOnBean(ChatMemoryRepository.class)
    @ConditionalOnMissingBean(ChatMemory.class)
    public ChatMemory chatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(50)
                .chatMemoryRepository(repository)
                .build();
    }
}
