package com.deepreach.message.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(MessageSmsProperties.class)
@MapperScan("com.deepreach.message.mapper")
public class MessageMybatisConfig {

    @Bean
    public OkHttpClient messageOkHttpClient(MessageSmsProperties properties) {
        return new OkHttpClient.Builder()
            .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .readTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .writeTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .callTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .build();
    }
}
