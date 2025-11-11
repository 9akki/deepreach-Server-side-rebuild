package com.deepreach.web.config;

import com.deepreach.common.core.config.TranslateBillingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TranslateBillingProperties.class)
public class TranslateBillingConfig {
}
