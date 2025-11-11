package com.deepreach.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 智能建议服务配置。
 */
@Data
@ConfigurationProperties(prefix = "ai.suggestion")
public class AiSuggestionProperties {

    /**
     * 是否启用 AI 建议服务。
     */
    private boolean enabled = true;

    /**
     * 后端服务地址，例如：http://206.82.1.18/agent/chat_with_character
     */
    private String endpoint;

    /**
     * 请求超时时间（毫秒）。
     */
    private long timeoutMs = 10000L;

    /**
     * 调用前余额预校验使用的最小额度，单位：DR积分。
     */
    private String sceneName = "AI建议服务";
}
