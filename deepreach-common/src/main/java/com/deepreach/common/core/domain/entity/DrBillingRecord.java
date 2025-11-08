package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DR积分账单记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DrBillingRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long billId;
    private String billNo;
    private Long userId;
    private Long operatorId;
    private Integer billType;
    private Integer billingType;
    private String businessType;
    private Long businessId;
    private String username;
    private BigDecimal level1Commission;
    private BigDecimal level2Commission;
    private BigDecimal level3Commission;
    @JsonIgnore
    private Map<String, BigDecimal> agentLevelCommission;
    private BigDecimal drAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private String extraData;
    private String consumer;
    private Integer status;

    public static final String BUSINESS_TYPE_RECHARGE = "RECHARGE";
    public static final String BUSINESS_TYPE_INSTANCE_PRE_DEDUCT = "INSTANCE_PRE_DEDUCT";
    public static final String BUSINESS_TYPE_INSTANCE_MARKETING = "INSTANCE_MARKETING";
    public static final String BUSINESS_TYPE_INSTANCE_PROSPECTING = "INSTANCE_PROSPECTING";
    public static final String BUSINESS_TYPE_SMS = "SMS";
    public static final String BUSINESS_TYPE_TOKEN = "TOKEN";
    public static final String BUSINESS_TYPE_AI_CHARACTER = "AI_CHARACTER";
    public static final String BUSINESS_TYPE_MANUAL_ADJUST = "MANUAL_ADJUST";
    public static final String BUSINESS_TYPE_DAILY_CONSUME = "DAILY_CONSUME";
    public static final String BUSINESS_TYPE_BY_TIMES = "BY_TIMES";

    public Map<String, BigDecimal> getAgentLevelCommission() {
        if (agentLevelCommission == null) {
            agentLevelCommission = new HashMap<>(3);
        }
        return agentLevelCommission;
    }

    public boolean isRecharge() {
        return Integer.valueOf(1).equals(this.billType);
    }

    public boolean isConsume() {
        return Integer.valueOf(2).equals(this.billType);
    }

    public boolean isRefund() {
        return Integer.valueOf(3).equals(this.billType);
    }

    public String getBillTypeDisplay() {
        if (this.billType == null) {
            return "未知";
        }

        switch (this.billType) {
            case 1:
                return "充值";
            case 2:
                return "消费";
            case 3:
                return "退款";
            default:
                return "未知";
        }
    }

    public String getBillingTypeDisplay() {
        if (this.billingType == null) {
            return "未知";
        }

        switch (this.billingType) {
            case 1:
                return "秒结秒扣";
            case 2:
                return "日结日扣";
            default:
                return "未知";
        }
    }

    public String getBusinessTypeDisplay() {
        if (this.businessType == null) {
            return "未知";
        }

        switch (this.businessType) {
            case BUSINESS_TYPE_RECHARGE:
                return "充值";
            case BUSINESS_TYPE_INSTANCE_PRE_DEDUCT:
                return "营销实例预扣费";
            case BUSINESS_TYPE_INSTANCE_MARKETING:
                return "营销实例";
            case BUSINESS_TYPE_INSTANCE_PROSPECTING:
                return "拓客实例";
            case BUSINESS_TYPE_AI_CHARACTER:
                return "AI人设创建";
            case BUSINESS_TYPE_SMS:
                return "短信服务";
            case BUSINESS_TYPE_TOKEN:
                return "AI服务";
            case BUSINESS_TYPE_BY_TIMES:
                return "按次扣费";
            case BUSINESS_TYPE_MANUAL_ADJUST:
                return "手动调账";
            case BUSINESS_TYPE_DAILY_CONSUME:
                return "日结消费";
            default:
                return "未知";
        }
    }

    public static DrBillingRecord createRechargeRecord(Long userId, Long operatorId,
                                                       BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter) {
        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(userId);
        record.setOperatorId(operatorId);
        record.setBillType(1);
        record.setBillingType(1);
        record.setBusinessType(BUSINESS_TYPE_RECHARGE);
        record.setDrAmount(amount);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setDescription("DR积分充值");
        record.setStatus(1);
        return record;
    }

    public static DrBillingRecord createConsumeRecord(Long userId, Long operatorId, String businessType,
                                                      Long businessId, BigDecimal amount, Integer billingType,
                                                      BigDecimal balanceBefore, BigDecimal balanceAfter, String description) {
        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(userId);
        record.setOperatorId(operatorId);
        record.setBillType(2);
        record.setBillingType(billingType);
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setDrAmount(amount);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setDescription(description);
        record.setStatus(1);
        return record;
    }

    public static DrBillingRecord createPreDeductRecord(Long userId, Long operatorId,
                                                        BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter,
                                                        BigDecimal preDeductedBefore, BigDecimal preDeductedAfter) {
        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(userId);
        record.setOperatorId(operatorId);
        record.setBillType(2);
        record.setBillingType(1);
        record.setBusinessType(BUSINESS_TYPE_INSTANCE_PRE_DEDUCT);
        record.setDrAmount(amount);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setDescription("营销实例创建预扣费");

        String extraData = String.format("{\"preDeductedBefore\":%s,\"preDeductedAfter\":%s}",
            preDeductedBefore, preDeductedAfter);
        record.setExtraData(extraData);
        record.setStatus(1);
        return record;
    }
}
