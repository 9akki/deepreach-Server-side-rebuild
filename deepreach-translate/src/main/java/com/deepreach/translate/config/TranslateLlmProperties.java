package com.deepreach.translate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "translate.llm")
public class TranslateLlmProperties {

    private String openaiApiKey = "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";
    private String openaiBaseUrl = "https://api.openai.com/v1/";
    private String qwenApiKey = "sk-a9a6a4eb0c9245fca44796deb6fa6b41";
    private String qwenBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/";
    private int timeoutMs = 10000;

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public String getOpenaiBaseUrl() {
        return openaiBaseUrl;
    }

    public void setOpenaiBaseUrl(String openaiBaseUrl) {
        this.openaiBaseUrl = openaiBaseUrl;
    }

    public String getQwenApiKey() {
        return qwenApiKey;
    }

    public void setQwenApiKey(String qwenApiKey) {
        this.qwenApiKey = qwenApiKey;
    }

    public String getQwenBaseUrl() {
        return qwenBaseUrl;
    }

    public void setQwenBaseUrl(String qwenBaseUrl) {
        this.qwenBaseUrl = qwenBaseUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
