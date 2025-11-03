package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 代理佣金账户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentCommissionAccount extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long accountId;

    private Long agentUserId;

    private BigDecimal totalCommission;

    private BigDecimal availableCommission;

    private BigDecimal frozenCommission;

    private BigDecimal pendingSettlementCommission;

    private BigDecimal settledCommission;

    private Integer version;

    private String status;

    private String remark;

    public static AgentCommissionAccount createForAgent(Long agentUserId) {
        AgentCommissionAccount account = new AgentCommissionAccount();
        account.setAgentUserId(agentUserId);
        account.setTotalCommission(BigDecimal.ZERO);
        account.setAvailableCommission(BigDecimal.ZERO);
        account.setFrozenCommission(BigDecimal.ZERO);
        account.setPendingSettlementCommission(BigDecimal.ZERO);
        account.setSettledCommission(BigDecimal.ZERO);
        account.setVersion(0);
        account.setStatus("0");
        return account;
    }

    public boolean isNormal() {
        return "0".equals(this.status);
    }
}
