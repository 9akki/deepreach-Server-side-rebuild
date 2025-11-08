package com.deepreach.translate.client;

public class LlmResult {

    private final String content;
    private final long totalTokens;

    public LlmResult(String content, long totalTokens) {
        this.content = content;
        this.totalTokens = totalTokens;
    }

    public String getContent() {
        return content;
    }

    public long getTotalTokens() {
        return totalTokens;
    }
}
