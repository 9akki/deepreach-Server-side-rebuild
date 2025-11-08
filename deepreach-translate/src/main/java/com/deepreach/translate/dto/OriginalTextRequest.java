package com.deepreach.translate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OriginalTextRequest {

    @NotNull
    @JsonAlias("user_id")
    private Long userId;

    @NotBlank
    @JsonAlias("self_language_code")
    private String selfLanguageCode;

    @NotBlank
    @JsonAlias("sent_text")
    private String sentText;

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
}
