package com.deepreach.web.mq;

import com.deepreach.common.core.config.TranslateBillingProperties;
import com.deepreach.common.core.mq.event.TranslationChargeEvent;
import com.deepreach.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationChargeDlqConsumer {

    private static final int MAX_DLQ_REPLAYS = 5;
    private static final long RETRY_DELAY_MS = 2_000;

    private final TranslationChargeProcessor processor;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TranslateBillingProperties billingProperties;

    @KafkaListener(
        topics = "${translate.billing.dlq-topic:translation-charge-dlq}",
        containerFactory = "deepreachKafkaListenerFactory"
    )
    public void onDlqMessage(TranslationChargeEvent event) {
        if (event == null) {
            return;
        }
        int retryCount = event.getRetryCount() != null ? event.getRetryCount() : 0;
        try {
            processor.process(event, false);
            log.info("DLQ 自动补扣成功 eventId={} chargeUserId={} amount={} retries={}",
                event.getEventId(), event.getChargeUserId(), event.getAmount(), retryCount);
        } catch (ServiceException ex) {
            if (retryCount >= MAX_DLQ_REPLAYS) {
                log.error("DLQ 消息多次自动补扣失败，需人工处理 eventId={} chargeUserId={} amount={}",
                    event.getEventId(), event.getChargeUserId(), event.getAmount(), ex);
                throw ex;
            }
            log.warn("DLQ 自动补扣失败，准备重新入队 eventId={} chargeUserId={} amount={} retryCount={}",
                event.getEventId(), event.getChargeUserId(), event.getAmount(), retryCount + 1, ex);
            requeue(event, retryCount + 1);
        }
    }

    private void requeue(TranslationChargeEvent event, int nextRetryCount) {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        TranslationChargeEvent nextEvent = TranslationChargeEvent.builder()
            .eventId(event.getEventId())
            .requestUserId(event.getRequestUserId())
            .chargeUserId(event.getChargeUserId())
            .operatorUserId(event.getOperatorUserId())
            .totalTokens(event.getTotalTokens())
            .amount(event.getAmount())
            .billType(event.getBillType())
            .billingType(event.getBillingType())
            .businessType(event.getBusinessType())
            .description(event.getDescription())
            .remark(event.getRemark())
            .pricingMode(event.getPricingMode())
            .occurredAt(event.getOccurredAt())
            .retryCount(nextRetryCount)
            .build();
        String topic = billingProperties.getDlqTopic();
        if (!StringUtils.hasText(topic)) {
            log.error("DLQ topic 未配置，无法重新入队 eventId={}", event.getEventId());
            return;
        }
        kafkaTemplate.send(topic,
            nextEvent.getChargeUserId() != null ? nextEvent.getChargeUserId().toString() : null,
            nextEvent);
    }
}
