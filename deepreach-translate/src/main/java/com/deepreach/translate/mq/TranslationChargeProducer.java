package com.deepreach.translate.mq;

import com.deepreach.common.core.config.TranslateBillingProperties;
import com.deepreach.common.core.mq.event.TranslationChargeEvent;
import java.util.Objects;
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
        sendInternal(kafkaTemplate, topic, key, event);
    }

    private void sendInternal(KafkaOperations<String, Object> operations, String topic, String key, TranslationChargeEvent event) {
        operations.send(topic, key, event)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("翻译扣费事件发送失败 topic={} key={} amount={}", topic, key, event.getAmount(), throwable);
                } else {
                    log.debug("Published translation charge event to topic={}, key={}, offset={}",
                        topic, key, result != null && result.getRecordMetadata() != null
                            ? result.getRecordMetadata().offset() : null);
                }
            });
    }
}
