package com.deepreach.common.core.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * 用户信息传输对象
 *
 * 用于返回给前端的用户信息，不包含敏感数据
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息")
public class UserInfo {

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

    // ==================== 基于部门类型的扩展字段 ====================

    @Schema(description = "部门类型（1系统 2代理 3买家总账户 4买家子账户）")
    private String deptType;

    @Schema(description = "代理层级（仅代理部门有效）")
    private Integer agentLevel;

    @Schema(description = "父用户ID（仅买家子账户有效）")
    private Long parentUserId;

    @Schema(description = "父用户名称（仅买家子账户有效）")
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

    /**
     * 从LoginUser创建UserInfo
     *
     * @param loginUser 登录用户对象
     * @return 用户信息对象
     */
    public static UserInfo fromLoginUser(LoginUser loginUser) {
        if (loginUser == null) {
            return null;
        }

        return UserInfo.builder()
                .id(loginUser.getUserId())
                .username(loginUser.getUsername())
                .nickname(loginUser.getNickname())
                .email(loginUser.getEmail())
                .phone(loginUser.getPhone())
                .deptId(loginUser.getDeptId())
                .deptType(loginUser.getDeptType())
                .deptName(loginUser.getDeptName())
                .agentLevel(loginUser.getAgentLevel())
                .parentUserId(loginUser.getParentUserId())
                .parentUserName(loginUser.getParentUserName())
                .displayName(loginUser.getDisplayName())
                .roles(loginUser.getRoles())
                .permissions(loginUser.getPermissions())
                .loginTime(loginUser.getLoginTime())
                .build();
    }

    /**
     * 从SysUser创建UserInfo
     *
     * @param sysUser 系统用户对象
     * @return 用户信息对象
     */
    public static UserInfo fromSysUser(com.deepreach.common.core.domain.entity.SysUser sysUser) {
        if (sysUser == null) {
            return null;
        }

        // 转换Date为LocalDateTime - 使用最简单的方法
        LocalDateTime loginTime = null;
        if (sysUser.getLoginTime() != null) {
            loginTime = sysUser.getLoginTime();
        }

        LocalDateTime createTime = null;
        if (sysUser.getCreateTime() != null) {
            createTime = sysUser.getCreateTime();
        }

        LocalDateTime updateTime = null;
        if (sysUser.getUpdateTime() != null) {
            updateTime = sysUser.getUpdateTime();
        }

        // 获取部门信息
        com.deepreach.common.core.domain.entity.SysDept dept = sysUser.getDept();
        String deptType = null;
        String deptName = null;
        Integer agentLevel = null;
        Long parentUserId = null;
        String parentUserName = null;

        if (dept != null) {
            deptType = dept.getDeptType();
            deptName = dept.getDeptName();
            agentLevel = dept.getLevel();

            // 如果是买家子账户，获取父用户信息
            if (dept.isBuyerSubDept() && sysUser.getParentUserId() != null) {
                parentUserId = sysUser.getParentUserId();
                // TODO: 可以在这里查询父用户名称
            }
        }

        return UserInfo.builder()
                .id(sysUser.getUserId())
                .username(sysUser.getUsername())
                .nickname(sysUser.getNickname())
                .realName(sysUser.getRealName())
                .email(sysUser.getEmail())
                .phone(sysUser.getPhone())
                .gender(sysUser.getGender())
                .avatar(sysUser.getAvatar())
                .userType(sysUser.getUserType())
                .status(sysUser.getStatus())
                .deptId(sysUser.getDeptId())
                .deptType(deptType)
                .deptName(deptName)
                .agentLevel(agentLevel)
                .parentUserId(parentUserId)
                .parentUserName(parentUserName)
                .displayName(sysUser.getDisplayName())
                .loginIp(sysUser.getLoginIp())
                .loginTime(loginTime)
                .createTime(createTime)
                .updateTime(updateTime)
                .build();
    }

    /**
     * 检查用户是否为管理员
     *
     * @return true如果是管理员，false否则
     */
    public boolean isAdmin() {
        return id != null && 1L == id;
    }

    /**
     * 检查用户是否为后台用户
     *
     * @return true如果是后台用户，false如果是客户端用户
     */
    public boolean isBackendUser() {
        return Integer.valueOf(1).equals(this.userType);
    }

    /**
     * 检查用户是否为客户端用户
     *
     * @return true如果是客户端用户，false如果是后台用户
     */
    public boolean isClientUser() {
        return Integer.valueOf(2).equals(this.userType);
    }

    /**
     * 检查用户账号是否正常
     *
     * @return true如果账号正常，false如果账号被停用
     */
    public boolean isNormal() {
        return "0".equals(this.status);
    }

    /**
     * 检查用户账号是否被停用
     *
     * @return true如果账号被停用，false如果账号正常
     */
    public boolean isDisabled() {
        return "1".equals(this.status);
    }

    /**
     * 获取性别显示文本
     *
     * @return 性别显示文本：男/女/未知
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
     * 获取用户类型显示文本
     *
     * @return 用户类型显示文本：后台用户/客户端用户
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
     *
     * @return 状态显示文本：正常/停用
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
    public boolean isBackendDeptUser() {
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
}