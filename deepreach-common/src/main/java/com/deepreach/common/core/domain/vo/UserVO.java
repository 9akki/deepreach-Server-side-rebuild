package com.deepreach.common.core.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户信息视图对象
 *
 * 基于部门类型的简化用户信息视图对象，用于返回给前端的完整用户信息
 * 包含：用户基本信息、组织架构信息、权限相关数据、登录和审计信息
 *
 * 设计理念：
 * - 部门决定用户类型：用户类型由部门类型自动决定
 * - 简化数据结构：移除复杂的业务字段
 * - 组织架构优先：重点展示部门和层级信息
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息视图对象")
public class UserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "用户类型")
    private Integer userType;

    @Schema(description = "账号状态")
    private String status;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门类型（1系统 2代理 3买家总账户 4买家子账户）")
    private String deptType;

    @Schema(description = "部门类型显示文本")
    private String deptTypeDisplay;

    @Schema(description = "代理层级（仅代理部门有效）")
    private Integer agentLevel;

    @Schema(description = "代理层级显示文本")
    private String agentLevelDisplay;

    @Schema(description = "直属负责人用户ID")
    private Long leaderId;

    @Schema(description = "直属负责人昵称")
    private String leaderNickname;

    @Schema(description = "父用户ID（用于买家子账号）")
    private Long parentUserId;

    @Schema(description = "父用户名称（用于买家子账号）")
    private String parentUserName;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "用户角色列表")
    private Set<String> roles;

    @Schema(description = "用户权限列表")
    private Set<String> permissions;

    @Schema(description = "最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // 便捷显示字段
    @Schema(description = "性别显示")
    private String genderDisplay;

    @Schema(description = "用户类型显示")
    private String userTypeDisplay;

    @Schema(description = "状态显示")
    private String statusDisplay;

    // 业务判断方法

    /**
     * 检查用户是否为管理员
     */
    public boolean isAdmin() {
        return id != null && 1L == id;
    }

    /**
     * 检查用户是否为客户端用户
     */
    public boolean isClientUser() {
        return Integer.valueOf(2).equals(this.userType);
    }

    /**
     * 检查用户账号是否正常
     */
    public boolean isNormal() {
        return "0".equals(this.status);
    }

    /**
     * 检查用户账号是否被停用
     */
    public boolean isDisabled() {
        return "1".equals(this.status);
    }

    // ==================== 基于部门类型的业务判断方法 ====================

    /**
     * 判断是否为系统部门用户
     */
    public boolean isSystemDeptUser() {
        return "1".equals(this.deptType);
    }

    /**
     * 判断是否为代理部门用户
     */
    public boolean isAgentDeptUser() {
        return "2".equals(this.deptType);
    }

    /**
     * 判断是否为买家总账户用户
     */
    public boolean isBuyerMainAccountUser() {
        return "3".equals(this.deptType);
    }

    /**
     * 判断是否为买家子账户用户
     */
    public boolean isBuyerSubAccountUser() {
        return "4".equals(this.deptType);
    }

    /**
     * 判断是否为买家用户（总账户或子账户）
     */
    public boolean isBuyerUser() {
        return isBuyerMainAccountUser() || isBuyerSubAccountUser();
    }

    /**
     * 判断是否为后台用户
     */
    public boolean isBackendUser() {
        return isSystemDeptUser() || isAgentDeptUser() || isBuyerMainAccountUser();
    }

    /**
     * 判断是否为前端用户
     */
    public boolean isFrontendUser() {
        return isBuyerSubAccountUser();
    }

    /**
     * 判断是否有父用户（是否为子账号）
     */
    public boolean hasParentUser() {
        return this.parentUserId != null && this.parentUserId > 0;
    }

    /**
     * 获取性别显示文本
     */
    public String getGenderDisplay() {
        if ("1".equals(this.gender)) {
            return "男";
        } else if ("2".equals(this.gender)) {
            return "女";
        } else {
            return "未知";
        }
    }

    /**
     * 获取部门类型显示文本
     */
    public String getDeptTypeDisplay() {
        if (this.deptType == null) {
            return "未知类型";
        }
        switch (this.deptType) {
            case "1":
                return "系统部门";
            case "2":
                return "代理部门";
            case "3":
                return "买家总账户";
            case "4":
                return "买家子账户";
            default:
                return "未知类型";
        }
    }

    /**
     * 获取代理层级显示文本
     */
    public String getAgentLevelDisplay() {
        if (this.agentLevel == null) {
            return "未设置";
        }
        switch (this.agentLevel) {
            case 1:
                return "一级代理";
            case 2:
                return "二级代理";
            case 3:
                return "三级代理";
            default:
                return "未知层级";
        }
    }

    /**
     * 获取用户类型显示文本
     */
    public String getUserTypeDisplay() {
        if (Integer.valueOf(1).equals(this.userType)) {
            return "后台用户";
        } else if (Integer.valueOf(2).equals(this.userType)) {
            return "客户端用户";
        } else {
            return "未知类型";
        }
    }

    /**
     * 获取账号状态显示文本
     */
    public String getStatusDisplay() {
        if ("0".equals(this.status)) {
            return "正常";
        } else if ("1".equals(this.status)) {
            return "停用";
        } else {
            return "未知";
        }
    }

    /**
     * 获取显示名称（优先显示昵称，其次显示用户名）
     */
    public String getDisplayName() {
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname;
        }
        return username != null ? username : "未知用户";
    }

    /**
     * 构建完整的用户信息（包含显示字段）
     */
    public UserVO buildWithDisplayFields() {
        return UserVO.builder()
                .id(this.id)
                .username(this.username)
                .nickname(this.nickname)
                .realName(this.realName)
                .email(this.email)
                .phone(this.phone)
                .gender(this.gender)
                .avatar(this.avatar)
                .userType(this.userType)
                .status(this.status)
                .deptId(this.deptId)
                .deptType(this.deptType)
                .deptName(this.deptName)
                .deptTypeDisplay(getDeptTypeDisplay())
                .agentLevel(this.agentLevel)
                .agentLevelDisplay(getAgentLevelDisplay())
                .leaderId(this.leaderId)
                .leaderNickname(this.leaderNickname)
                .parentUserId(this.parentUserId)
                .parentUserName(this.parentUserName)
                .displayName(getDisplayName())
                .roles(this.roles)
                .permissions(this.permissions)
                .loginIp(this.loginIp)
                .loginTime(this.loginTime)
                .createTime(this.createTime)
                .updateTime(this.updateTime)
                .genderDisplay(getGenderDisplay())
                .userTypeDisplay(getUserTypeDisplay())
                .statusDisplay(getStatusDisplay())
                .build();
    }
}
