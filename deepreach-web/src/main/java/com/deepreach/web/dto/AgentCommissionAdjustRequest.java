package com.deepreach.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AgentCommissionAdjustRequest {

    @NotNull(message = "代理用户ID不能为空")
    private Long agentUserId;

    @NotNull(message = "调整金额不能为空")
    private BigDecimal amount;

    private String remark;

    public Long getAgentUserId() {
        return agentUserId;
    }

    public void setAgentUserId(Long agentUserId) {
        this.agentUserId = agentUserId;
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
