package com.deepreach.web.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentCommissionRecordDTO {
    private Long recordId;
    private Long agentUserId;
    private Long agentDeptId;
    private Long buyerUserId;
    private String buyerUsername;
    private Long triggerBillingId;
    private String billingNo;
    private BigDecimal triggerAmount;
    private BigDecimal commissionAmount;
    private BigDecimal commissionRate;
    private Integer hierarchyLevel;
    private String businessType;
    private String status;
    private String description;
    private LocalDateTime createTime;
}
