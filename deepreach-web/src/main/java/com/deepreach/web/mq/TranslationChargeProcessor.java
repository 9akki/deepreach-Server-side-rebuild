package com.deepreach.web.mq;

import com.deepreach.common.core.config.TranslateBillingProperties;
import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.dto.DeductResponse;
import com.deepreach.common.core.mq.event.TranslationChargeEvent;
import com.deepreach.common.core.service.UserDrBalanceService;
import com.deepreach.common.core.support.ConsumptionBalanceGuard;
import com.deepreach.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationChargeProcessor {

    private static final int MAX_RETRY = 3;

    private final UserDrBalanceService userDrBalanceService;
    private final TranslateBillingProperties billingProperties;
    private final ConsumptionBalanceGuard balanceGuard;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void process(TranslationChargeEvent event, boolean allowDlqForward) {
        if (event == null) {
            return;
        }
        if (event.getChargeUserId() == null || event.getAmount() == null || event.getAmount().signum() <= 0) {
            throw new ServiceException("翻译扣费事件数据异常，chargeUserId或amount缺失");
        }

        ServiceException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                DeductResponse response = executeDeduct(event);
                if (response != null && response.isSuccess()) {
                    balanceGuard.evictBalance(event.getChargeUserId());
                    log.debug("翻译扣费事件处理成功 eventId={} chargeUserId={} amount={} attempt={}",
                        event.getEventId(), event.getChargeUserId(), event.getAmount(), attempt);
                    return;
                }
                lastException = new ServiceException("翻译扣费入账失败：" +
                    (response != null ? response.getMessage() : "扣费响应为空"));
                if (!isRetriable(response)) {
                    break;
                }
                log.warn("翻译扣费事件处理失败，准备重试 attempt={} eventId={} reason={}",
                    attempt, event.getEventId(), lastException.getMessage());
            } catch (ServiceException ex) {
                lastException = ex;
                if (!isRetriable(ex)) {
                    break;
                }
                log.warn("翻译扣费事件处理异常，准备重试 attempt={} eventId={} reason={}",
                    attempt, event.getEventId(), ex.getMessage());
            }
        }

        if (allowDlqForward) {
            sendToDlq(event, lastException);
        }
        throw lastException != null ? lastException :
            new ServiceException("翻译扣费入账失败：未知错误");
    }

    private DeductResponse executeDeduct(TranslationChargeEvent event) {
        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(event.getChargeUserId());
        record.setOperatorId(event.getOperatorUserId());
        record.setBillType(event.getBillType() != null ? Integer.valueOf(event.getBillType()) : DrBillingRecord.BILL_TYPE_CONSUME);
        record.setBillingType(event.getBillingType() != null ? Integer.valueOf(event.getBillingType()) : DrBillingRecord.BILLING_TYPE_REALTIME);
        record.setBusinessType(StringUtils.hasText(event.getBusinessType()) ? event.getBusinessType() : DrBillingRecord.BUSINESS_TYPE_TOKEN);
        record.setDrAmount(event.getAmount());
        record.setDescription(resolveDescription(event));
        record.setRemark(StringUtils.hasText(event.getRemark()) ? event.getRemark() : "TranslationChargeAsync");

        return userDrBalanceService.deductWithDailyAggregation(record, event.getOperatorUserId());
    }

    private void sendToDlq(TranslationChargeEvent event, Exception ex) {
        String topic = billingProperties.getDlqTopic();
        if (!StringUtils.hasText(topic)) {
            log.error("DLQ topic 未配置，无法转发 eventId={} reason={}", event.getEventId(),
                ex != null ? ex.getMessage() : "unknown");
            return;
        }
        TranslationChargeEvent dlqEvent = cloneForDlq(event);
        try {
            kafkaTemplate.send(topic,
                dlqEvent.getChargeUserId() != null ? dlqEvent.getChargeUserId().toString() : null,
                dlqEvent);
            log.warn("翻译扣费事件已写入DLQ topic={} eventId={} reason={}",
                topic, event.getEventId(), ex != null ? ex.getMessage() : "unknown");
        } catch (Exception sendEx) {
            log.error("发送翻译扣费事件到DLQ失败 eventId={}", event.getEventId(), sendEx);
        }
    }

    private TranslationChargeEvent cloneForDlq(TranslationChargeEvent source) {
        int retryCount = source.getRetryCount() != null ? source.getRetryCount() : 0;
        return TranslationChargeEvent.builder()
            .eventId(source.getEventId())
            .requestUserId(source.getRequestUserId())
            .chargeUserId(source.getChargeUserId())
            .operatorUserId(source.getOperatorUserId())
            .totalTokens(source.getTotalTokens())
            .amount(source.getAmount())
            .billType(source.getBillType())
            .billingType(source.getBillingType())
            .businessType(source.getBusinessType())
            .description(source.getDescription())
            .remark(source.getRemark())
            .pricingMode(source.getPricingMode())
            .occurredAt(source.getOccurredAt())
            .retryCount(retryCount + 1)
            .build();
    }

    private String resolveDescription(TranslationChargeEvent event) {
        if (StringUtils.hasText(event.getDescription())) {
            return event.getDescription();
        }
        if (StringUtils.hasText(billingProperties.getDescription())) {
            return billingProperties.getDescription();
        }
        return "翻译扣费";
    }

    private boolean isRetriable(DeductResponse response) {
        if (response == null) {
            return true;
        }
        String message = response.getMessage();
        return StringUtils.hasText(message) && message.contains("重试");
    }

    private boolean isRetriable(Exception ex) {
        return ex != null && StringUtils.hasText(ex.getMessage()) && ex.getMessage().contains("重试");
    }
}
