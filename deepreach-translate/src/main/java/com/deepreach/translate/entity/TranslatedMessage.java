package com.deepreach.translate.entity;

/**
 * 记录用户翻译后的消息，复刻 Python translated_messages 表结构。
 */
public class TranslatedMessage {

    private Long id;
    private Long userId;
    private String originalText;
    private String sentText;
    private String selfLanguageCode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getSentText() {
        return sentText;
    }

    public void setSentText(String sentText) {
        this.sentText = sentText;
    }

    public String getSelfLanguageCode() {
        return selfLanguageCode;
    }

    public void setSelfLanguageCode(String selfLanguageCode) {
        this.selfLanguageCode = selfLanguageCode;
    }

}
