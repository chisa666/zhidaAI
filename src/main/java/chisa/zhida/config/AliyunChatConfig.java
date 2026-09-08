package chisa.zhida.config;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.OpenAIClientAsyncImpl;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/** Optional Aliyun OpenAI-compatible chat model available alongside local Ollama. */
@Configuration
@Profile("ollama")
@ConditionalOnProperty(name = "zhida.aliyun.enabled", havingValue = "true")
public class AliyunChatConfig {

    @Bean(destroyMethod = "close")
    public OpenAIClient aliyunOpenAiClient(@Value("${zhida.aliyun.base-url}") String baseUrl,
                                           @Value("${zhida.aliyun.api-key}") String apiKey) {
        return new OpenAIClientImpl(clientOptions(baseUrl, apiKey));
    }

    @Bean(destroyMethod = "close")
    public OpenAIClientAsync aliyunOpenAiClientAsync(@Value("${zhida.aliyun.base-url}") String baseUrl,
                                                     @Value("${zhida.aliyun.api-key}") String apiKey) {
        return new OpenAIClientAsyncImpl(clientOptions(baseUrl, apiKey));
    }

    @Bean
    public OpenAiChatModel aliyunChatModel(OpenAIClient aliyunOpenAiClient,
                                           OpenAIClientAsync aliyunOpenAiClientAsync,
                                           @Value("${zhida.aliyun.model:qwen3.8-flash}") String model) {
        return OpenAiChatModel.builder()
                .openAiClient(aliyunOpenAiClient)
                .openAiClientAsync(aliyunOpenAiClientAsync)
                .options(OpenAiChatOptions.builder().model(model).build())
                .build();
    }

    private ClientOptions clientOptions(String baseUrl, String apiKey) {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("启用阿里云模型时必须配置 base-url 和 api-key");
        }
        return ClientOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .httpClient(SpringAiOpenAiHttpClient.builder().timeout(Duration.ofSeconds(120)).build())
                .maxRetries(2)
                .build();
    }
}