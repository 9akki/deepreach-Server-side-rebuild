package com.deepreach.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "message.sms")
public class MessageSmsProperties {

    private String baseUrl = "http://47.82.1.178:8000";
    private String accountId = "49336626";
    private String apiKey = "4b3e71d9-badf-4767-85ea-90cbb5ef0944";
    private long rateLimitMs = 0;
    private int timeoutMs = 10000;
    private String senderNumber = "17707695953";
    private String sendPath = "/sms-api/v1/49336626/send-sms/4b3e71d9-badf-4767-85ea-90cbb5ef0944/";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getRateLimitMs() {
        return rateLimitMs;
    }

    public void setRateLimitMs(long rateLimitMs) {
        this.rateLimitMs = rateLimitMs;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getSenderNumber() {
        return senderNumber;
    }

    public void setSenderNumber(String senderNumber) {
        this.senderNumber = senderNumber;
    }

    public String getSendPath() {
        return sendPath;
    }

    public void setSendPath(String sendPath) {
        this.sendPath = sendPath;
    }
}
