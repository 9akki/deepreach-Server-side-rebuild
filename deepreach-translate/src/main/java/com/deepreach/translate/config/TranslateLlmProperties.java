package com.deepreach.translate.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "translate.llm")
public class TranslateLlmProperties {

    private String openaiApiKey = "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";
    private String openaiBaseUrl = "https://api.openai.com/v1/";
    private String qwenApiKey = "sk-a9a6a4eb0c9245fca44796deb6fa6b41";
    private String qwenBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/";
    private int timeoutMs = 10000;
    private AzureProperties azure = new AzureProperties();

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

    public AzureProperties getAzure() {
        return azure;
    }

    public void setAzure(AzureProperties azure) {
        this.azure = azure;
    }

    public boolean isAzureEnabled() {
        return azure != null && azure.isEnabled();
    }

    public Map<String, String> getAzureDeployments() {
        if (azure == null || azure.getDeployments() == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(azure.getDeployments());
    }

    public static class AzureProperties {
        private boolean enabled = false;
        private String apiKey;
        private String endpoint;
        private String apiVersion = "2024-02-15-preview";
        private Map<String, String> deployments = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiVersion() {
            return apiVersion;
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public Map<String, String> getDeployments() {
            return deployments;
        }

        public void setDeployments(Map<String, String> deployments) {
            this.deployments = deployments != null ? new HashMap<>(deployments) : new HashMap<>();
        }
    }
}
