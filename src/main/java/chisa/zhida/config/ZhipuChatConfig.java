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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/** Optional Zhipu GLM OpenAI-compatible chat model. */
@Configuration
@Profile("ollama")
@ConditionalOnProperty(name = "zhida.zhipu.enabled", havingValue = "true")
public class ZhipuChatConfig {
    @Bean(name = "zhipuOpenAiClient", destroyMethod = "close")
    public OpenAIClient zhipuOpenAiClient(@Value("${zhida.zhipu.base-url}") String baseUrl,
                                          @Value("${zhida.zhipu.api-key}") String apiKey) {
        return new OpenAIClientImpl(options(baseUrl, apiKey));
    }

    @Bean(name = "zhipuOpenAiClientAsync", destroyMethod = "close")
    public OpenAIClientAsync zhipuOpenAiClientAsync(@Value("${zhida.zhipu.base-url}") String baseUrl,
                                                    @Value("${zhida.zhipu.api-key}") String apiKey) {
        return new OpenAIClientAsyncImpl(options(baseUrl, apiKey));
    }

    @Bean(name = "zhipuChatModel")
    public OpenAiChatModel zhipuChatModel(@Qualifier("zhipuOpenAiClient") OpenAIClient zhipuOpenAiClient,
                                          @Qualifier("zhipuOpenAiClientAsync") OpenAIClientAsync zhipuOpenAiClientAsync,
                                          @Value("${zhida.zhipu.model:glm-4-flash}") String model) {
        return OpenAiChatModel.builder().openAiClient(zhipuOpenAiClient)
                .openAiClientAsync(zhipuOpenAiClientAsync)
                .options(OpenAiChatOptions.builder().model(model).build()).build();
    }

    private ClientOptions options(String baseUrl, String apiKey) {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("启用智谱模型时必须配置 base-url 和 api-key");
        }
        return ClientOptions.builder().baseUrl(baseUrl).apiKey(apiKey)
                .httpClient(SpringAiOpenAiHttpClient.builder().timeout(Duration.ofSeconds(120)).build())
                .maxRetries(1).build();
    }
}
