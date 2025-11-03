package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * DR价格配置实体
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DrPriceConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 价格ID
     */
    private Long priceId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务名称
     */
    private String businessName;

    /**
     * 计价单位
     */
    private String priceUnit;

    /**
     * DR积分单价
     */
    private BigDecimal drPrice;

    /**
     * 结算类型（1秒结秒扣 2日结日扣）
     */
    private Integer billingType;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    // ==================== 业务判断方法 ====================

    /**
     * 判断是否为正常状态
     *
     * @return true如果正常，false否则
     */
    public boolean isActive() {
        return "0".equals(status);
    }

    /**
     * 判断是否为停用状态
     *
     * @return true如果停用，false否则
     */
    public boolean isInactive() {
        return "1".equals(status);
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
     * 获取状态显示文本
     *
     * @return 状态显示文本
     */
    public String getStatusDisplay() {
        if (isActive()) {
            return "正常";
        } else if (isInactive()) {
            return "停用";
        } else {
            return "未知";
        }
    }

    /**
     * 创建营销实例预扣费价格配置
     *
     * @return 价格配置对象
     */
    public static DrPriceConfig createInstancePreDeductConfig() {
        DrPriceConfig config = new DrPriceConfig();
        config.setBusinessType(BUSINESS_TYPE_INSTANCE_PRE_DEDUCT);
        config.setBusinessName("营销实例预扣费");
        config.setPriceUnit("DR/个");
        config.setDrPrice(new BigDecimal("100.0000"));
        config.setBillingType(1);
        config.setStatus("0");
        return config;
    }

    /**
     * 创建营销实例价格配置
     *
     * @return 价格配置对象
     */
    public static DrPriceConfig createMarketingInstanceConfig() {
        DrPriceConfig config = new DrPriceConfig();
        config.setBusinessType(BUSINESS_TYPE_INSTANCE_MARKETING);
        config.setBusinessName("营销实例");
        config.setPriceUnit("DR/天");
        config.setDrPrice(new BigDecimal("6.0000"));
        config.setBillingType(2);
        config.setStatus("0");
        return config;
    }

    /**
     * 创建拓客实例价格配置
     *
     * @return 价格配置对象
     */
    public static DrPriceConfig createProspectingInstanceConfig() {
        DrPriceConfig config = new DrPriceConfig();
        config.setBusinessType(BUSINESS_TYPE_INSTANCE_PROSPECTING);
        config.setBusinessName("拓客实例");
        config.setPriceUnit("DR/天");
        config.setDrPrice(new BigDecimal("1.0000"));
        config.setBillingType(2);
        config.setStatus("0");
        return config;
    }

    /**
     * 创建短信服务价格配置
     *
     * @return 价格配置对象
     */
    public static DrPriceConfig createSmsConfig() {
        DrPriceConfig config = new DrPriceConfig();
        config.setBusinessType(BUSINESS_TYPE_SMS);
        config.setBusinessName("短信服务");
        config.setPriceUnit("DR/条");
        config.setDrPrice(new BigDecimal("0.0500"));
        config.setBillingType(1);
        config.setStatus("0");
        return config;
    }

    /**
     * 创建AI服务Token价格配置
     *
     * @return 价格配置对象
     */
    public static DrPriceConfig createTokenConfig() {
        DrPriceConfig config = new DrPriceConfig();
        config.setBusinessType(BUSINESS_TYPE_TOKEN);
        config.setBusinessName("AI服务");
        config.setPriceUnit("DR/token");
        config.setDrPrice(new BigDecimal("0.0001"));
        config.setBillingType(1);
        config.setStatus("0");
        return config;
    }

    // ==================== 业务类型常量 ====================

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
     * 业务类型：一级代理佣金比例
     */
    public static final String BUSINESS_TYPE_AGENT_LEVEL1_COMMISSION = "AGENT_LEVEL1_COMMISSION";

    /**
     * 业务类型：二级代理佣金比例
     */
    public static final String BUSINESS_TYPE_AGENT_LEVEL2_COMMISSION = "AGENT_LEVEL2_COMMISSION";

    /**
     * 业务类型：三级代理佣金比例
     */
    public static final String BUSINESS_TYPE_AGENT_LEVEL3_COMMISSION = "AGENT_LEVEL3_COMMISSION";

    // ==================== 结算类型常量 ====================

    /**
     * 结算类型：秒结秒扣
     */
    public static final Integer BILLING_TYPE_INSTANT = 1;

    /**
     * 结算类型：日结日扣
     */
    public static final Integer BILLING_TYPE_DAILY = 2;
}
