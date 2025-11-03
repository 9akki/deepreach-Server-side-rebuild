package com.deepreach.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AgentCommissionAccountDTO {
    private Long agentUserId;
    private String username;
    private String nickname;
    private Long deptId;
    private String deptName;
    private Integer deptLevel;
    private BigDecimal totalCommission = BigDecimal.ZERO;
    private BigDecimal availableCommission = BigDecimal.ZERO;
    private BigDecimal frozenCommission = BigDecimal.ZERO;
    private BigDecimal pendingSettlementCommission = BigDecimal.ZERO;
    private BigDecimal settlementCommission = BigDecimal.ZERO;
    private BigDecimal earnedCommissionInRange = BigDecimal.ZERO;
}
