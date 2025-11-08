package com.deepreach.translate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OriginalTextResponse {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("self_language_code")
    private String selfLanguageCode;

    @JsonProperty("sent_text")
    private String sentText;

    @JsonProperty("original_text")
    private String originalText;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSelfLanguageCode() {
        return selfLanguageCode;
    }

    public void setSelfLanguageCode(String selfLanguageCode) {
        this.selfLanguageCode = selfLanguageCode;
    }

    public String getSentText() {
        return sentText;
    }

    public void setSentText(String sentText) {
        this.sentText = sentText;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }
}
