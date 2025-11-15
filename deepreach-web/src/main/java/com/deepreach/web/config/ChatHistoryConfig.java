package com.deepreach.web.config;

import com.deepreach.common.core.config.ChatHistoryAiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatHistoryAiProperties.class)
public class ChatHistoryConfig {
}
