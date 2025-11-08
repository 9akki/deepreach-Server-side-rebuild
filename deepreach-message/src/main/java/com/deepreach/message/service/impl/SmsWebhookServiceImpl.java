package com.deepreach.message.service.impl;

import com.deepreach.common.exception.ServiceException;
import com.deepreach.message.dto.SmsWebhookPayload;
import com.deepreach.message.entity.SmsHistory;
import com.deepreach.message.mapper.SmsHistoryMapper;
import com.deepreach.message.mapper.SmsTaskMapper;
import com.deepreach.message.service.SmsWebhookService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsWebhookServiceImpl implements SmsWebhookService {

    private final SmsHistoryMapper smsHistoryMapper;
    private final SmsTaskMapper smsTaskMapper;

    @Override
    public SmsWebhookPayload normalizePayload(SmsWebhookPayload payload) {
        if (payload == null) {
            throw new ServiceException("Webhook payload 为空");
        }
        if (!StringUtils.hasText(payload.getMessageContent())) {
            throw new ServiceException("Webhook 缺少消息内容");
        }
        payload.setMessageFrom(cleanNumber(payload.getMessageFrom()));
        if (!StringUtils.hasText(payload.getMessageFrom())) {
            throw new ServiceException("Webhook 缺少来源号码");
        }
        if (!StringUtils.hasText(payload.getMessageTo())) {
            payload.setMessageTo(cleanNumber(payload.getSource()));
        } else {
            payload.setMessageTo(cleanNumber(payload.getMessageTo()));
        }
        payload.setTargetNumber(payload.getMessageFrom());
        if (StringUtils.hasText(payload.getMediaUrls())) {
            String[] parts = payload.getMediaUrls().split(";");
            StringBuilder builder = new StringBuilder();
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty() || !trimmed.startsWith("http")) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(";");
                }
                builder.append(trimmed);
            }
            payload.setMediaUrls(builder.toString());
        }
        return payload;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmsWebhookPayload handleInbound(SmsWebhookPayload payload) {
        SmsWebhookPayload normalized = normalizePayload(payload);
        Long taskId = smsHistoryMapper.findTaskIdByFromAndTo(normalized.getMessageFrom(), normalized.getMessageTo());
        if (taskId == null) {
            throw new ServiceException("未找到匹配任务");
        }
        SmsHistory history = new SmsHistory();
        history.setTaskId(taskId);
        history.setTargetNumber(normalized.getMessageFrom());
        history.setMessageContent(normalized.getMessageContent());
        history.setMediaUrls(normalized.getMediaUrls());
        history.setMessageTo(normalized.getMessageTo());
        history.setMessageFrom(normalized.getMessageFrom());
        history.setSentAt(parseTime(normalized.getReceivedDatetime()));
        history.setStatus(0);
        history.setRead(0);
        smsHistoryMapper.insertHistory(history);
        smsTaskMapper.incrementReplyCount(taskId, 1);
        log.info("Webhook inbound linked to taskId={}, from={}, to={}", taskId, normalized.getMessageFrom(), normalized.getMessageTo());
        return normalized;
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(value.trim()).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            log.warn("无法解析Webhook时间: {}", value);
            return LocalDateTime.now();
        }
    }

    private String cleanNumber(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String normalized = raw.trim();
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        return normalized.replaceAll("\\s+", "");
    }
}
