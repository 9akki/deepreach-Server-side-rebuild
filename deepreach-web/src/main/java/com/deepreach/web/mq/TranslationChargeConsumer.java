package com.deepreach.web.mq;

import com.deepreach.common.core.mq.event.TranslationChargeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 正常翻译扣费事件消费者（主Topic）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationChargeConsumer {

    private final TranslationChargeProcessor processor;

    @KafkaListener(
        topics = "${translate.billing.topic:translation-charge}",
        containerFactory = "deepreachKafkaListenerFactory"
    )
    public void onMessage(TranslationChargeEvent event) {
        if (event == null) {
            return;
        }
        try {
            processor.process(event, true);
        } catch (Exception ex) {
            log.error("处理翻译扣费事件失败 eventId={} chargeUserId={} amount={}",
                event.getEventId(), event.getChargeUserId(), event.getAmount(), ex);
            throw ex;
        }
    }
}
