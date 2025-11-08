package com.deepreach.translate.controller;

import com.deepreach.common.exception.ServiceException;
import com.deepreach.common.web.LegacyResponse;
import com.deepreach.translate.dto.OriginalTextRequest;
import com.deepreach.translate.dto.OriginalTextResponse;
import com.deepreach.translate.dto.TranslateRequest;
import com.deepreach.translate.dto.TranslateResponse;
import com.deepreach.translate.service.TranslationService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/translate")
public class TranslateController {

    private static final Logger log = LoggerFactory.getLogger(TranslateController.class);
    private final TranslationService translationService;

    public TranslateController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping
    public Map<String, Object> translate(@Valid @RequestBody TranslateRequest request) {
        try {
            TranslateResponse response = translationService.translate(request);
            return LegacyResponse.success(response);
        } catch (ServiceException ex) {
            return LegacyResponse.error(ex.getCode() != null ? ex.getCode() : 500, ex.getMessage());
        } catch (Exception ex) {
            log.error("Translation failed", ex);
            return LegacyResponse.error("翻译失败: " + ex.getMessage());
        }
    }

    @PostMapping("/original")
    public Map<String, Object> getOriginal(@Valid @RequestBody OriginalTextRequest request) {
        try {
            Optional<OriginalTextResponse> original = translationService.getOriginalText(request);
            if (original.isPresent()) {
                return LegacyResponse.success(original.get());
            }
            return LegacyResponse.error("未找到匹配记录");
        } catch (ServiceException ex) {
            return LegacyResponse.error(ex.getCode() != null ? ex.getCode() : 500, ex.getMessage());
        } catch (Exception ex) {
            log.error("Query original text failed", ex);
            return LegacyResponse.error("查询原文失败: " + ex.getMessage());
        }
    }
}
