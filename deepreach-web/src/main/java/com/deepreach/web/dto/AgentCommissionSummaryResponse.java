package com.deepreach.web.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 代理佣金汇总响应
 */
@Data
public class AgentCommissionSummaryResponse {
    private Long agentUserId;
    private BigDecimal totalCommission = BigDecimal.ZERO;
    private BigDecimal settlementCommission = BigDecimal.ZERO;
    private BigDecimal availableCommission = BigDecimal.ZERO;
}

