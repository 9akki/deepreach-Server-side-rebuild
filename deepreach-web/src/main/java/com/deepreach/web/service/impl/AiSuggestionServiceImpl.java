package com.deepreach.web.service.impl;

import com.deepreach.common.core.config.AiSuggestionProperties;
import com.deepreach.common.core.support.ConsumptionBalanceGuard;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.translate.service.TranslationBillingService;
import com.deepreach.web.entity.AiCharacter;
import com.deepreach.web.entity.AiInstance;
import com.deepreach.web.domain.dto.AiSuggestionRequest;
import com.deepreach.web.domain.dto.AiSuggestionResult;
import com.deepreach.web.service.AiCharacterService;
import com.deepreach.web.service.AiInstanceService;
import com.deepreach.web.service.AiSuggestionService;
import com.deepreach.web.service.CommunicateHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * AI 建议服务实现。
 */
@Slf4j
@Service
public class AiSuggestionServiceImpl implements AiSuggestionService {

    private static final Map<String, String> LANGUAGE_DISPLAY_MAP = buildLanguageDisplayMap();

    private final AiInstanceService aiInstanceService;
    private final AiCharacterService aiCharacterService;
    private final TranslationBillingService translationBillingService;
    private final ConsumptionBalanceGuard balanceGuard;
    private final AiSuggestionProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CommunicateHistoryService communicateHistoryService;

    public AiSuggestionServiceImpl(AiInstanceService aiInstanceService,
                                   AiCharacterService aiCharacterService,
                                   TranslationBillingService translationBillingService,
                                   ConsumptionBalanceGuard balanceGuard,
                                   AiSuggestionProperties properties,
                                   RestTemplateBuilder restTemplateBuilder,
                                   ObjectMapper objectMapper,
                                   CommunicateHistoryService communicateHistoryService) {
        this.aiInstanceService = aiInstanceService;
        this.aiCharacterService = aiCharacterService;
        this.translationBillingService = translationBillingService;
        this.balanceGuard = balanceGuard;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.communicateHistoryService = communicateHistoryService;
        long timeout = Math.max(properties.getTimeoutMs(), 1000L);
        Duration timeoutDuration = Duration.ofMillis(timeout);
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(timeoutDuration)
            .setReadTimeout(timeoutDuration)
            .build();
    }

    @Override
    public AiSuggestionResult suggest(AiSuggestionRequest request) {
        if (!properties.isEnabled()) {
            throw new ServiceException("AI建议服务暂未启用");
        }
        if (!StringUtils.hasText(properties.getEndpoint())) {
            throw new ServiceException("AI建议服务地址未配置");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null || currentUserId <= 0) {
            throw new ServiceException("用户未登录或会话已失效");
        }

        AiInstance instance = aiInstanceService.selectById(request.getInstanceId());
        if (instance == null) {
            throw new ServiceException("实例不存在或已删除");
        }
        boolean hasPermission = aiInstanceService.hasAccessPermission(instance.getInstanceId(), currentUserId)
            || SecurityUtils.isCurrentUserAdmin();
        if (!hasPermission) {
            throw new ServiceException("无权限访问该实例");
        }

        Integer characterId = instance.getCharacterId();
        if (characterId == null || characterId <= 0) {
            throw new ServiceException("当前实例未绑定AI人设");
        }
        AiCharacter character = aiCharacterService.selectById(characterId.longValue());
        if (character == null || !StringUtils.hasText(character.getPrompt())) {
            throw new ServiceException("人设不存在或缺少prompt配置");
        }

        balanceGuard.ensureSufficientBalance(
            currentUserId,
            translationBillingService.resolveUnitPrice(),
            properties.getSceneName()
        );

        List<AiSuggestionRequest.ChatRecord> history = request.getHistory();
        if (CollectionUtils.isEmpty(history)) {
            throw new ServiceException("history不能为空");
        }
        String contactUsername = resolveContactUsername(history);
        Integer platformId = instance.getPlatformId();
        if (platformId == null) {
            throw new ServiceException("实例未绑定平台，无法存储聊天历史");
        }
        CommunicateHistoryService.CommunicateHistorySnapshot snapshot =
            communicateHistoryService.loadSnapshot(currentUserId, contactUsername, platformId);

        AiSuggestionResult result = invokeAiService(request, character.getPrompt(), currentUserId, snapshot.chatPortrait());
        if (result == null) {
            throw new ServiceException("AI服务响应为空");
        }

        if (result.getCode() == 200 && result.getTotalTokens() > 0) {
            translationBillingService.deduct(currentUserId, result.getTotalTokens());
        } else if (result.getTotalTokens() > 0) {
            log.warn("Skip billing for AI suggestion due to non-success code={}, tokens={}",
                result.getCode(), result.getTotalTokens());
        }

        communicateHistoryService.mergeAndSaveAsync(snapshot, history);

        return result;
    }

    private AiSuggestionResult invokeAiService(AiSuggestionRequest request,
                                               String characterPrompt,
                                               Long userId,
                                               String chatPortrait) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("history", request.toPayloadHistory());
        payload.put("character", characterPrompt);

        String displayLang = resolveDisplayLanguage(request.getLang());
        if (StringUtils.hasText(displayLang)) {
            payload.put("lang", displayLang);
        }

        payload.put("user_id", userId);
        if (StringUtils.hasText(chatPortrait)) {
            payload.put("user_profile", chatPortrait);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            long start = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.postForEntity(
                properties.getEndpoint(), entity, String.class);
            long duration = System.currentTimeMillis() - start;
            String rawBody = response.getBody();
            log.info("AI suggestion raw response: status={}, duration={}ms, body={}",
                response.getStatusCode(), duration, rawBody);
            if (!StringUtils.hasText(rawBody)) {
                throw new ServiceException("AI服务未返回有效内容");
            }
            return objectMapper.readValue(rawBody, AiSuggestionResult.class);
        } catch (RestClientException ex) {
            log.error("调用AI建议服务失败，endpoint={}", properties.getEndpoint(), ex);
            throw new ServiceException("AI服务调用失败：" + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("AI建议响应解析失败", ex);
            throw new ServiceException("AI服务返回解析失败：" + ex.getMessage(), ex);
        }
    }

    private static String resolveDisplayLanguage(String lang) {
        if (!StringUtils.hasText(lang)) {
            return null;
        }
        String normalized = lang.trim().toLowerCase(Locale.ROOT);
        return LANGUAGE_DISPLAY_MAP.getOrDefault(normalized, lang);
    }

    private static Map<String, String> buildLanguageDisplayMap() {
        Map<String, String> map = new HashMap<>();
        map.put("zh", "中文");
        map.put("zh-cn", "中文");
        map.put("zh_cn", "中文");
        map.put("zh-hans", "中文");
        map.put("zh-tw", "繁體中文");
        map.put("zh_hk", "繁體中文");
        map.put("en", "English");
        map.put("en-us", "English");
        map.put("en-gb", "English");
        map.put("ja", "日本語");
        map.put("ko", "한국어");
        map.put("fr", "Français");
        map.put("de", "Deutsch");
        map.put("es", "Español");
        map.put("pt", "Português");
        map.put("ru", "Русский");
        map.put("ar", "العربية");
        map.put("th", "ไทย");
        map.put("vi", "Tiếng Việt");
        map.put("id", "Bahasa Indonesia");
        map.put("hi", "हिन्दी");
        map.put("it", "Italiano");
        map.put("ms", "Bahasa Melayu");
        return Collections.unmodifiableMap(map);
    }

    private String resolveContactUsername(List<AiSuggestionRequest.ChatRecord> history) {
        String contact = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            AiSuggestionRequest.ChatRecord record = history.get(i);
            if (!StringUtils.hasText(record.getRole()) || !StringUtils.hasText(record.getContact())) {
                continue;
            }
            String role = record.getRole().trim().toLowerCase(Locale.ROOT);
            if ("user".equals(role) && contact == null) {
                contact = record.getContact();
            }
            if (StringUtils.hasText(contact)) {
                break;
            }
        }
        if (!StringUtils.hasText(contact)) {
            throw new ServiceException("无法识别联系人用户名");
        }
        return contact;
    }

}
