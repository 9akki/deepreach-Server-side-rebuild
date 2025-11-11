package com.deepreach.web.controller;

import com.deepreach.common.exception.ServiceException;
import com.deepreach.common.web.BaseController;
import com.deepreach.common.web.domain.Result;
import com.deepreach.web.domain.dto.AiSuggestionRequest;
import com.deepreach.web.domain.dto.AiSuggestionResult;
import com.deepreach.web.service.AiSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ai/suggestion")
@RequiredArgsConstructor
public class AiSuggestionController extends BaseController {

    private final AiSuggestionService aiSuggestionService;

    @PostMapping("/chat")
    public Result<AiSuggestionResult> getSuggestion(@Valid @RequestBody AiSuggestionRequest request) {
        try {
            AiSuggestionResult response = aiSuggestionService.suggest(request);
            return Result.success(response);
        } catch (ServiceException ex) {
            return Result.error(ex.getCode() != null ? ex.getCode() : 500, ex.getMessage());
        } catch (Exception ex) {
            log.error("AI suggestion request failed", ex);
            return Result.error("AI建议调用失败：" + ex.getMessage());
        }
    }
}
