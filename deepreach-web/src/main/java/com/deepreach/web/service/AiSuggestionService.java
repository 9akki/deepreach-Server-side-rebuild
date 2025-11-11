package com.deepreach.web.service;

import com.deepreach.web.domain.dto.AiSuggestionRequest;
import com.deepreach.web.domain.dto.AiSuggestionResult;

public interface AiSuggestionService {

    /**
     * 调用AI建议服务。
     *
     * @param request 请求参数
     * @return AI建议结果
     */
    AiSuggestionResult suggest(AiSuggestionRequest request);
}
