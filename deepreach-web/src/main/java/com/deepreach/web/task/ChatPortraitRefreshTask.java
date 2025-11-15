package com.deepreach.web.task;

import com.deepreach.common.core.config.ChatHistoryAiProperties;
import com.deepreach.web.entity.CommunicateHistory;
import com.deepreach.web.mapper.CommunicateHistoryMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPortraitRefreshTask {

    private static final TypeReference<List<Map<String, Object>>> HISTORY_TYPE =
        new TypeReference<>() {};
    private static final Pattern USER_PROFILE_PATTERN =
        Pattern.compile("<user_profile>(.*?)</user_profile>", Pattern.DOTALL);
    private static final Pattern PREVIOUS_SUMMARY_PATTERN =
        Pattern.compile("<previous_summary>(.*?)</previous_summary>", Pattern.DOTALL);

    private final CommunicateHistoryMapper historyMapper;
    private final ChatHistoryAiProperties chatHistoryAiProperties;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplateBuilder restTemplateBuilder;
    private volatile RestTemplate profileRestTemplate;

    @Scheduled(fixedDelayString = "${chat.history.ai.profile-refresh-interval-ms}")
    public void refreshPortraits() {
        if (!chatHistoryAiProperties.isProfileEnabled()
            || !StringUtils.hasText(chatHistoryAiProperties.getProfileEndpoint())) {
            return;
        }
        List<CommunicateHistory> pending = historyMapper.selectNeedPortraitUpdate(
            chatHistoryAiProperties.getProfileBatchSize());
        if (CollectionUtils.isEmpty(pending)) {
            return;
        }
        for (CommunicateHistory record : pending) {
            try {
                refreshPortrait(record);
            } catch (Exception ex) {
                log.warn("刷新聊天画像失败 userId={} contact={} platformId={}",
                    record.getUserId(), record.getContactUsername(), record.getPlatformId(), ex);
            }
        }
    }

    private void refreshPortrait(CommunicateHistory record) throws Exception {
        RestTemplate template = getProfileRestTemplate();
        if (template == null) {
            return;
        }
        List<Map<String, Object>> history = parseHistory(record.getHistorySplice());
        if (history.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("history", normalizeHistory(history));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response;
        try {
            response = template.postForEntity(chatHistoryAiProperties.getProfileEndpoint(), entity, String.class);
        } catch (RestClientException ex) {
            log.warn("调用画像接口失败 userId={} contact={} platformId={}",
                record.getUserId(), record.getContactUsername(), record.getPlatformId(), ex);
            return;
        }
        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
            log.warn("画像接口返回异常 status={} userId={} contact={}",
                response.getStatusCode(), record.getUserId(), record.getContactUsername());
            return;
        }
        ProfileResponse profileResponse =
            objectMapper.readValue(response.getBody(), ProfileResponse.class);
        if (profileResponse == null || profileResponse.getCode() != 200
            || !StringUtils.hasText(profileResponse.getMessage())) {
            log.warn("画像接口返回失败 code={} userId={} contact={}",
                profileResponse != null ? profileResponse.getCode() : null,
                record.getUserId(), record.getContactUsername());
            return;
        }
        String portraitJson = buildPortraitJson(profileResponse.getMessage());
        if (!StringUtils.hasText(portraitJson)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        historyMapper.updatePortrait(
            record.getUserId(), record.getContactUsername(), record.getPlatformId(),
            portraitJson, now);
        String cacheKey = buildPortraitCacheKey(record.getUserId(), record.getContactUsername(), record.getPlatformId());
        redisTemplate.opsForValue().set(cacheKey, portraitJson);
    }

    private List<Map<String, Object>> parseHistory(String json) throws Exception {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        return objectMapper.readValue(json, HISTORY_TYPE);
    }

    private List<Map<String, String>> normalizeHistory(List<Map<String, Object>> origin) {
        if (CollectionUtils.isEmpty(origin)) {
            return Collections.emptyList();
        }
        List<Map<String, String>> normalized = new ArrayList<>();
        for (Map<String, Object> entry : origin) {
            if (entry == null) {
                continue;
            }
            Map<String, String> payload = new HashMap<>();
            Object role = entry.getOrDefault("role", "user");
            Object content = entry.getOrDefault("content", "");
            payload.put("role", role != null ? role.toString() : "user");
            payload.put("content", content != null ? content.toString() : "");
            normalized.add(payload);
        }
        return normalized;
    }

    private String buildPortraitJson(String rawMessage) throws Exception {
        String userProfile = extractTag(rawMessage, USER_PROFILE_PATTERN);
        String previousSummary = extractTag(rawMessage, PREVIOUS_SUMMARY_PATTERN);
        Map<String, Object> portrait = new HashMap<>();
        portrait.put("summary", StringUtils.hasText(userProfile) ? userProfile : rawMessage);
        portrait.put("previousSummary", previousSummary);
        portrait.put("lastUpdated", LocalDateTime.now().toString());
        return objectMapper.writeValueAsString(portrait);
    }

    private String extractTag(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private RestTemplate getProfileRestTemplate() {
        if (!StringUtils.hasText(chatHistoryAiProperties.getProfileEndpoint())) {
            return null;
        }
        if (profileRestTemplate == null) {
            synchronized (this) {
                if (profileRestTemplate == null) {
                    long timeout = Math.max(chatHistoryAiProperties.getTimeoutMs(), 1000L);
                    profileRestTemplate = restTemplateBuilder
                        .setConnectTimeout(Duration.ofMillis(timeout))
                        .setReadTimeout(Duration.ofMillis(timeout))
                        .build();
                }
            }
        }
        return profileRestTemplate;
    }

    private String buildPortraitCacheKey(Long userId, String contactUsername, Integer platformId) {
        return "chat:portrait:" + userId + ":" + contactUsername + ":" + platformId;
    }

    private static class ProfileResponse {
        private int code;
        private String message;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
