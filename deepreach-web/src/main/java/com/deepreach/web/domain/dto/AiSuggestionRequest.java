package com.deepreach.web.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * AI 建议请求体。
 */
@Data
public class AiSuggestionRequest {

    /**
     * AI 实例 ID。
     */
    @NotNull(message = "instanceId不能为空")
    private Long instanceId;

    /**
     * 已处理的聊天历史。
     */
    @NotEmpty(message = "history不能为空")
    @Valid
    private List<ChatRecord> history;

    /**
     * 语言代码。
     */
    @NotBlank(message = "lang不能为空")
    private String lang;

    /**
     * 将历史记录转换为下游 AI 服务所需的 Map 结构，并规范化时间格式。
     */
    public List<Map<String, Object>> toPayloadHistory() {
        if (history == null) {
            return Collections.emptyList();
        }
        return history.stream()
            .map(ChatRecord::toPayload)
            .toList();
    }

    @Data
    public static class ChatRecord {

        private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        /**
         * 时间戳，格式：yyyy-MM-dd HH:mm:ss，可为空。
         */
        private String timestamp;

        /**
         * 图片地址，可为空。
         */
        private String images;

        /**
         * 引用的上一句文本，可为空。
         */
        private String referance;

        /**
         * 来源标记（可选）：history_splice 或 new_record。
         */
        private String source;

        @NotBlank(message = "history.role不能为空")
        private String role;

        @NotBlank(message = "history.content不能为空")
        private String content;

        @NotBlank(message = "history.contact不能为空")
        private String contact;

        public Map<String, Object> toPayload() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("timestamp", normalizeTimestamp(timestamp));
            map.put("images", blankToNull(images));
            map.put("referance", blankToNull(referance));
            map.put("source", blankToNull(source));
            map.put("role", role);
            map.put("content", content);
            map.put("contact", contact);
            return map;
        }

        public ChatRecord copy() {
            ChatRecord clone = new ChatRecord();
            clone.setTimestamp(this.timestamp);
            clone.setImages(this.images);
            clone.setReferance(this.referance);
            clone.setSource(this.source);
            clone.setRole(this.role);
            clone.setContent(this.content);
            clone.setContact(this.contact);
            return clone;
        }

        private String normalizeTimestamp(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            try {
                LocalDateTime time = LocalDateTime.parse(value.trim(), INPUT);
                return time.toString();
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("timestamp格式必须为yyyy-MM-dd HH:mm:ss");
            }
        }

        private String blankToNull(String value) {
            return StringUtils.hasText(value) ? value : null;
        }
    }
}
