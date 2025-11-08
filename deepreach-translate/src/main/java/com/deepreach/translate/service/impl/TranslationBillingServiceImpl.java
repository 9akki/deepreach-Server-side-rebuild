package com.deepreach.translate.service.impl;

import com.deepreach.common.core.domain.entity.DrPriceConfig;
import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.dto.DeductResponse;
import com.deepreach.common.core.service.DrPriceConfigService;
import com.deepreach.common.core.service.UserDrBalanceService;
import com.deepreach.common.core.support.ChargeAccountResolver;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.translate.service.TranslationBillingService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TranslationBillingServiceImpl implements TranslationBillingService {

    private static final Logger log = LoggerFactory.getLogger(TranslationBillingServiceImpl.class);
    private static final BigDecimal DEFAULT_UNIT_PRICE = new BigDecimal("0.0001");

    private final DrPriceConfigService drPriceConfigService;
    private final UserDrBalanceService userDrBalanceService;
    private final ChargeAccountResolver chargeAccountResolver;

    public TranslationBillingServiceImpl(DrPriceConfigService drPriceConfigService,
                                         UserDrBalanceService userDrBalanceService,
                                         ChargeAccountResolver chargeAccountResolver) {
        this.drPriceConfigService = drPriceConfigService;
        this.userDrBalanceService = userDrBalanceService;
        this.chargeAccountResolver = chargeAccountResolver;
    }

    @Override
    public BigDecimal deduct(Long userId, long totalTokens) {
        if (totalTokens <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal unitPrice = resolveUnitPrice();
        BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(totalTokens));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (userId == null || userId <= 0) {
            log.warn("Skip translation billing, invalid userId={}, tokens={}, amount={}", userId, totalTokens, amount);
            return BigDecimal.ZERO;
        }
        ChargeAccountResolver.ChargeAccount account = chargeAccountResolver.resolve(userId);
        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(account.getChargeUserId());
        record.setOperatorId(account.getOperatorUserId());
        record.setBillType(2);
        record.setBillingType(1);
        record.setBusinessType(DrBillingRecord.BUSINESS_TYPE_TOKEN);
        record.setDrAmount(amount);
        record.setDescription("翻译扣费");
        record.setRemark("Translate");

        DeductResponse response = userDrBalanceService.deductWithDailyAggregation(record, account.getOperatorUserId());
        if (response == null || !response.isSuccess()) {
            String message = response != null ? response.getMessage() : "扣费响应为空";
            throw new ServiceException("翻译扣费失败: " + message);
        }
        log.info("Translation tokens deducted, requestUserId={} chargeAccount={} tokens={} amount={} billNo={}",
            userId,
            account.getChargeUserId(),
            totalTokens,
            amount,
            response.getBillingRecord() != null ? response.getBillingRecord().getBillNo() : null);
        return amount;
    }

    @Override
    public BigDecimal resolveUnitPrice() {
        DrPriceConfig config = drPriceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_TRANSLATE);
        if (config == null || config.getDrPrice() == null) {
            log.warn("Translation price config missing, fallback to default {}", DEFAULT_UNIT_PRICE);
            return DEFAULT_UNIT_PRICE;
        }
        return config.getDrPrice();
    }
}
