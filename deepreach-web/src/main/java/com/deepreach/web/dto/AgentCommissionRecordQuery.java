package com.deepreach.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AgentCommissionRecordQuery {
    private String startTime;
    private String endTime;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}
