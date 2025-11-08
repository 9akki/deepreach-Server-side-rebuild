package com.deepreach.translate.config;

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
    public LlmClient dashScopeChatClient(OkHttpClient translateOkHttpClient, TranslateLlmProperties properties) {
        String baseUrl = ensureTrailingSlash(properties.getQwenBaseUrl());
        return new DashScopeChatClient(translateOkHttpClient, baseUrl, properties.getQwenApiKey());
    }

    @Bean
    public LlmClientFactory llmClientFactory(LlmClient openAiChatClient, LlmClient dashScopeChatClient) {
        return new LlmClientFactory(openAiChatClient, dashScopeChatClient);
    }

    private String ensureTrailingSlash(String rawBaseUrl) {
        if (!StringUtils.hasText(rawBaseUrl)) {
            throw new IllegalArgumentException("BaseUrl for LLM client must not be blank");
        }
        return rawBaseUrl.endsWith("/") ? rawBaseUrl : rawBaseUrl + "/";
    }
}
