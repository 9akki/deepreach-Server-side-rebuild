package com.deepreach.web.config;

import com.deepreach.common.core.config.AiSuggestionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiSuggestionProperties.class)
public class AiSuggestionConfig {
}
