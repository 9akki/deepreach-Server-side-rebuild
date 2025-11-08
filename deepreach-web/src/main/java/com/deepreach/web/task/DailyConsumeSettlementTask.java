package com.deepreach.web.task;

import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.domain.entity.UserDrBalance;
import com.deepreach.common.core.service.UserDrBalanceService;
import com.deepreach.web.service.DrBillingRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 翻译/第三方服务每日消费清算任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyConsumeSettlementTask {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final UserDrBalanceService userDrBalanceService;
    private final DrBillingRecordService billingRecordService;

    /**
     * 每天北京时间00:00执行，将daily_consume汇总为一条账单记录并清零
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Shanghai")
    public void settleDailyConsume() {
        List<UserDrBalance> balances = userDrBalanceService.listUsersWithDailyConsume();
        if (balances == null || balances.isEmpty()) {
            return;
        }
        LocalDate settlementDate = LocalDate.now(SHANGHAI_ZONE).minusDays(1);
        for (UserDrBalance balance : balances) {
            BigDecimal amount = balance.getDailyConsume();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            try {
                createDailyRecord(balance, amount, settlementDate);
                boolean cleared = userDrBalanceService.subtractDailyConsume(balance.getUserId(), amount);
                if (!cleared) {
                    log.warn("日结扣费清零失败，userId={}, amount={}", balance.getUserId(), amount);
                }
            } catch (Exception ex) {
                log.error("日结扣费生成账单失败，userId={}, amount={}", balance.getUserId(), amount, ex);
            }
        }
    }

    private void createDailyRecord(UserDrBalance balance, BigDecimal amount, LocalDate settlementDate) {
        BigDecimal currentBalance = balance.getDrBalance() != null ? balance.getDrBalance() : BigDecimal.ZERO;
        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(balance.getUserId());
        record.setOperatorId(balance.getUserId());
        record.setBillType(2);
        record.setBillingType(1);
        record.setBusinessType(DrBillingRecord.BUSINESS_TYPE_DAILY_CONSUME);
        record.setDrAmount(amount);
        record.setBalanceBefore(currentBalance.add(amount));
        record.setBalanceAfter(currentBalance);
        record.setDescription(String.format("翻译/第三方日结扣费(%s)", settlementDate));
        record.setRemark("DailyConsume");
        record.setCreateBy("system");
        record.setConsumer("system");
        billingRecordService.createRecord(record);
    }
}
