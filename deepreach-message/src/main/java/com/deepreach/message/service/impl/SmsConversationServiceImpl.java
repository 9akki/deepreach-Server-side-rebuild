package com.deepreach.message.service.impl;

import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.domain.entity.DrPriceConfig;
import com.deepreach.common.core.dto.DeductResponse;
import com.deepreach.common.core.service.DrPriceConfigService;
import com.deepreach.common.core.service.UserDrBalanceService;
import com.deepreach.common.core.support.ChargeAccountResolver;
import com.deepreach.common.core.support.ConsumptionBalanceGuard;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.message.config.MessageSmsProperties;
import com.deepreach.message.dto.SmsContactListRequest;
import com.deepreach.message.dto.SmsContactSummary;
import com.deepreach.message.dto.SmsLegacyMessageSendRequest;
import com.deepreach.message.dto.SmsMessageQueryRequest;
import com.deepreach.message.dto.SmsMessageRecord;
import com.deepreach.message.dto.SmsMessageSendRequest;
import com.deepreach.message.dto.SmsSendLogResponse;
import com.deepreach.message.dto.SmsSendResult;
import com.deepreach.message.entity.SmsHistory;
import com.deepreach.message.entity.SmsTask;
import com.deepreach.message.mapper.SmsHistoryMapper;
import com.deepreach.message.mapper.SmsTaskMapper;
import com.deepreach.message.service.SmsConversationService;
import com.deepreach.message.service.SmsGatewayClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsConversationServiceImpl implements SmsConversationService {

    private static final BigDecimal DEFAULT_SMS_UNIT_PRICE = new BigDecimal("0.05");

    private final SmsGatewayClient smsGatewayClient;
    private final SmsTaskMapper smsTaskMapper;
    private final SmsHistoryMapper smsHistoryMapper;
    private final UserDrBalanceService userDrBalanceService;
    private final DrPriceConfigService drPriceConfigService;
    private final MessageSmsProperties messageSmsProperties;
    private final ChargeAccountResolver chargeAccountResolver;
    private final ConsumptionBalanceGuard balanceGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmsSendLogResponse sendMessage(SmsMessageSendRequest request) {
        SmsTask task = loadTask(request.getTaskId());
        if (request.getUserId() != null && !task.getUserId().equals(request.getUserId())) {
            throw new ServiceException("任务与用户不匹配");
        }
        BigDecimal unitPrice = resolveSmsUnitPrice();
        ChargeAccountResolver.ChargeAccount account = balanceGuard.ensureSufficientBalance(task.getUserId(), unitPrice, "短信发送");
        String normalizedNumber = normalizeNumber(request.getTargetNumber());
        String messageTo = StringUtils.hasText(request.getMessageTo())
            ? normalizeNumber(request.getMessageTo())
            : normalizedNumber;
        String defaultSource = resolveDefaultSource(task);
        String outboundSource = StringUtils.hasText(request.getMessageFrom())
            ? normalizeNumber(request.getMessageFrom())
            : defaultSource;
        LocalDateTime sentAt = parseSendTime(request.getSentAt());

        SmsGatewayClient.SendCommand command = new SmsGatewayClient.SendCommand();
        command.setTo(normalizedNumber);
        command.setSource(outboundSource);
        command.setBody(request.getMessageContent());
        command.setImageUrl(extractFirstMedia(request.getMediaUrls()));

        SmsGatewayClient.SendResult sendResult = smsGatewayClient.send(command);
        int computedStatus = sendResult.isSuccess() ? 0 : 1;
        int finalStatus = request.getStatus() != null ? request.getStatus() : computedStatus;
        Integer readFlag = request.getRead();
        if (readFlag == null) {
            readFlag = Objects.equals(messageTo, outboundSource) ? 1 : 0;
        }

        String persistedSource = StringUtils.hasText(sendResult.getSourceId())
            ? normalizeNumber(sendResult.getSourceId())
            : outboundSource;

        SmsHistory history = new SmsHistory();
        history.setTaskId(task.getId());
        history.setTargetNumber(normalizedNumber);
        history.setMessageContent(request.getMessageContent());
        history.setMediaUrls(request.getMediaUrls());
        history.setMessageTo(messageTo);
        history.setMessageFrom(persistedSource);
        history.setSentAt(sentAt);
        history.setStatus(finalStatus);
        history.setRead(readFlag);
        smsHistoryMapper.insertHistory(history);

        smsTaskMapper.incrementTotalCount(task.getId(), 1);
        if (finalStatus == 0) {
            smsTaskMapper.incrementSentCount(task.getId(), 1);
            deductBalance(account, unitPrice, "短信发送扣费");
        } else {
            log.warn("短信发送失败, taskId={}, number={}, message={}", task.getId(), normalizedNumber, sendResult.getMessage());
        }
        Object gatewayPayload = sendResult.getRawPayload() != null ? sendResult.getRawPayload() : sendResult;
        return new SmsSendLogResponse(history.getId(), finalStatus, readFlag, gatewayPayload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmsSendResult sendMessageLegacy(SmsLegacyMessageSendRequest request) {
        SmsTask task = loadTask(request.getTaskId());
        BigDecimal unitPrice = resolveSmsUnitPrice();
        ChargeAccountResolver.ChargeAccount account = balanceGuard.ensureSufficientBalance(task.getUserId(), unitPrice, "短信发送");
        String normalizedNumber = normalizeNumber(request.getTargetNumber());
        String messageFrom = normalizeNumber(Optional.ofNullable(request.getMessageFrom()).orElse("default"));
        LocalDateTime sentAt = parseSendTime(request.getSentAt());

        SmsGatewayClient.SendCommand command = new SmsGatewayClient.SendCommand();
        command.setTo(normalizedNumber);
        command.setSource(messageFrom);
        command.setBody(request.getMessageContent());
        command.setImageUrl(extractFirstMedia(request.getMediaUrls()));

        SmsGatewayClient.SendResult sendResult = smsGatewayClient.send(command);
        int status = sendResult.isSuccess() ? 0 : 1;
        String upstreamSource = StringUtils.hasText(sendResult.getSourceId()) ? sendResult.getSourceId() : messageFrom;
        if (!sendResult.isSuccess()) {
            log.warn("Legacy短信发送失败 taskId={} reason={}", task.getId(), sendResult.getMessage());
        }

        SmsHistory history = new SmsHistory();
        history.setTaskId(task.getId());
        history.setTargetNumber(normalizedNumber);
        history.setMessageContent(request.getMessageContent());
        history.setMediaUrls(request.getMediaUrls());
        history.setMessageTo(normalizedNumber);
        history.setMessageFrom(upstreamSource);
        history.setSentAt(sentAt);
        history.setStatus(status);
        history.setRead(1);
        smsHistoryMapper.insertHistory(history);

        smsTaskMapper.incrementTotalCount(task.getId(), 1);
        if (status == 0) {
            smsTaskMapper.incrementSentCount(task.getId(), 1);
            deductBalance(account, unitPrice, "旧版短信发送扣费");
        }
        return new SmsSendResult(history.getId(), history.getStatus(), history.getRead());
    }

    @Override
    public List<SmsContactSummary> listContactsByTask(SmsContactListRequest request) {
        int page = Optional.ofNullable(request.getPage()).orElse(1);
        int size = Optional.ofNullable(request.getSize()).orElse(20);
        int limit = Math.max(size, 1);
        int offset = Math.max(page, 1);
        offset = (offset - 1) * limit;
        return smsHistoryMapper.listContactsWithLatestMessagePaged(request.getTaskId(), limit, offset);
    }

    @Override
    public List<SmsMessageRecord> listMessagesByContact(SmsMessageQueryRequest request) {
        int page = Optional.ofNullable(request.getPage()).orElse(1);
        int size = Optional.ofNullable(request.getSize()).orElse(20);
        int limit = Math.max(size, 1);
        int offset = Math.max(page, 1);
        offset = (offset - 1) * limit;
        return smsHistoryMapper.listMessagesByContactPaged(
            request.getTaskId(),
            normalizeNumber(request.getTargetNumber()),
            limit,
            offset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMessageRead(Long taskId, String targetNumber) {
        smsHistoryMapper.updateReadFlag(taskId, normalizeNumber(targetNumber), 1);
    }

    private SmsTask loadTask(Long taskId) {
        SmsTask task = smsTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("短信任务不存在");
        }
        return task;
    }

    private String normalizeNumber(String number) {
        if (!StringUtils.hasText(number)) {
            return number;
        }
        String normalized = number.trim();
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        return normalized.replaceAll("\\s+", "");
    }

    private String resolveDefaultSource(SmsTask task) {
        if (StringUtils.hasText(messageSmsProperties.getSenderNumber())) {
            return normalizeNumber(messageSmsProperties.getSenderNumber());
        }
        if (task.getInstanceId() != null) {
            return normalizeNumber(task.getInstanceId().toString());
        }
        return "default";
    }

    private void deductBalance(ChargeAccountResolver.ChargeAccount account, BigDecimal unitPrice, String description) {
        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        DrBillingRecord record = buildSmsBillingRecord(unitPrice, account, description);
        DeductResponse response = userDrBalanceService.deductWithDetails(record, account.getOperatorUserId());
        if (response == null || !response.isSuccess()) {
            String message = response != null ? response.getMessage() : "扣费响应为空";
            throw new ServiceException("短信扣费失败: " + message);
        }
    }

    private DrBillingRecord buildSmsBillingRecord(BigDecimal amount,
                                                  ChargeAccountResolver.ChargeAccount account,
                                                  String description) {
        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(account.getChargeUserId());
        record.setOperatorId(account.getOperatorUserId());
        record.setBillType(2);
        record.setBillingType(1);
        record.setBusinessType(DrBillingRecord.BUSINESS_TYPE_SMS);
        record.setDrAmount(amount);
        record.setDescription(StringUtils.hasText(description) ? description : "短信扣费");
        record.setRemark("SmsSend");
        return record;
    }

    private BigDecimal resolveSmsUnitPrice() {
        DrPriceConfig config = drPriceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_SMS);
        if (config == null || config.getDrPrice() == null) {
            log.warn("短信计价配置缺失，使用默认单价 {}", DEFAULT_SMS_UNIT_PRICE);
            return DEFAULT_SMS_UNIT_PRICE;
        }
        return config.getDrPrice();
    }

    private LocalDateTime parseSendTime(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        try {
            if (value.contains("T")) {
                return OffsetDateTime.parse(value.replace("Z", "+00:00")).toLocalDateTime();
            }
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ex) {
            log.warn("无法解析发送时间: {}", value);
            return LocalDateTime.now();
        }
    }

    private String extractFirstMedia(String mediaUrls) {
        if (!StringUtils.hasText(mediaUrls)) {
            return null;
        }
        String[] parts = mediaUrls.split(";");
        return parts.length > 0 ? parts[0].trim() : null;
    }
}
