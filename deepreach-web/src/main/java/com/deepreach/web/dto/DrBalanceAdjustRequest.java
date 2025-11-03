package com.deepreach.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 商家DR余额调账请求
 */
public class DrBalanceAdjustRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "调账金额不能为空")
    private BigDecimal amount;

    private String remark;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

