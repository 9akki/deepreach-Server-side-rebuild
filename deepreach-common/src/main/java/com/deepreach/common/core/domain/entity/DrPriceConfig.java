package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DR价格配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DrPriceConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long priceId;
    private String businessType;
    private String businessName;
    private String priceUnit;
    private BigDecimal drPrice;
    private Integer billingType;
    private String status;

    public boolean isActive() {
        return "0".equals(status);
    }

    public boolean isInactive() {
        return "1".equals(status);
    }

    public boolean isInstantBilling() {
        return Integer.valueOf(1).equals(this.billingType);
    }

    public boolean isDailyBilling() {
        return Integer.valueOf(2).equals(this.billingType);
    }

    public String getBillingTypeDisplay() {
        if (isInstantBilling()) {
            return "秒结秒扣";
        } else if (isDailyBilling()) {
            return "日结日扣";
        }
        return "未知";
    }

    public String getStatusDisplay() {
        if (isActive()) {
            return "正常";
        } else if (isInactive()) {
            return "停用";
        }
        return "未知";
    }

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

    public static DrPriceConfig createAiCharacterConfig() {
        DrPriceConfig config = new DrPriceConfig();
        config.setBusinessType(BUSINESS_TYPE_AI_CHARACTER);
        config.setBusinessName("AI人设创建费用");
        config.setPriceUnit("DR/个");
        config.setDrPrice(new BigDecimal("100.0000"));
        config.setBillingType(1);
        config.setStatus("0");
        return config;
    }

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

    public static final String BUSINESS_TYPE_INSTANCE_PRE_DEDUCT = "INSTANCE_PRE_DEDUCT";
    public static final String BUSINESS_TYPE_INSTANCE_MARKETING = "INSTANCE_MARKETING";
    public static final String BUSINESS_TYPE_INSTANCE_PROSPECTING = "INSTANCE_PROSPECTING";
    public static final String BUSINESS_TYPE_AI_CHARACTER = "AI_CHARACTER";
    public static final String BUSINESS_TYPE_SMS = "SMS";
    public static final String BUSINESS_TYPE_TOKEN = "TOKEN";
    public static final String BUSINESS_TYPE_BY_TIMES = "BY_TIMES";
    public static final String BUSINESS_TYPE_AGENT_LEVEL1_COMMISSION = "AGENT_LEVEL1_COMMISSION";
    public static final String BUSINESS_TYPE_AGENT_LEVEL2_COMMISSION = "AGENT_LEVEL2_COMMISSION";
    public static final String BUSINESS_TYPE_AGENT_LEVEL3_COMMISSION = "AGENT_LEVEL3_COMMISSION";

    public static final Integer BILLING_TYPE_INSTANT = 1;
    public static final Integer BILLING_TYPE_DAILY = 2;

    /**
     * 翻译业务类型常量
     */
    public static final String BUSINESS_TYPE_TRANSLATE = "TRANSLATE";
}
