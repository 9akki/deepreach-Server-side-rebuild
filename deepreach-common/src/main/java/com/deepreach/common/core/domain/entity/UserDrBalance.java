package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户DR积分余额实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDrBalance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long balanceId;
    private Long userId;
    private BigDecimal drBalance;
    private BigDecimal preDeductedBalance;
    private BigDecimal totalRecharge;
    private BigDecimal totalConsume;
    private BigDecimal totalRefund;
    private BigDecimal frozenAmount;
    private Integer version;
    private String status;
    private Integer aiCharacterFreeTimes;

    public boolean isBalanceSufficient(BigDecimal amount) {
        if (drBalance == null || amount == null) {
            return false;
        }
        return drBalance.compareTo(amount) >= 0;
    }

    public BigDecimal getAvailableBalance() {
        if (drBalance == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal frozen = frozenAmount != null ? frozenAmount : BigDecimal.ZERO;
        return drBalance.subtract(frozen);
    }

    public BigDecimal getPreDeductedBalance() {
        return preDeductedBalance != null ? preDeductedBalance : BigDecimal.ZERO;
    }

    public BigDecimal getTotalAvailableBalance() {
        BigDecimal available = getAvailableBalance();
        BigDecimal preDeducted = getPreDeductedBalance();
        return available.add(preDeducted);
    }

    public boolean isBaseBalanceSufficient(BigDecimal amount) {
        return getAvailableBalance().compareTo(amount) >= 0;
    }

    public boolean isPreDeductedBalanceSufficient(BigDecimal amount) {
        return getPreDeductedBalance().compareTo(amount) >= 0;
    }

    public boolean isTotalBalanceSufficient(BigDecimal amount) {
        return getTotalAvailableBalance().compareTo(amount) >= 0;
    }

    public boolean isNormal() {
        return "0".equals(status);
    }

    public boolean isFrozen() {
        return "1".equals(status);
    }

    public boolean isCancelled() {
        return "2".equals(status);
    }

    public static UserDrBalance createForUser(Long userId) {
        UserDrBalance balance = new UserDrBalance();
        balance.setUserId(userId);
        balance.setDrBalance(BigDecimal.ZERO);
        balance.setPreDeductedBalance(BigDecimal.ZERO);
        balance.setTotalRecharge(BigDecimal.ZERO);
        balance.setTotalConsume(BigDecimal.ZERO);
        balance.setTotalRefund(BigDecimal.ZERO);
        balance.setFrozenAmount(BigDecimal.ZERO);
        balance.setVersion(0);
        balance.setStatus("0");
        balance.setAiCharacterFreeTimes(1);
        return balance;
    }
}
