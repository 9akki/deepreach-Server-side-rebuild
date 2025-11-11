package com.deepreach.web.mq;

import com.deepreach.common.core.config.TranslateBillingProperties;
import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.dto.DeductResponse;
import com.deepreach.common.core.mq.event.TranslationChargeEvent;
import com.deepreach.common.core.service.UserDrBalanceService;
import com.deepreach.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 翻译扣费事件消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationChargeConsumer {

    private final UserDrBalanceService userDrBalanceService;
    private final TranslateBillingProperties billingProperties;

    @KafkaListener(
        topics = "${translate.billing.topic:translation-charge}",
        containerFactory = "deepreachKafkaListenerFactory"
    )
    public void onMessage(TranslationChargeEvent event) {
        if (event == null) {
            return;
        }
        try {
            handleEvent(event);
        } catch (Exception ex) {
            log.error("处理翻译扣费事件失败 eventId={} chargeUserId={} amount={}",
                event.getEventId(), event.getChargeUserId(), event.getAmount(), ex);
            throw ex;
        }
    }

    private void handleEvent(TranslationChargeEvent event) {
        if (event.getChargeUserId() == null || event.getAmount() == null || event.getAmount().signum() <= 0) {
            throw new ServiceException("翻译扣费事件数据异常，chargeUserId或amount缺失");
        }
        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(event.getChargeUserId());
        record.setOperatorId(event.getOperatorUserId());
        record.setBillType(event.getBillType() != null ? event.getBillType() : DrBillingRecord.BILL_TYPE_CONSUME);
        record.setBillingType(event.getBillingType() != null ? event.getBillingType() : DrBillingRecord.BILLING_TYPE_REALTIME);
        record.setBusinessType(StringUtils.hasText(event.getBusinessType()) ? event.getBusinessType() : DrBillingRecord.BUSINESS_TYPE_TOKEN);
        record.setDrAmount(event.getAmount());
        record.setDescription(resolveDescription(event));
        record.setRemark(StringUtils.hasText(event.getRemark()) ? event.getRemark() : "TranslationChargeAsync");

        DeductResponse response = userDrBalanceService.deductWithDailyAggregation(record, event.getOperatorUserId());
        if (response == null || !response.isSuccess()) {
            String message = response != null ? response.getMessage() : "扣费响应为空";
            throw new ServiceException("翻译扣费入账失败：" + message);
        }
        log.debug("翻译扣费事件消费成功 eventId={} chargeUserId={} amount={}",
            event.getEventId(), event.getChargeUserId(), event.getAmount());
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
}
