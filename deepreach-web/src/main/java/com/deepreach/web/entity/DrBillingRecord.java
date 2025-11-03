package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * DR积分账单记录实体
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DrBillingRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 账单ID
     */
    private Long billId;

    /**
     * 账单编号
     */
    private String billNo;

    /**
     * 用户ID（买家总账户）
     */
    private Long userId;

    /**
     * 操作者ID（管理员）
     */
    private Long operatorId;

    /**
     * 账单类型（1充值 2消费 3退款）
     */
    private Integer billType;

    /**
     * 结算类型（1秒结秒扣 2日结日扣）
     */
    private Integer billingType;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务关联ID
     */
    private Long businessId;

    /**
     * 买家用户名（查询辅助字段）
     */
    private String username;

    /**
     * 一级代理佣金
     */
    private BigDecimal level1Commission;

    /**
     * 二级代理佣金
     */
    private BigDecimal level2Commission;

    /**
     * 三级代理佣金
     */
    private BigDecimal level3Commission;

    /**
     * 各级代理佣金汇总
     */
    private Map<String, BigDecimal> agentLevelCommission;

    /**
     * DR积分数量
     */
    private BigDecimal drAmount;

    /**
     * 操作前余额
     */
    private BigDecimal balanceBefore;

    /**
     * 操作后余额
     */
    private BigDecimal balanceAfter;

    /**
     * 描述
     */
    private String description;

    /**
     * 扩展数据（JSON格式）
     */
    private String extraData;

    /**
     * 发起人用户名
     */
    private String consumer;

    /**
     * 状态（1成功 0失败）
     */
    private Integer status;

    // ==================== 业务类型常量 ====================

    /**
     * 业务类型：充值
     */
    public static final String BUSINESS_TYPE_RECHARGE = "RECHARGE";

    /**
     * 业务类型：营销实例预扣费
     */
    public static final String BUSINESS_TYPE_INSTANCE_PRE_DEDUCT = "INSTANCE_PRE_DEDUCT";

    /**
     * 业务类型：营销实例
     */
    public static final String BUSINESS_TYPE_INSTANCE_MARKETING = "INSTANCE_MARKETING";

    /**
     * 业务类型：拓客实例
     */
    public static final String BUSINESS_TYPE_INSTANCE_PROSPECTING = "INSTANCE_PROSPECTING";

    /**
     * 业务类型：短信
     */
    public static final String BUSINESS_TYPE_SMS = "SMS";

    /**
     * 业务类型：AI服务Token
     */
    public static final String BUSINESS_TYPE_TOKEN = "TOKEN";

    /**
     * 业务类型：手动调账
     */
    public static final String BUSINESS_TYPE_MANUAL_ADJUST = "MANUAL_ADJUST";

    // ==================== 业务判断方法 ====================

    /**
     * 判断是否为充值账单
     *
     * @return true如果是充值，false否则
     */
    public boolean isRecharge() {
        return Integer.valueOf(1).equals(this.billType);
    }

    /**
     * 判断是否为消费账单
     *
     * @return true如果是消费，false否则
     */
    public boolean isConsume() {
        return Integer.valueOf(2).equals(this.billType);
    }

    /**
     * 判断是否为退款账单
     *
     * @return true如果是退款，false否则
     */
    public boolean isRefund() {
        return Integer.valueOf(3).equals(this.billType);
    }

    /**
     * 判断是否为秒结秒扣
     *
     * @return true如果是秒结秒扣，false否则
     */
    public boolean isInstantBilling() {
        return Integer.valueOf(1).equals(this.billingType);
    }

    /**
     * 判断是否为日结日扣
     *
     * @return true如果是日结日扣，false否则
     */
    public boolean isDailyBilling() {
        return Integer.valueOf(2).equals(this.billingType);
    }

    /**
     * 判断是否为成功状态
     *
     * @return true如果成功，false否则
     */
    public boolean isSuccess() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 判断是否为失败状态
     *
     * @return true如果失败，false否则
     */
    public boolean isFailed() {
        return Integer.valueOf(0).equals(this.status);
    }

    /**
     * 获取账单类型显示文本
     *
     * @return 账单类型显示文本
     */
    public String getBillTypeDisplay() {
        if (isRecharge()) {
            return "充值";
        } else if (isConsume()) {
            return "消费";
        } else if (isRefund()) {
            return "退款";
        } else {
            return "未知";
        }
    }

    /**
     * 获取结算类型显示文本
     *
     * @return 结算类型显示文本
     */
    public String getBillingTypeDisplay() {
        if (isInstantBilling()) {
            return "秒结秒扣";
        } else if (isDailyBilling()) {
            return "日结日扣";
        } else {
            return "未知";
        }
    }

    /**
     * 获取业务类型显示文本
     *
     * @return 业务类型显示文本
     */
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
            case BUSINESS_TYPE_SMS:
                return "短信服务";
            case BUSINESS_TYPE_TOKEN:
                return "AI服务";
            case BUSINESS_TYPE_MANUAL_ADJUST:
                return "手动调账";
            default:
                return "未知";
        }
    }

    /**
     * 创建充值账单记录
     *
     * @param userId 用户ID
     * @param operatorId 操作者ID
     * @param amount DR积分数量
     * @param balanceBefore 操作前余额
     * @param balanceAfter 操作后余额
     * @return 账单记录
     */
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

    /**
     * 创建消费账单记录
     *
     * @param userId 用户ID
     * @param operatorId 操作者ID
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param amount DR积分数量
     * @param billingType 结算类型
     * @param balanceBefore 操作前余额
     * @param balanceAfter 操作后余额
     * @param description 描述
     * @return 账单记录
     */
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

    /**
     * 创建营销实例预扣费账单记录
     *
     * @param userId 用户ID
     * @param operatorId 操作者ID
     * @param amount DR积分数量
     * @param balanceBefore 操作前基本余额
     * @param balanceAfter 操作后基本余额
     * @param preDeductedBefore 操作前预扣费余额
     * @param preDeductedAfter 操作后预扣费余额
     * @return 账单记录
     */
    public static DrBillingRecord createPreDeductRecord(Long userId, Long operatorId,
                                                       BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter,
                                                       BigDecimal preDeductedBefore, BigDecimal preDeductedAfter) {
        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(userId);
        record.setOperatorId(operatorId);
        record.setBillType(2); // 消费类型
        record.setBillingType(1); // 秒结秒扣
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
