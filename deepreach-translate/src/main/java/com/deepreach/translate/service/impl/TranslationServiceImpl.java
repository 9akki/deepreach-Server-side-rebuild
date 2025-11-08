package com.deepreach.translate.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.deepreach.common.core.support.ConsumptionBalanceGuard;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.translate.client.LlmClient;
import com.deepreach.translate.client.LlmClientFactory;
import com.deepreach.translate.client.LlmResult;
import com.deepreach.translate.client.Message;
import com.deepreach.translate.dto.OriginalTextRequest;
import com.deepreach.translate.dto.OriginalTextResponse;
import com.deepreach.translate.dto.TranslateRequest;
import com.deepreach.translate.dto.TranslateResponse;
import com.deepreach.translate.entity.TranslatedMessage;
import com.deepreach.translate.mapper.TranslatedMessageMapper;
import com.deepreach.translate.prompt.TranslatePromptProvider;
import com.deepreach.translate.service.TranslationBillingService;
import com.deepreach.translate.service.TranslationService;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TranslationServiceImpl implements TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationServiceImpl.class);

    private final LlmClientFactory llmClientFactory;
    private final TranslationBillingService translationBillingService;
    private final TranslatedMessageMapper translatedMessageMapper;
    private final ConsumptionBalanceGuard balanceGuard;

    public TranslationServiceImpl(LlmClientFactory llmClientFactory,
                                  TranslationBillingService translationBillingService,
                                  TranslatedMessageMapper translatedMessageMapper,
                                  ConsumptionBalanceGuard balanceGuard) {
        this.llmClientFactory = llmClientFactory;
        this.translationBillingService = translationBillingService;
        this.translatedMessageMapper = translatedMessageMapper;
        this.balanceGuard = balanceGuard;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TranslateResponse translate(TranslateRequest request) {
        validateRequest(request);
        Long requestUserId = request.getUserId();
        long normalizedUserId = (requestUserId != null && requestUserId > 0) ? requestUserId : 0L;
        if (requestUserId != null && requestUserId > 0) {
            balanceGuard.ensureSufficientBalance(requestUserId,
                translationBillingService.resolveUnitPrice(),
                "翻译服务");
        }
        String channel = request.getChannel().trim();
        String normalizedChannel = channel.toLowerCase(Locale.ROOT);
        String prompt = TranslatePromptProvider.build(request.getText(), request.getTargetLang(), request.getSourceLang());
        List<Message> messages = Collections.singletonList(new Message("user", prompt));
        LlmClient client = llmClientFactory.getClient(normalizedChannel);
        LlmResult llmResult = client.chat(messages, normalizedChannel);
        String translation = parseTranslation(llmResult.getContent());

        TranslatedMessage entity = new TranslatedMessage();
        entity.setUserId(normalizedUserId);
        entity.setOriginalText(request.getText());
        entity.setSentText(translation);
        entity.setSelfLanguageCode(request.getTargetLang());
        translatedMessageMapper.insert(entity);

        if (requestUserId != null && requestUserId > 0) {
            translationBillingService.deduct(requestUserId, llmResult.getTotalTokens());
        } else {
            log.warn("Skip translation billing due to missing userId, tokens={}", llmResult.getTotalTokens());
        }

        TranslateResponse response = new TranslateResponse();
        response.setTranslation(translation);
        response.setSourceLang(StringUtils.hasText(request.getSourceLang()) ? request.getSourceLang() : "auto");
        response.setTargetLang(request.getTargetLang());
        response.setChannel(normalizedChannel);
        response.setTotalTokens(llmResult.getTotalTokens());
        log.info("Translation success userId={} channel={} tokens={}", request.getUserId(), normalizedChannel, llmResult.getTotalTokens());
        return response;
    }

    @Override
    public Optional<OriginalTextResponse> getOriginalText(OriginalTextRequest request) {
        return translatedMessageMapper.selectLatestBySentText(
                request.getUserId(),
                request.getSelfLanguageCode(),
                request.getSentText())
            .map(entity -> {
                OriginalTextResponse response = new OriginalTextResponse();
                response.setUserId(request.getUserId());
                response.setSelfLanguageCode(request.getSelfLanguageCode());
                response.setSentText(request.getSentText());
                response.setOriginalText(entity.getOriginalText());
                return response;
            });
    }

    private void validateRequest(TranslateRequest request) {
        if (!StringUtils.hasText(request.getText())) {
            throw new ServiceException("text不能为空");
        }
        if (!StringUtils.hasText(request.getTargetLang())) {
            throw new ServiceException("targetLang不能为空");
        }
        if (!StringUtils.hasText(request.getChannel())) {
            throw new ServiceException("channel不能为空");
        }
    }

    private String parseTranslation(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            throw new ServiceException("LLM响应为空");
        }
        String trimmed = rawContent.trim();
        if (trimmed.startsWith("```")) {
            int firstBrace = trimmed.indexOf('{');
            int lastBrace = trimmed.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                trimmed = trimmed.substring(firstBrace, lastBrace + 1);
            }
        }
        try {
            JSONObject json = JSONObject.parseObject(trimmed);
            String translation = json.getString("translation");
            if (!StringUtils.hasText(translation)) {
                throw new ServiceException("LLM响应缺少 translation 字段");
            }
            return translation.trim();
        } catch (Exception ex) {
            log.error("Failed to parse translation payload: {}", rawContent, ex);
            throw new ServiceException("翻译结果解析异常", ex);
        }
    }
}
