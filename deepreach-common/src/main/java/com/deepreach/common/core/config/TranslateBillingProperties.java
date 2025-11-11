package com.deepreach.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 翻译计费相关配置，支持按需关闭或切换Topic
 */
@Data
@ConfigurationProperties(prefix = "translate.billing")
public class TranslateBillingProperties {

    /**
     * 是否启用翻译扣费异步事件
     */
    private boolean enabled = true;

    /**
     * 主事件Topic
     */
    private String topic = "translation-charge";

    /**
     * 死信Topic（目前仅用于监控记录）
     */
    private String dlqTopic = "translation-charge-dlq";

    /**
     * 默认账单描述
     */
    private String description = "翻译扣费";
}
