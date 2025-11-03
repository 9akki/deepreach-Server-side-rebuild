package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI角色实体类
 *
 * 存储AI角色和人设信息的实体，包含：
 * 1. 角色基本信息（名称、标识、描述等）
 * 2. 角色人设配置（提示词、头像等）
 * 3. 角色分类和来源信息
 * 4. 创建和更新时间等审计信息
 * 5. 用户关联信息
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiCharacter extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     *
     * 角色的主键标识，系统内部使用
     * 自增长主键，数据库自动生成
     */
    private Long id;

    /**
     * 角色名称
     *
     * 角色的显示名称，用户可以自定义
     * 用于角色选择和界面显示
     * 长度限制：最多100个字符
     */
    private String name;

    /**
     * 角色人设提示词
     *
     * AI角色的核心人设定义，指导AI的行为和语言风格
     * 包含角色背景、性格特点、说话方式等
     * 用于AI对话时的人设保持
     */
    private String prompt;

    /**
     * 角色描述
     *
     * 角色的详细描述信息
     * 用于角色的功能说明和特点介绍
     * 可选字段，支持富文本
     */
    private String description;

    /**
     * 角色头像URL
     *
     * 角色头像的OSS链接地址
     * 支持网络图片和本地上传的图片
     * 可选字段，不设置则使用默认头像
     */
    private String avatar;

    /**
     * 是否系统提供
     *
     * 标识角色的来源：
     * 0 - 用户自建：用户自定义创建的角色
     * 1 - 系统提供：系统预设的通用角色
     */
    private Boolean isSystem;

    /**
     * 角色分类
     *
     * 角色的功能分类：
     * emotion - 情感类：陪伴、倾听、安慰等情感支持角色
     * business - 业务类：客服、助理、咨询等业务支持角色
     */
    private String type;

    /**
     * 创建时间
     *
     * 角色创建的时间，由数据库自动设置
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 角色最后更新的时间，由数据库自动更新
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 关联的用户ID
     *
     * 角色创建者的用户ID，外键关联users表
     * 用于权限控制和角色归属管理
     */
    private Long userId;

    // ==================== 业务判断方法 ====================

    /**
     * 判断角色是否为系统提供
     *
     * @return true如果是系统角色，false如果是用户自建
     */
    public boolean isSystemCharacter() {
        return Boolean.TRUE.equals(this.isSystem);
    }

    /**
     * 判断角色是否为用户自建
     *
     * @return true如果是用户自建，false如果是系统角色
     */
    public boolean isUserCreated() {
        return !Boolean.TRUE.equals(this.isSystem);
    }

    /**
     * 判断角色是否为情感类
     *
     * @return true如果是情感类角色，false否则
     */
    public boolean isEmotionType() {
        return "emotion".equals(this.type);
    }

    /**
     * 判断角色是否为业务类
     *
     * @return true如果是业务类角色，false否则
     */
    public boolean isBusinessType() {
        return "business".equals(this.type);
    }

    /**
     * 判断角色是否有头像
     *
     * @return true如果设置了头像，false否则
     */
    public boolean hasAvatar() {
        return this.avatar != null && !this.avatar.trim().isEmpty();
    }

    /**
     * 判断角色是否有描述
     *
     * @return true如果设置了描述，false否则
     */
    public boolean hasDescription() {
        return this.description != null && !this.description.trim().isEmpty();
    }

    /**
     * 获取角色类型显示文本
     *
     * @return 角色类型显示文本
     */
    public String getTypeDisplay() {
        if ("emotion".equals(this.type)) {
            return "情感类";
        } else if ("business".equals(this.type)) {
            return "业务类";
        } else {
            return "未分类";
        }
    }

    /**
     * 获取角色来源显示文本
     *
     * @return 角色来源显示文本
     */
    public String getSourceDisplay() {
        if (Boolean.TRUE.equals(this.isSystem)) {
            return "系统提供";
        } else {
            return "用户自建";
        }
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建用户自建角色
     *
     * @param name 角色名称
     * @param prompt 角色提示词
     * @param userId 创建者用户ID
     * @return 角色对象
     */
    public static AiCharacter createUserCharacter(String name, String prompt, Long userId) {
        AiCharacter character = new AiCharacter();
        character.setName(name);
        character.setPrompt(prompt);
        character.setUserId(userId);
        character.setIsSystem(false);
        return character;
    }

    /**
     * 创建系统角色
     *
     * @param name 角色名称
     * @param prompt 角色提示词
     * @param type 角色类型
     * @return 角色对象
     */
    public static AiCharacter createSystemCharacter(String name, String prompt, String type) {
        AiCharacter character = new AiCharacter();
        character.setName(name);
        character.setPrompt(prompt);
        character.setType(type);
        character.setIsSystem(true);
        character.setUserId(1L); // 系统角色归属于超级管理员
        return character;
    }

    /**
     * 创建情感类角色
     *
     * @param name 角色名称
     * @param prompt 角色提示词
     * @param userId 创建者用户ID
     * @return 角色对象
     */
    public static AiCharacter createEmotionCharacter(String name, String prompt, Long userId) {
        AiCharacter character = createUserCharacter(name, prompt, userId);
        character.setType("emotion");
        return character;
    }

    /**
     * 创建业务类角色
     *
     * @param name 角色名称
     * @param prompt 角色提示词
     * @param userId 创建者用户ID
     * @return 角色对象
     */
    public static AiCharacter createBusinessCharacter(String name, String prompt, Long userId) {
        AiCharacter character = createUserCharacter(name, prompt, userId);
        character.setType("business");
        return character;
    }

    // ==================== 验证方法 ====================

    /**
     * 验证角色基本信息
     *
     * @return 验证结果
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
               && prompt != null && !prompt.trim().isEmpty()
               && userId != null && userId > 0;
    }

    /**
     * 验证角色名称
     *
     * @return true如果名称有效，false否则
     */
    public boolean isNameValid() {
        return name != null && name.trim().length() > 0 && name.trim().length() <= 100;
    }

    /**
     * 验证角色提示词
     *
     * @return true如果提示词有效，false否则
     */
    public boolean isPromptValid() {
        return prompt != null && prompt.trim().length() > 0;
    }

    /**
     * 验证角色类型
     *
     * @return true如果类型有效，false否则
     */
    public boolean isTypeValid() {
        return type == null || "emotion".equals(type) || "business".equals(type);
    }

    /**
     * 验证头像URL
     *
     * @return true如果头像URL有效，false否则
     */
    public boolean isAvatarValid() {
        if (avatar == null || avatar.trim().isEmpty()) {
            return true; // 头像是可选的
        }
        return avatar.length() <= 500;
    }

    /**
     * 获取角色摘要信息
     *
     * @return 角色摘要字符串
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("角色：").append(name != null ? name : "未命名");

        if (type != null) {
            summary.append(" | 类型：").append(getTypeDisplay());
        }

        summary.append(" | 来源：").append(getSourceDisplay());

        if (hasDescription()) {
            String desc = description.length() > 50 ? description.substring(0, 50) + "..." : description;
            summary.append(" | 描述：").append(desc);
        }

        return summary.toString();
    }
}