package com.deepreach.web.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Data;

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
    private List<Map<String, Object>> history;

    /**
     * 语言代码。
     */
    @NotBlank(message = "lang不能为空")
    private String lang;
}
