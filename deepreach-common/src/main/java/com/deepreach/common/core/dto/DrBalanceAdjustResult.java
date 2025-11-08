package com.deepreach.common.core.dto;

import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.domain.entity.UserDrBalance;

import java.math.BigDecimal;

/**
 * 商家DR余额调账结果
 */
public class DrBalanceAdjustResult {

    private UserDrBalance balance;
    private DrBillingRecord billingRecord;
    private BigDecimal appliedAmount;

    public DrBalanceAdjustResult() {
    }

    public DrBalanceAdjustResult(UserDrBalance balance, DrBillingRecord billingRecord, BigDecimal appliedAmount) {
        this.balance = balance;
        this.billingRecord = billingRecord;
        this.appliedAmount = appliedAmount;
    }

    public UserDrBalance getBalance() {
        return balance;
    }

    public void setBalance(UserDrBalance balance) {
        this.balance = balance;
    }

    public DrBillingRecord getBillingRecord() {
        return billingRecord;
    }

    public void setBillingRecord(DrBillingRecord billingRecord) {
        this.billingRecord = billingRecord;
    }

    public BigDecimal getAppliedAmount() {
        return appliedAmount;
    }

    public void setAppliedAmount(BigDecimal appliedAmount) {
        this.appliedAmount = appliedAmount;
    }
}

