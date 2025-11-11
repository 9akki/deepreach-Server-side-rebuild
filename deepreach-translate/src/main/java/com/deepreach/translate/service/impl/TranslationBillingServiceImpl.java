package com.deepreach.translate.service.impl;

import com.deepreach.common.core.config.TranslateBillingProperties;
import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.domain.entity.DrPriceConfig;
import com.deepreach.common.core.mq.event.TranslationChargeEvent;
import com.deepreach.common.core.service.DrPriceConfigService;
import com.deepreach.common.core.support.ChargeAccountResolver;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.translate.mq.TranslationChargeProducer;
import com.deepreach.translate.service.TranslationBillingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class TranslationBillingServiceImpl implements TranslationBillingService {

    private static final Logger log = LoggerFactory.getLogger(TranslationBillingServiceImpl.class);
    private static final BigDecimal DEFAULT_UNIT_PRICE = new BigDecimal("0.0001");
    private static final int PRICE_SCALE = 6;

    private enum PricingMode {
        TOKEN,
        BY_TIMES,
        NONE
    }

    private final DrPriceConfigService drPriceConfigService;
    private final ChargeAccountResolver chargeAccountResolver;
    private final TranslateBillingProperties billingProperties;
    private final TranslationChargeProducer translationChargeProducer;

    public TranslationBillingServiceImpl(DrPriceConfigService drPriceConfigService,
                                         ChargeAccountResolver chargeAccountResolver,
                                         TranslateBillingProperties billingProperties,
                                         TranslationChargeProducer translationChargeProducer) {
        this.drPriceConfigService = drPriceConfigService;
        this.chargeAccountResolver = chargeAccountResolver;
        this.billingProperties = billingProperties;
        this.translationChargeProducer = translationChargeProducer;
    }

    @Override
    public BigDecimal deduct(Long userId, long totalTokens) {
        if (totalTokens <= 0) {
            return BigDecimal.ZERO;
        }
        if (!billingProperties.isEnabled()) {
            log.info("翻译扣费异步事件暂未启用，userId={}, tokens={}", userId, totalTokens);
            return BigDecimal.ZERO;
        }
        PricingMode pricingMode = resolvePricingMode();
        if (pricingMode == PricingMode.BY_TIMES) {
            log.info("Translation billing skipped due to BY_TIMES pricing mode, userId={}, tokens={}", userId, totalTokens);
            return BigDecimal.ZERO;
        }
        if (pricingMode == PricingMode.NONE) {
            log.warn("Translation billing skipped due to missing price config, userId={}, tokens={}", userId, totalTokens);
            return BigDecimal.ZERO;
        }
        BigDecimal unitPrice = resolveUnitPrice();
        BigDecimal amount = unitPrice
            .multiply(BigDecimal.valueOf(totalTokens))
            .setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (userId == null || userId <= 0) {
            log.warn("Skip translation billing, invalid userId={}, tokens={}, amount={}", userId, totalTokens, amount);
            return BigDecimal.ZERO;
        }
        ChargeAccountResolver.ChargeAccount account = chargeAccountResolver.resolve(userId);
        if (account == null) {
            throw new ServiceException("翻译扣费失败：无法解析扣费账号");
        }
        TranslationChargeEvent event = TranslationChargeEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .requestUserId(userId)
            .chargeUserId(account.getChargeUserId())
            .operatorUserId(account.getOperatorUserId())
            .totalTokens(totalTokens)
            .amount(amount)
            .billType(DrBillingRecord.BILL_TYPE_CONSUME)
            .billingType(DrBillingRecord.BILLING_TYPE_REALTIME)
            .businessType(DrBillingRecord.BUSINESS_TYPE_TOKEN)
            .description(resolveDescription())
            .remark("TranslationChargeAsync")
            .pricingMode(pricingMode.name())
            .occurredAt(Instant.now())
            .build();
        translationChargeProducer.publish(event);
        log.info("翻译扣费事件已入队，requestUserId={}, chargeUserId={}, tokens={}, amount={}",
            userId, account.getChargeUserId(), totalTokens, amount);
        return amount;
    }

    @Override
    public BigDecimal resolveUnitPrice() {
        DrPriceConfig config = drPriceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_TOKEN);
        if (config == null || config.getDrPrice() == null) {
            log.warn("Translation price config missing (TOKEN), fallback to default {}", DEFAULT_UNIT_PRICE);
            return DEFAULT_UNIT_PRICE.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }
        return config.getDrPrice().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private PricingMode resolvePricingMode() {
        DrPriceConfig tokenConfig = drPriceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_TOKEN);
        if (tokenConfig != null && tokenConfig.isActive()) {
            return PricingMode.TOKEN;
        }
        DrPriceConfig byTimesConfig = drPriceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_BY_TIMES);
        if (byTimesConfig != null && byTimesConfig.isActive()) {
            return PricingMode.BY_TIMES;
        }
        return PricingMode.NONE;
    }

    private String resolveDescription() {
        if (StringUtils.hasText(billingProperties.getDescription())) {
            return billingProperties.getDescription();
        }
        return "翻译扣费";
    }
}
