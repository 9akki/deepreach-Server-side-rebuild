package com.deepreach.translate.client;

import com.deepreach.common.exception.ServiceException;
import java.util.Locale;
import java.util.Map;
import org.springframework.lang.Nullable;

public class LlmClientFactory {

    private final LlmClient openAiChatClient;
    private final LlmClient dashScopeChatClient;
    @Nullable
    private final LlmClient azureOpenAiChatClient;
    private final Map<String, String> azureDeployments;

    public LlmClientFactory(LlmClient openAiChatClient,
                            LlmClient dashScopeChatClient,
                            @Nullable LlmClient azureOpenAiChatClient,
                            Map<String, String> azureDeployments) {
        this.openAiChatClient = openAiChatClient;
        this.dashScopeChatClient = dashScopeChatClient;
        this.azureOpenAiChatClient = azureOpenAiChatClient;
        this.azureDeployments = azureDeployments;
    }

    public LlmClient getClient(String channel) {
        if (channel == null) {
            throw new ServiceException("channel不能为空");
        }
        String normalized = channel.trim().toLowerCase(Locale.ROOT);
        if ("qwen-plus".equals(normalized)) {
            return dashScopeChatClient;
        }
        if (shouldUseAzure(channel, normalized)) {
            return azureOpenAiChatClient;
        }
        if (normalized.startsWith("gpt-")) {
            return openAiChatClient;
        }
        throw new ServiceException("不支持的翻译渠道: " + channel);
    }

    private boolean shouldUseAzure(String rawChannel, String normalizedChannel) {
        if (azureOpenAiChatClient == null || azureDeployments == null) {
            return false;
        }
        return azureDeployments.containsKey(rawChannel)
            || azureDeployments.containsKey(normalizedChannel)
            || azureDeployments.containsKey(rawChannel.trim())
            || azureDeployments.containsKey(normalizedChannel.trim());
    }
}
