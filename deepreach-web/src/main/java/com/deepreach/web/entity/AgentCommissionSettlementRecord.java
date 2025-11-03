package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 代理佣金结算流水实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentCommissionSettlementRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long recordId;

    private Long settlementId;

    private Long agentUserId;

    private BigDecimal changeAmount;

    private String direction;

    private String status;

    private Long operatorId;

    private String description;

    private String extraData;

    public static final String DIRECTION_CREDIT = "+";
    public static final String DIRECTION_DEBIT = "-";
    public static final String STATUS_PENDING = "0";
    public static final String STATUS_SUCCESS = "1";
    public static final String STATUS_FAILED = "2";
}
