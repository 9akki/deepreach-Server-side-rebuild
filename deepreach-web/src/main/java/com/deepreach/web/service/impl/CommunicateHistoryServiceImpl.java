package com.deepreach.web.service.impl;

import com.deepreach.common.core.config.ChatHistoryAiProperties;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.web.domain.dto.AiSuggestionRequest;
import com.deepreach.web.entity.CommunicateHistory;
import com.deepreach.web.mapper.CommunicateHistoryMapper;
import com.deepreach.web.service.CommunicateHistoryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicateHistoryServiceImpl implements CommunicateHistoryService {

    private static final DateTimeFormatter PLAIN_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final TypeReference<List<AiSuggestionRequest.ChatRecord>> HISTORY_TYPE =
        new TypeReference<>() {};

    private final CommunicateHistoryMapper historyMapper;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatHistoryAiProperties chatHistoryAiProperties;
    private final RestTemplateBuilder restTemplateBuilder;
    private volatile RestTemplate sortRestTemplate;

    @Override
    public CommunicateHistorySnapshot loadSnapshot(Long userId,
                                                   String contactUsername,
                                                   Integer platformId) {
        if (userId == null || !StringUtils.hasText(contactUsername) || platformId == null) {
            throw new ServiceException("聊天信息缺少 user/contact/platformId");
        }
        String cacheKey = buildPortraitCacheKey(userId, contactUsername, platformId);
        Object cachedPortrait = redisTemplate.opsForValue().get(cacheKey);

        CommunicateHistory record = historyMapper.selectByPk(userId, contactUsername, platformId);
        String portrait = cachedPortrait instanceof String
            ? (String) cachedPortrait
            : (record != null ? record.getChatPortrait() : null);
        if (cachedPortrait == null && portrait != null) {
            redisTemplate.opsForValue().set(cacheKey, portrait);
        }

        String historyJson = record != null ? record.getHistorySplice() : null;
        return new CommunicateHistorySnapshot(userId, contactUsername, platformId, historyJson, portrait);
    }

    @Override
    @Async("chatHistoryExecutor")
    public void mergeAndSaveAsync(CommunicateHistorySnapshot snapshot,
                                  List<AiSuggestionRequest.ChatRecord> incomingRecords) {
        mergeAndSaveInternal(snapshot, incomingRecords);
    }

    private void mergeAndSaveInternal(CommunicateHistorySnapshot snapshot,
                                      List<AiSuggestionRequest.ChatRecord> incomingRecords) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot不能为空");
        }
        List<AiSuggestionRequest.ChatRecord> existing = parseHistory(snapshot.historyJson());
        List<AiSuggestionRequest.ChatRecord> incoming = incomingRecords == null
            ? Collections.emptyList()
            : incomingRecords;
        if (incoming.isEmpty()) {
            return;
        }

        List<AiSuggestionRequest.ChatRecord> merged;
        if (hasFullTimestamps(existing) && hasFullTimestamps(incoming)) {
            merged = mergeByTimestamp(existing, incoming);
        } else {
            merged = mergeByHeuristic(existing, incoming);
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(merged);
        } catch (Exception ex) {
            throw new ServiceException("聊天记录序列化失败", ex);
        }

        LocalDateTime now = LocalDateTime.now();
        historyMapper.upsertHistory(snapshot.userId(), snapshot.contactUsername(),
            snapshot.platformId(), payload, now);
    }

    private List<AiSuggestionRequest.ChatRecord> parseHistory(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, HISTORY_TYPE);
        } catch (Exception ex) {
            log.warn("解析 history_splice 失败，将重建记录: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean hasFullTimestamps(List<AiSuggestionRequest.ChatRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return true;
        }
        for (AiSuggestionRequest.ChatRecord record : records) {
            if (!StringUtils.hasText(record.getTimestamp())) {
                return false;
            }
        }
        return true;
    }

    private List<AiSuggestionRequest.ChatRecord> mergeByTimestamp(List<AiSuggestionRequest.ChatRecord> existing,
                                                                  List<AiSuggestionRequest.ChatRecord> incoming) {
        List<AiSuggestionRequest.ChatRecord> merged = new ArrayList<>();
        existing.forEach(record -> merged.add(record.copy()));
        incoming.forEach(record -> merged.add(record.copy()));
        merged.sort(Comparator.comparing(this::safeParseTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));
        return deduplicate(merged);
    }

    private List<AiSuggestionRequest.ChatRecord> mergeByHeuristic(List<AiSuggestionRequest.ChatRecord> existing,
                                                                  List<AiSuggestionRequest.ChatRecord> incoming) {
        List<AiSuggestionRequest.ChatRecord> merged = new ArrayList<>();
        existing.forEach(record -> merged.add(record.copy()));
        List<AiSuggestionRequest.ChatRecord> mutableIncoming = incoming.stream()
            .map(AiSuggestionRequest.ChatRecord::copy)
            .toList();

        List<String> baseSig = buildSignatureList(merged);
        List<String> incomingSig = buildSignatureList(mutableIncoming);
        int overlap = findOverlap(baseSig, incomingSig);
        if (overlap >= mutableIncoming.size()) {
            return merged;
        }
        if (shouldInvokeSortService(overlap, merged, mutableIncoming)) {
            List<AiSuggestionRequest.ChatRecord> sorted = sortHistoryWithAi(merged, mutableIncoming);
            if (!sorted.isEmpty()) {
                return deduplicate(sorted);
            }
        }
        int start = Math.max(overlap, 0);
        for (int i = start; i < mutableIncoming.size(); i++) {
            merged.add(mutableIncoming.get(i));
        }
        return deduplicate(merged);
    }

    private List<String> buildSignatureList(List<AiSuggestionRequest.ChatRecord> records) {
        List<String> signatures = new ArrayList<>();
        for (AiSuggestionRequest.ChatRecord record : records) {
            signatures.add(String.join("|",
                defaultString(record.getRole()),
                defaultString(record.getContact()),
                defaultString(record.getContent())));
        }
        return signatures;
    }

    private int findOverlap(List<String> base, List<String> incoming) {
        int max = Math.min(base.size(), incoming.size());
        for (int len = max; len > 0; len--) {
            boolean match = true;
            for (int i = 0; i < len; i++) {
                if (!Objects.equals(base.get(base.size() - len + i), incoming.get(i))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return len;
            }
        }
        return 0;
    }

    private List<AiSuggestionRequest.ChatRecord> deduplicate(List<AiSuggestionRequest.ChatRecord> records) {
        List<AiSuggestionRequest.ChatRecord> unique = new ArrayList<>();
        AiSuggestionRequest.ChatRecord previous = null;
        for (AiSuggestionRequest.ChatRecord record : records) {
            if (previous != null && isSameRecord(previous, record)) {
                continue;
            }
            unique.add(record);
            previous = record;
        }
        return unique;
    }

    private boolean isSameRecord(AiSuggestionRequest.ChatRecord a, AiSuggestionRequest.ChatRecord b) {
        return Objects.equals(defaultString(a.getTimestamp()), defaultString(b.getTimestamp()))
            && Objects.equals(defaultString(a.getRole()), defaultString(b.getRole()))
            && Objects.equals(defaultString(a.getContact()), defaultString(b.getContact()))
            && Objects.equals(defaultString(a.getContent()), defaultString(b.getContent()));
    }

    private boolean shouldInvokeSortService(int overlap,
                                            List<AiSuggestionRequest.ChatRecord> existing,
                                            List<AiSuggestionRequest.ChatRecord> incoming) {
        return overlap == 0
            && !CollectionUtils.isEmpty(existing)
            && !CollectionUtils.isEmpty(incoming)
            && StringUtils.hasText(chatHistoryAiProperties.getSortEndpoint());
    }

    private List<AiSuggestionRequest.ChatRecord> sortHistoryWithAi(
        List<AiSuggestionRequest.ChatRecord> existing,
        List<AiSuggestionRequest.ChatRecord> incoming) {
        try {
            RestTemplate template = getSortRestTemplate();
            if (template == null) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> historyPayload = new ArrayList<>();
            existing.forEach(record -> historyPayload.add(buildSortPayload(record, "history_splice")));
            incoming.forEach(record -> historyPayload.add(buildSortPayload(record, "new_record")));

            Map<String, Object> payload = new HashMap<>();
            payload.put("history", historyPayload);
            payload.put("instruction", chatHistoryAiProperties.getSortInstruction());
            payload.put("character", chatHistoryAiProperties.getSortCharacter());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = template.postForEntity(
                chatHistoryAiProperties.getSortEndpoint(), entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                log.warn("聊天排序兜底接口返回异常，status={}", response.getStatusCode());
                return Collections.emptyList();
            }
            SortResponse sortResponse = objectMapper.readValue(response.getBody(), SortResponse.class);
            if (sortResponse == null || sortResponse.getCode() != 200 || CollectionUtils.isEmpty(sortResponse.getMessage())) {
                log.warn("聊天排序兜底接口返回失败 code={}", sortResponse != null ? sortResponse.getCode() : null);
                return Collections.emptyList();
            }
            return convertSortedHistory(sortResponse.getMessage());
        } catch (Exception ex) {
            log.warn("调用聊天排序兜底接口失败", ex);
            return Collections.emptyList();
        }
    }

    private Map<String, Object> buildSortPayload(AiSuggestionRequest.ChatRecord record, String source) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", record.getRole());
        map.put("contact", record.getContact());
        map.put("content", record.getContent());
        map.put("timestamp", defaultString(record.getTimestamp()));
        map.put("source", source);
        return map;
    }

    private List<AiSuggestionRequest.ChatRecord> convertSortedHistory(List<SortRecord> records) {
        List<AiSuggestionRequest.ChatRecord> result = new ArrayList<>();
        for (SortRecord record : records) {
            AiSuggestionRequest.ChatRecord chatRecord = new AiSuggestionRequest.ChatRecord();
            chatRecord.setRole(record.getRole());
            chatRecord.setContact(record.getContact());
            chatRecord.setContent(record.getContent());
            chatRecord.setTimestamp(record.getTimestamp());
            result.add(chatRecord);
        }
        return result;
    }

    private RestTemplate getSortRestTemplate() {
        if (!StringUtils.hasText(chatHistoryAiProperties.getSortEndpoint())) {
            return null;
        }
        if (sortRestTemplate == null) {
            synchronized (this) {
                if (sortRestTemplate == null) {
                    long timeout = Math.max(chatHistoryAiProperties.getTimeoutMs(), 1000L);
                    sortRestTemplate = restTemplateBuilder
                        .setConnectTimeout(Duration.ofMillis(timeout))
                        .setReadTimeout(Duration.ofMillis(timeout))
                        .build();
                }
            }
        }
        return sortRestTemplate;
    }

    private LocalDateTime safeParseTimestamp(AiSuggestionRequest.ChatRecord record) {
        if (record == null || !StringUtils.hasText(record.getTimestamp())) {
            return null;
        }
        String raw = record.getTimestamp().trim();
        try {
            if (raw.contains("T")) {
                return LocalDateTime.parse(raw);
            }
            return LocalDateTime.parse(raw, PLAIN_TS);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private void cachePortrait(Long userId, String contactUsername, Integer platformId, String portrait) {
        if (!StringUtils.hasText(portrait)) {
            return;
        }
        String key = buildPortraitCacheKey(userId, contactUsername, platformId);
        redisTemplate.opsForValue().set(key, portrait);
    }

    private String buildPortraitCacheKey(Long userId, String contactUsername, Integer platformId) {
        return "chat:portrait:" + userId + ":" + contactUsername + ":" + platformId;
    }

    private static class SortResponse {
        private int code;
        private List<SortRecord> message;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public List<SortRecord> getMessage() {
            return message;
        }

        public void setMessage(List<SortRecord> message) {
            this.message = message;
        }
    }

    private static class SortRecord {
        private String role;
        private String contact;
        private String content;
        private String timestamp;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContact() {
            return contact;
        }

        public void setContact(String contact) {
            this.contact = contact;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}
