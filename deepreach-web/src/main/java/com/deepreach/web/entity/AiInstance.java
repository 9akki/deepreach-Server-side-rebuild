package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI实例实体类
 *
 * 存储AI实例的信息，包含：
 * 1. 实例基本信息（名称、类型、绑定信息等）
 * 2. 实例配置和关联信息
 * 3. 代理和网络配置
 * 4. 创建和更新时间等审计信息
 * 5. 用户和平台关联信息
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiInstance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     *
     * 实例的主键标识，系统内部使用
     * 自增长主键，数据库自动生成
     */
    private Long instanceId;

    /**
     * 创建用户ID
     *
     * 实例创建者的用户ID，关联users表
     * 用于权限控制和实例归属管理
     */
    private Long userId;

    /**
     * 实例名称
     *
     * 实例的显示名称，用户自定义
     * 用于实例识别和管理
     * 长度限制：最多20个字符
     */
    private String instanceName;

    /**
     * 实例类型
     *
     * 实例的业务类型分类：
     * 0 - 营销：营销推广类实例
     * 1 - 拓客：客户拓展类实例
     */
    private String instanceType;

    /**
     * 绑定平台ID
     *
     * 实例绑定的平台标识，关联平台表
     * 用于确定实例运行的平台环境
     */
    private Integer platformId;

    /**
     * 绑定的AI人设ID
     *
     * 实例关联的AI人设ID，关联ai_character表
     * 用于确定实例的AI角色和对话风格
     * 可选字段，实例可以不绑定特定人设
     */
    private Integer characterId;

    /**
     * 代理ID
     *
     * 实例使用的网络代理ID，关联proxy表
     * 用于网络请求的代理配置
     * 可选字段，指向代理记录的主键
     */
    private Integer proxyId;

    /**
     * 创建时间
     *
     * 实例创建的时间，由数据库自动设置
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 创建者
     *
     * 实例创建者的用户名
     */
    private String createBy;

    /**
     * 更新时间
     *
     * 实例最后更新的时间，由数据库自动更新
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 更新者
     *
     * 实例最后更新者的用户名
     */
    private String updateBy;

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
     * 判断实例是否绑定了AI人设
     *
     * @return true如果绑定了人设，false否则
     */
    public boolean hasCharacter() {
        return this.characterId != null && this.characterId > 0;
    }

    /**
     * 判断实例是否配置了代理
     *
     * @return true如果配置了代理，false否则
     */
    public boolean hasProxy() {
        return this.proxyId != null && this.proxyId > 0;
    }

    /**
     * 获取实例类型显示文本
     *
     * @return 实例类型显示文本
     */
    public String getInstanceTypeDisplay() {
        if ("0".equals(this.instanceType)) {
            return "营销";
        } else if ("1".equals(this.instanceType)) {
            return "拓客";
        } else {
            return "未知类型";
        }
    }

    /**
     * 获取实例状态显示文本
     *
     * @return 实例状态显示文本
     */
    public String getStatusDisplay() {
        StringBuilder status = new StringBuilder();
        status.append(getInstanceTypeDisplay());

        if (hasCharacter()) {
            status.append(" | 已绑定人设");
        } else {
            status.append(" | 未绑定人设");
        }

        if (hasProxy()) {
            status.append(" | 使用代理");
        }

        return status.toString();
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建营销类型实例
     *
     * @param instanceName 实例名称
     * @param userId 创建者用户ID
     * @param platformId 平台ID
     * @return 实例对象
     */
    public static AiInstance createMarketingInstance(String instanceName, Long userId, Integer platformId) {
        AiInstance instance = new AiInstance();
        instance.setInstanceName(instanceName);
        instance.setUserId(userId);
        instance.setPlatformId(platformId);
        instance.setInstanceType("0"); // 营销类型
        return instance;
    }

    /**
     * 创建拓客类型实例
     *
     * @param instanceName 实例名称
     * @param userId 创建者用户ID
     * @param platformId 平台ID
     * @return 实例对象
     */
    public static AiInstance createProspectingInstance(String instanceName, Long userId, Integer platformId) {
        AiInstance instance = new AiInstance();
        instance.setInstanceName(instanceName);
        instance.setUserId(userId);
        instance.setPlatformId(platformId);
        instance.setInstanceType("1"); // 拓客类型
        return instance;
    }

    /**
     * 创建完整实例
     *
     * @param instanceName 实例名称
     * @param instanceType 实例类型
     * @param userId 创建者用户ID
     * @param platformId 平台ID
     * @param characterId AI人设ID
     * @param proxyId 代理ID
     * @return 实例对象
     */
    public static AiInstance createCompleteInstance(String instanceName, String instanceType,
                                                   Long userId, Integer platformId,
                                                   Integer characterId, Integer proxyId) {
        AiInstance instance = new AiInstance();
        instance.setInstanceName(instanceName);
        instance.setInstanceType(instanceType);
        instance.setUserId(userId);
        instance.setPlatformId(platformId);
        instance.setCharacterId(characterId);
        instance.setProxyId(proxyId);
        return instance;
    }

    // ==================== 验证方法 ====================

    /**
     * 验证实例基本信息
     *
     * @return 验证结果
     */
    public boolean isValid() {
        return instanceName != null && !instanceName.trim().isEmpty()
               && instanceType != null && ("0".equals(instanceType) || "1".equals(instanceType))
               && userId != null && userId > 0
               && platformId != null && platformId > 0;
    }

    /**
     * 验证实例名称
     *
     * @return true如果名称有效，false否则
     */
    public boolean isInstanceNameValid() {
        return instanceName != null && instanceName.trim().length() > 0 && instanceName.trim().length() <= 20;
    }

    /**
     * 验证实例类型
     *
     * @return true如果类型有效，false否则
     */
    public boolean isInstanceTypeValid() {
        return "0".equals(instanceType) || "1".equals(instanceType);
    }

    /**
     * 验证代理ID格式
     *
     * @return true如果代理ID格式有效，false否则
     */
    public boolean isProxyIdValid() {
        if (proxyId == null) {
            return true; // 代理是可选的
        }

        // 代理ID必须是正整数
        return proxyId > 0;
    }

    /**
     * 获取实例摘要信息
     *
     * @return 实例摘要字符串
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("实例：").append(instanceName != null ? instanceName : "未命名");
        summary.append(" | 类型：").append(getInstanceTypeDisplay());
        summary.append(" | 平台ID：").append(platformId);

        if (hasCharacter()) {
            summary.append(" | 人设ID：").append(characterId);
        }

        if (hasProxy()) {
            summary.append(" | 代理ID：").append(proxyId);
        }

        return summary.toString();
    }

    /**
     * 克隆实例（用于复制）
     *
     * @param newInstanceName 新实例名称
     * @return 克隆的实例对象
     */
    public AiInstance clone(String newInstanceName) {
        AiInstance cloned = new AiInstance();
        cloned.setInstanceName(newInstanceName);
        cloned.setInstanceType(this.instanceType);
        cloned.setPlatformId(this.platformId);
        cloned.setCharacterId(this.characterId);
        cloned.setProxyId(this.proxyId);
        // userId 和其他字段需要在新创建时设置
        return cloned;
    }

    /**
     * 检查实例是否可以运行
     *
     * @return true如果实例具备运行的基本条件
     */
    public boolean canRun() {
        return isValid() && platformId != null && platformId > 0;
    }

    /**
     * 检查实例是否完整配置
     *
     * @return true如果实例配置完整
     */
    public boolean isFullyConfigured() {
        return isValid() && hasCharacter() && platformId != null && platformId > 0;
    }

    /**
     * 获取配置完整度
     *
     * @return 配置完整度百分比（0-100）
     */
    public int getConfigurationCompleteness() {
        int completeness = 0;
        int maxScore = 100;

        // 基本信息占40%
        if (isInstanceNameValid()) completeness += 10;
        if (isInstanceTypeValid()) completeness += 10;
        if (userId != null && userId > 0) completeness += 10;
        if (platformId != null && platformId > 0) completeness += 10;

        // 高级配置占60%
        if (hasCharacter()) completeness += 30;
        if (hasProxy()) completeness += 20;
        if (isProxyIdValid()) completeness += 10;

        return Math.min(completeness, maxScore);
    }
}