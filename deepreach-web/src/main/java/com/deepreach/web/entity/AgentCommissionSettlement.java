package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import com.deepreach.common.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    private String network;

    private String address;

    @JsonIgnore
    private String agentRoleKeys;

    private Set<String> agentRoles = Collections.emptySet();

    public static final String STATUS_PENDING = "0";
    public static final String STATUS_APPROVED = "1";
    public static final String STATUS_REJECTED = "2";
    public static final String STATUS_CANCELLED = "3";

    public void setAgentRoleKeys(String agentRoleKeys) {
        this.agentRoleKeys = agentRoleKeys;
        if (StringUtils.isEmpty(agentRoleKeys)) {
            this.agentRoles = Collections.emptySet();
        } else {
            this.agentRoles = Arrays.stream(agentRoleKeys.split(","))
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}
