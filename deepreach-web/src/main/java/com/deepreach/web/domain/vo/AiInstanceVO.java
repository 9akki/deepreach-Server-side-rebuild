package com.deepreach.web.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI实例视图对象
 *
 * 包含AI实例的完整信息，包括关联的平台名称
 * 用于API返回给前端的数据展示
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Data
public class AiInstanceVO {

    /**
     * 实例ID
     */
    private Long instanceId;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 实例名称
     */
    private String instanceName;

    /**
     * 实例类型（0-营销，1-拓客）
     */
    private String instanceType;

    /**
     * 实例类型显示文本
     */
    private String instanceTypeDisplay;

    /**
     * 平台ID
     */
    private Integer platformId;

    /**
     * 平台名称
     */
    private String platformName;

    /**
     * 人设ID
     */
    private Integer characterId;

    /**
     * 人设名称
     */
    private String characterName;

    /**
     * 代理ID
     */
    private Integer proxyId;

    /**
     * 代理地址
     *
     * 代理的完整地址信息，格式：hostname:port
     * 通过JOIN proxy表获取
     */
    private String proxyAddress;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 实例是否有效
     */
    private Boolean valid;

    /**
     * 实例摘要信息
     */
    private String summary;

    /**
     * 状态显示文本
     */
    private String statusDisplay;

    /**
     * 实例名称是否有效
     */
    private Boolean instanceNameValid;

    /**
     * 实例类型是否有效
     */
    private Boolean instanceTypeValid;

    /**
     * 代理ID是否有效
     */
    private Boolean proxyIdValid;

    /**
     * 是否完全配置
     */
    private Boolean fullyConfigured;

    /**
     * 是否为营销类型
     */
    private Boolean marketingType;

    /**
     * 是否为拓客类型
     */
    private Boolean prospectingType;

    /**
     * 配置完整度（0-100）
     */
    private Integer configurationCompleteness;

    /**
     * 参数数量
     */
    private Integer paramSize;

    // ==================== 业务判断方法 ====================

    /**
     * 判断实例是否为营销类型
     *
     * @return true如果是营销类型，false否则
     */
    public boolean isMarketingType() {
        return "0".equals(this.instanceType);
    }

    /**
     * 判断实例是否为拓客类型
     *
     * @return true如果是拓客类型，false否则
     */
    public boolean isProspectingType() {
        return "1".equals(this.instanceType);
    }

    /**
     * 判断实例是否完全配置
     *
     * @return true如果完全配置，false否则
     */
    public boolean isFullyConfigured() {
        return Boolean.TRUE.equals(this.fullyConfigured);
    }

    /**
     * 判断实例是否使用代理
     *
     * @return true如果使用代理，false否则
     */
    public boolean isUsingProxy() {
        return this.proxyId != null && this.proxyId > 0;
    }

    /**
     * 判断实例是否绑定了人设
     *
     * @return true如果绑定了人设，false否则
     */
    public boolean hasCharacter() {
        return this.characterId != null && this.characterId > 0;
    }

    /**
     * 获取实例类型的完整显示文本
     *
     * @return 实例类型显示文本
     */
    public String getFullTypeDisplay() {
        if ("0".equals(this.instanceType)) {
            return "营销实例";
        } else if ("1".equals(this.instanceType)) {
            return "拓客实例";
        } else {
            return "未知类型";
        }
    }

    /**
     * 获取实例的完整状态显示
     *
     * @return 完整状态显示文本
     */
    public String getFullStatusDisplay() {
        StringBuilder status = new StringBuilder();

        // 类型
        status.append(getFullTypeDisplay());

        // 人设绑定状态
        if (hasCharacter()) {
            status.append(" | 已绑定人设");
        } else {
            status.append(" | 未绑定人设");
        }

        // 代理使用状态
        if (isUsingProxy()) {
            status.append(" | 使用代理");
        } else {
            status.append(" | 未使用代理");
        }

        // 配置状态
        if (isFullyConfigured()) {
            status.append(" | 已完全配置");
        } else {
            status.append(" | 配置不完整");
        }

        return status.toString();
    }
}