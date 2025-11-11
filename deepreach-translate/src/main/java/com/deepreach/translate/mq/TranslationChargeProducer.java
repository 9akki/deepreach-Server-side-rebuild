package com.deepreach.translate.mq;

import com.deepreach.common.core.config.TranslateBillingProperties;
import com.deepreach.common.core.mq.event.TranslationChargeEvent;
import com.deepreach.common.exception.ServiceException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 翻译扣费事件Producer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationChargeProducer {

    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TranslateBillingProperties billingProperties;

    public void publish(TranslationChargeEvent event) {
        if (!billingProperties.isEnabled()) {
            log.debug("Translate billing disabled, skip event: {}", event);
            return;
        }
        if (event == null || event.getAmount() == null || event.getAmount().signum() <= 0) {
            log.debug("Skip publishing empty translation charge event");
            return;
        }
        String topic = billingProperties.getTopic();
        String key = Objects.requireNonNullElse(event.getEventId(), event.getChargeUserId() + "");
        if (kafkaTemplate.isTransactional()) {
            kafkaTemplate.executeInTransaction(ops -> {
                sendInternal(ops, topic, key, event);
                return null;
            });
            return;
        }
        sendInternal(kafkaTemplate, topic, key, event);
    }

    private void sendInternal(KafkaOperations<String, Object> operations, String topic, String key, TranslationChargeEvent event) {
        try {
            operations.send(topic, key, event).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.debug("Published translation charge event to topic={}, key={}, amount={}", topic, key, event.getAmount());
        } catch (Exception ex) {
            throw new ServiceException("翻译扣费事件发送失败，请稍后重试", ex);
        }
    }
}
