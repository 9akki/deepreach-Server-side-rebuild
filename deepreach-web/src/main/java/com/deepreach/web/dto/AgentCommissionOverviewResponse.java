package com.deepreach.web.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AgentCommissionOverviewResponse {
    private BigDecimal totalSettlementCommission = BigDecimal.ZERO;
    private BigDecimal earnedCommissionInRange = BigDecimal.ZERO;
    private int agentCount = 0;
    private List<AgentCommissionAccountDTO> agents = new ArrayList<>();
}
