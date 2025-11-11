package com.deepreach.web.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Data;

/**
 * AI 建议响应数据。
 */
@Data
public class AiSuggestionResult {

    /** 第三方返回的状态码 */
    private int code;

    /** 规范化后的提示文本 */
    private String message;

    @JsonIgnore
    private Object originalMessage;
    private long totalTokens;

    @JsonProperty("message")
    public void deserializeMessage(Object value) {
        this.originalMessage = value;
        this.message = normalizeMessage(value);
    }

    private String normalizeMessage(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String str) {
            return str.trim();
        }
        if (value instanceof Collection<?>) {
            return flattenCollection((Collection<?>) value);
        }
        if (value.getClass().isArray()) {
            return flattenCollection(Arrays.asList((Object[]) value));
        }
        if (value instanceof Map<?, ?> map) {
            Object text = firstNonNull(
                map.get("text"),
                map.get("content"),
                map.get("message"),
                map.get("value")
            );
            if (text != null && !Objects.equals(text, value)) {
                return normalizeMessage(text);
            }
            return map.toString();
        }
        return value.toString();
    }

    private String flattenCollection(Collection<?> values) {
        List<String> segments = new ArrayList<>();
        for (Object item : values) {
            String segment = normalizeMessage(item);
            if (segment != null && !segment.isBlank()) {
                segments.add(segment);
            }
        }
        return String.join("\n", segments);
    }

    private Object firstNonNull(Object... candidates) {
        for (Object candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @JsonProperty("message")
    public Object serializeMessage() {
        if (originalMessage instanceof String || originalMessage instanceof Collection<?> || originalMessage instanceof Map<?, ?>) {
            return originalMessage;
        }
        if (originalMessage == null && message != null) {
            return message;
        }
        return originalMessage != null ? originalMessage : new LinkedHashMap<>();
    }

    @JsonProperty("total_tokens")
    public void setTotalTokensRaw(Object value) {
        if (value == null) {
            this.totalTokens = 0L;
            return;
        }
        if (value instanceof Number number) {
            this.totalTokens = number.longValue();
            return;
        }
        if (value instanceof String str) {
            try {
                this.totalTokens = Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
                this.totalTokens = 0L;
            }
            return;
        }
        this.totalTokens = 0L;
    }

    public long getTotalTokens() {
        return totalTokens;
    }
}
