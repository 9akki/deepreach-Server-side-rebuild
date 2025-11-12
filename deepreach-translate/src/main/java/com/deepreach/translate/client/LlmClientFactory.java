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
        switch (normalized) {
            case "gpt-4o":
            case "gpt-4o-mini":
            case "gpt-5-nano-2025-08-07":
            case "gpt-4.1-nano-2025-04-14":
                if (shouldUseAzure(normalized)) {
                    return azureOpenAiChatClient;
                }
                return openAiChatClient;
            case "qwen-plus":
                return dashScopeChatClient;
            default:
                throw new ServiceException("不支持的翻译渠道: " + channel);
        }
    }

    private boolean shouldUseAzure(String channel) {
        if (azureOpenAiChatClient == null || azureDeployments == null) {
            return false;
        }
        return azureDeployments.containsKey(channel) || azureDeployments.containsKey(channel.toLowerCase(Locale.ROOT));
    }
}
