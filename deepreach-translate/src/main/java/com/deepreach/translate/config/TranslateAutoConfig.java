package com.deepreach.translate.config;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.deepreach.translate.client.AzureOpenAiChatClient;
import com.deepreach.translate.client.DashScopeChatClient;
import com.deepreach.translate.client.LlmClient;
import com.deepreach.translate.client.LlmClientFactory;
import com.deepreach.translate.client.OpenAiChatClient;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Configuration
@EnableConfigurationProperties(TranslateLlmProperties.class)
public class TranslateAutoConfig {

    @Bean
    public OkHttpClient translateOkHttpClient(TranslateLlmProperties properties) {
        return new OkHttpClient.Builder()
            .callTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .readTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .writeTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .build();
    }

    @Bean
    public OpenAiService translateOpenAiService(TranslateLlmProperties properties) {
        Duration timeout = Duration.ofMillis(properties.getTimeoutMs());
        OkHttpClient client = OpenAiService.defaultClient(properties.getOpenaiApiKey(), timeout);
        String baseUrl = ensureTrailingSlash(properties.getOpenaiBaseUrl());
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(OpenAiService.defaultObjectMapper()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();
        OpenAiApi api = retrofit.create(OpenAiApi.class);
        ExecutorService executor = Executors.newCachedThreadPool();
        return new OpenAiService(api, executor);
    }

    @Bean
    public LlmClient openAiChatClient(OpenAiService translateOpenAiService) {
        return new OpenAiChatClient(translateOpenAiService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "translate.llm.azure", name = "enabled", havingValue = "true")
    public OpenAIClient azureOpenAIClient(TranslateLlmProperties properties) {
        TranslateLlmProperties.AzureProperties azure = properties.getAzure();
        if (azure == null || !StringUtils.hasText(azure.getEndpoint()) || !StringUtils.hasText(azure.getApiKey())) {
            throw new IllegalArgumentException("Azure OpenAI 需要配置 endpoint 与 apiKey");
        }
        return new OpenAIClientBuilder()
            .endpoint(trimTrailingSlash(azure.getEndpoint()))
            .credential(new AzureKeyCredential(azure.getApiKey()))
            .buildClient();
    }

    @Bean
    @ConditionalOnBean(OpenAIClient.class)
    public AzureOpenAiChatClient azureOpenAiChatClient(OpenAIClient azureOpenAIClient, TranslateLlmProperties properties) {
        return new AzureOpenAiChatClient(azureOpenAIClient, properties.getAzureDeployments());
    }

    @Bean
    public LlmClient dashScopeChatClient(OkHttpClient translateOkHttpClient, TranslateLlmProperties properties) {
        String baseUrl = ensureTrailingSlash(properties.getQwenBaseUrl());
        return new DashScopeChatClient(translateOkHttpClient, baseUrl, properties.getQwenApiKey());
    }

    @Bean
    public LlmClientFactory llmClientFactory(@Qualifier("openAiChatClient") LlmClient openAiChatClient,
                                             @Qualifier("dashScopeChatClient") LlmClient dashScopeChatClient,
                                             ObjectProvider<AzureOpenAiChatClient> azureOpenAiChatClientProvider,
                                             TranslateLlmProperties properties) {
        return new LlmClientFactory(
            openAiChatClient,
            dashScopeChatClient,
            azureOpenAiChatClientProvider.getIfAvailable(),
            properties.getAzureDeployments());
    }

    private String ensureTrailingSlash(String rawBaseUrl) {
        if (!StringUtils.hasText(rawBaseUrl)) {
            throw new IllegalArgumentException("BaseUrl for LLM client must not be blank");
        }
        return rawBaseUrl.endsWith("/") ? rawBaseUrl : rawBaseUrl + "/";
    }

    private String trimTrailingSlash(String rawBaseUrl) {
        if (!StringUtils.hasText(rawBaseUrl)) {
            throw new IllegalArgumentException("BaseUrl for LLM client must not be blank");
        }
        return rawBaseUrl.endsWith("/") ? rawBaseUrl.substring(0, rawBaseUrl.length() - 1) : rawBaseUrl;
    }
}
