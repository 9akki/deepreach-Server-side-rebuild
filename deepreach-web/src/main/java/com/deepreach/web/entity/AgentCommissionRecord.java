package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 代理佣金记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentCommissionRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long recordId;

    private Long agentUserId;

    private Long agentDeptId;

    private Long buyerUserId;

    private Long buyerDeptId;

    private Long triggerBillingId;

    private BigDecimal triggerAmount;

    private BigDecimal commissionAmount;

    private BigDecimal commissionRate;

    private Integer hierarchyLevel;

    private String direction;

    private String businessType;

    private String status;

    private Long operatorId;

    private String description;

    private String extraData;

    public static final String DIRECTION_CREDIT = "+";
    public static final String DIRECTION_DEBIT = "-";
    public static final String STATUS_SUCCESS = "0";
    public static final String STATUS_FAILED = "1";
    public static final String BUSINESS_TYPE_RECHARGE = "RECHARGE_COMMISSION";
    public static final String BUSINESS_TYPE_SETTLEMENT_FREEZE = "SETTLEMENT_FREEZE";
    public static final String BUSINESS_TYPE_SETTLEMENT_ROLLBACK = "SETTLEMENT_ROLLBACK";
    public static final String BUSINESS_TYPE_MANUAL_ADJUST = "MANUAL_ADJUST";
}
