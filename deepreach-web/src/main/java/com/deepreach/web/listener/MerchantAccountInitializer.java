package com.deepreach.web.listener;

import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.event.MerchantCreatedEvent;
import com.deepreach.web.entity.DrBillingRecord;
import com.deepreach.web.entity.DrPriceConfig;
import com.deepreach.web.entity.UserDrBalance;
import com.deepreach.web.service.DrPriceConfigService;
import com.deepreach.web.service.UserDrBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 监听商家创建事件，初始化其DR账户并发放首次赠送。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantAccountInitializer {

    private static final String FIRST_GIFT_BUSINESS_TYPE = "FIRSTGIFT";

    private final UserDrBalanceService userDrBalanceService;
    private final DrPriceConfigService drPriceConfigService;

    @EventListener
    public void onMerchantCreated(MerchantCreatedEvent event) {
        SysUser merchant = event.getMerchant();
        if (merchant == null || merchant.getUserId() == null) {
            return;
        }

        Long merchantId = merchant.getUserId();

        try {
            UserDrBalance balance = userDrBalanceService.createBalanceAccount(merchantId);
            if (balance == null) {
                log.warn("创建商家余额账户失败：userId={}", merchantId);
                return;
            }

            DrPriceConfig giftConfig = drPriceConfigService.selectDrPriceConfigByBusinessType(FIRST_GIFT_BUSINESS_TYPE);
            if (giftConfig == null || giftConfig.getDrPrice() == null) {
                log.info("未配置首次赠送价格，跳过初始充值：businessType={}", FIRST_GIFT_BUSINESS_TYPE);
                return;
            }

            BigDecimal giftAmount = giftConfig.getDrPrice();
            if (giftAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("首次赠送金额为零或负数，跳过初始充值：amount={}", giftAmount);
                return;
            }

            DrBillingRecord rechargeRecord = new DrBillingRecord();
            rechargeRecord.setUserId(merchantId);
            rechargeRecord.setDrAmount(giftAmount);
            rechargeRecord.setBillType(1);
            rechargeRecord.setBillingType(giftConfig.getBillingType() != null ? giftConfig.getBillingType() : 1);
            rechargeRecord.setBusinessType(DrBillingRecord.BUSINESS_TYPE_RECHARGE);
            rechargeRecord.setRemark("商家首次注册赠送");
            rechargeRecord.setDescription("商家首次注册赠送");

            userDrBalanceService.recharge(rechargeRecord, merchantId);
            log.info("商家首次赠送DR积分成功：userId={}, amount={}", merchantId, giftAmount);
        } catch (Exception ex) {
            log.warn("商家首次赠送DR积分失败：userId={}", merchantId, ex);
        }
    }
}
