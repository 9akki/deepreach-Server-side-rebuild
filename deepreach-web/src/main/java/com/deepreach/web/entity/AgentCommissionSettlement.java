package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 代理佣金结算申请实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentCommissionSettlement extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long settlementId;

    private Long agentUserId;

    private String agentUsername;

    private BigDecimal requestAmount;

    private BigDecimal approvedAmount;

    private String status;

    private Long approvalUserId;

    private java.time.LocalDateTime approvalTime;

    private String remark;

    private String extraData;

    public static final String STATUS_PENDING = "0";
    public static final String STATUS_APPROVED = "1";
    public static final String STATUS_REJECTED = "2";
    public static final String STATUS_CANCELLED = "3";
}
