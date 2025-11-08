package com.deepreach.translate.client;

import com.deepreach.common.exception.ServiceException;
import java.util.Locale;

public class LlmClientFactory {

    private final LlmClient openAiChatClient;
    private final LlmClient dashScopeChatClient;

    public LlmClientFactory(LlmClient openAiChatClient, LlmClient dashScopeChatClient) {
        this.openAiChatClient = openAiChatClient;
        this.dashScopeChatClient = dashScopeChatClient;
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
                return openAiChatClient;
            case "qwen-plus":
                return dashScopeChatClient;
            default:
                throw new ServiceException("不支持的翻译渠道: " + channel);
        }
    }
}
