package com.deepreach.common.core.domain.model;

import com.deepreach.common.core.domain.entity.SysDept;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.security.UserRoleUtils;
import com.deepreach.common.security.enums.UserIdentity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 登录用户身份权限
 *
 * 基于部门类型的登录用户模型，实现 Spring Security 的 UserDetails 接口：
 * 1. Spring Security集成 - 实现UserDetails接口
 * 2. 扩展用户信息 - 包含组织架构相关的用户数据
 * 3. 简化权限模型 - 基于部门类型的权限控制
 * 4. 序列化安全 - 敏感信息不序列化
 *
 * 设计理念：
 * - 部门决定用户类型：用户类型由部门类型自动决定
 * - 组织架构优先：重点展示部门和层级信息
 * - 统一认证模型：所有用户都使用统一的认证模型
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 父用户ID（仅买家子账户有效）
     */
    private Long parentUserId;

    /**
     * 部门概念逐步下线，仅为兼容旧代码保留。
     */
    private Long deptId;

    /**
     * 部门概念逐步下线，仅为兼容旧代码保留。
     */
    private String deptName;

    /**
     * 部门概念逐步下线，仅为兼容旧代码保留。
     */
    private Integer agentLevel;

    /**
     * 部门概念逐步下线，仅为兼容旧代码保留。
     */
    private String parentUserName;

    /**
     * 邀请码
     */
    private String invitationCode;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录密码（加密后）
     */
    private String password;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 用户状态（0正常 1停用）
     */
    private String status;

    /**
     * 登录IP地址
     */
    private String ipaddr;

    /**
     * 登录地点
     */
    private String loginLocation;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 用户权限列表
     */
    private Set<String> permissions;

    /**
     * 用户角色列表
     */
    private Set<String> roles;

    /**
     * 用户身份集合
     */
    @JsonIgnore
    private transient Set<UserIdentity> identities;

    /**
     * 用户令牌
     */
    private String token;

    /**
     * 过期时间
     */
    private Long expireTime;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 最后操作时间
     */
    private LocalDateTime lastOperateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 部门概念逐步下线，仅为兼容旧代码保留。
     */
    private transient SysDept dept;

    // ==================== UserDetails接口实现 ====================

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 账户是否未过期
     */
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账户是否未锁定
     */
    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return "0".equals(status);
    }

    /**
     * 密码是否未过期
     */
    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账户是否启用
     */
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return "0".equals(status);
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 添加角色权限（ROLE_前缀）
        if (roles != null && !roles.isEmpty()) {
            authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList()));
        }

        // 添加具体权限
        if (permissions != null && !permissions.isEmpty()) {
            authorities.addAll(permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList()));
        }

        return authorities;
    }

    // ==================== 业务方法 ====================

    /**
     * 获取显示名称
     * 优先显示昵称，其次显示用户名
     */
    public String getDisplayName() {
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname;
        }
        return username != null ? username : "未知用户";
    }

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin() {
        return userId != null && 1L == userId;
    }

    /**
     * 检查是否为超级管理员
     * 支持多种判断方式
     *
     * @return 是否为超级管理员
     */
    public boolean isSuperAdmin() {
        if (userId == null) {
            return false;
        }

        // 方式1：通过用户ID判断（最直接）
        if (1L == userId) {
            return true;
        }

        // 方式2：通过角色判断（更灵活，支持大小写）
        if (roles != null && (roles.contains("ADMIN") || roles.contains("admin"))) {
            return true;
        }

        return false;
    }

    /**
     * 检查是否有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * 检查是否有任意权限
     */
    public boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        return this.permissions != null && Arrays.stream(permissions)
                .anyMatch(this.permissions::contains);
    }

    /**
     * 检查是否有所有权限
     */
    public boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        return this.permissions != null && Arrays.stream(permissions)
                .allMatch(this.permissions::contains);
    }

    /**
     * 检查是否有指定角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 检查是否有任意角色
     */
    public boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        return this.roles != null && Arrays.stream(roles)
                .anyMatch(this.roles::contains);
    }

    /**
     * 检查是否有所有角色
     */
    public boolean hasAllRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        return this.roles != null && Arrays.stream(roles)
                .allMatch(this.roles::contains);
    }

    // ==================== 基于部门类型的业务判断方法 ====================

    public boolean hasIdentity(UserIdentity identity) {
        return UserRoleUtils.hasIdentity(this.roles, identity);
    }

    public boolean hasAnyIdentity(UserIdentity... identityList) {
        return UserRoleUtils.hasAnyIdentity(this.roles, identityList);
    }

    public boolean hasAllIdentities(UserIdentity... identityList) {
        return UserRoleUtils.hasAllIdentities(this.roles, identityList);
    }

    public boolean isAdminIdentity() {
        return hasIdentity(UserIdentity.ADMIN);
    }

    public boolean isAgentIdentity() {
        return hasAnyIdentity(UserIdentity.AGENT_LEVEL_1, UserIdentity.AGENT_LEVEL_2, UserIdentity.AGENT_LEVEL_3);
    }

    public boolean isBuyerMainIdentity() {
        return hasIdentity(UserIdentity.BUYER_MAIN);
    }

    public boolean isBuyerSubIdentity() {
        return hasIdentity(UserIdentity.BUYER_SUB);
    }

    public boolean isBuyerIdentity() {
        return isBuyerMainIdentity() || isBuyerSubIdentity();
    }

    /**
     * @deprecated 使用 {@link #isAdminIdentity()}。
     */
    @Deprecated
    /**
     * 判断是否为后台用户
     */
    public boolean isBackendUser() {
        return isAdminIdentity() || isAgentIdentity() || isBuyerMainIdentity();
    }

    /**
     * 判断是否为前端用户
     */
    public boolean isFrontendUser() {
        return isBuyerSubIdentity();
    }

    /**
     * 判断是否有父用户（是否为子账号）
     */
    public boolean hasParentUser() {
        return this.parentUserId != null && this.parentUserId > 0;
    }

    /**
     * 检查是否活跃
     * 检查用户状态和令牌是否过期
     */
    public boolean isActive() {
        return "0".equals(status) && !isTokenExpired();
    }

    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired() {
        return expireTime != null && System.currentTimeMillis() > expireTime;
    }

    /**
     * 刷新最后操作时间
     */
    public void refreshLastOperateTime() {
        this.lastOperateTime = LocalDateTime.now();
    }

    /**
     * 检查是否需要刷新令牌
     * 距离过期时间小于20分钟时需要刷新
     */
    public boolean needRefreshToken() {
        if (expireTime == null) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        long timeToExpire = expireTime - currentTime;
        return timeToExpire < 20 * 60 * 1000; // 20分钟
    }

    /**
     * 获取用户基本信息（不包含敏感信息）
     */
    public Map<String, Object> getBasicInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("userId", userId);
        info.put("username", username);
        info.put("nickname", nickname);
        info.put("email", email);
        info.put("phone", phone);
        info.put("status", status);
        info.put("deptId", deptId);
        info.put("displayName", getDisplayName());
        info.put("loginTime", loginTime);
        info.put("lastOperateTime", lastOperateTime);
        return info;
    }

    /**
     * 获取权限信息
     */
    public Map<String, Object> getPermissionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("roles", roles);
        info.put("permissions", permissions);
        info.put("isAdmin", isAdmin());
        return info;
    }

    /**
     * 获取登录信息
     */
    public Map<String, Object> getLoginInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("ipaddr", ipaddr);
        info.put("loginLocation", loginLocation);
        info.put("browser", browser);
        info.put("os", os);
        info.put("loginTime", loginTime);
        info.put("lastOperateTime", lastOperateTime);
        info.put("expireTime", expireTime);
        info.put("needRefreshToken", needRefreshToken());
        return info;
    }

    /**
     * 转换为安全的用户信息（用于返回给前端）
     */
    public UserInfo toUserInfo() {
        return UserInfo.builder()
                .id(userId)
                .username(username)
                .email(email)
                .nickname(nickname)
                .phone(phone)
                .parentUserId(parentUserId)
                .status(status)
                .roles(roles)
                .permissions(permissions)
                .displayName(getDisplayName())
                .loginTime(loginTime)
                .build();
    }

    /**
     * 从SysUser实体创建LoginUser
     */
    public static LoginUser fromSysUser(SysUser sysUser, Set<String> roles, Set<String> permissions) {
        if (sysUser == null) {
            return null;
        }

        // 获取部门信息
        SysDept dept = sysUser.getDept();
        String deptName = null;
        Integer agentLevel = null;
        Long parentUserId = sysUser.getParentUserId();
        String parentUserName = null;

        if (dept != null) {
            deptName = dept.getDeptName();
            agentLevel = dept.getLevel();
        }

        if (sysUser.isBuyerSubIdentity() && sysUser.getParentUserId() != null) {
            parentUserId = sysUser.getParentUserId();
            // TODO: 可以在这里查询父用户名称
        }

        // 转换Date为LocalDateTime
        LocalDateTime createTime = null;
        if (sysUser.getCreateTime() != null) {
            createTime = sysUser.getCreateTime();
        }

        LocalDateTime updateTime = null;
        if (sysUser.getUpdateTime() != null) {
            updateTime = sysUser.getUpdateTime();
        }

        LocalDateTime loginTime = null;
        if (sysUser.getLoginTime() != null) {
            loginTime = sysUser.getLoginTime();
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(sysUser.getUserId());
        loginUser.setDeptId(sysUser.getDeptId());
        loginUser.setDeptName(deptName);
        loginUser.setAgentLevel(agentLevel);
        loginUser.setParentUserId(parentUserId);
        loginUser.setParentUserName(parentUserName);
        loginUser.setInvitationCode(sysUser.getInvitationCode());
        loginUser.setUsername(sysUser.getUsername());
        loginUser.setPassword(sysUser.getPassword());
        loginUser.setEmail(sysUser.getEmail());
        loginUser.setNickname(sysUser.getNickname());
        loginUser.setPhone(sysUser.getPhone());
        loginUser.setStatus(sysUser.getStatus());
        loginUser.setRoles(roles != null ? roles : Collections.emptySet());
        loginUser.setPermissions(permissions != null ? permissions : Collections.emptySet());
        loginUser.setCreateTime(createTime);
        loginUser.setUpdateTime(updateTime);
        loginUser.setLoginTime(loginTime);
        loginUser.setDept(dept);
        return loginUser;
    }

    /**
     * 创建匿名用户
     */
    public static LoginUser createAnonymous() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(-1L);
        loginUser.setUsername("anonymous");
        loginUser.setStatus("0");
        loginUser.setRoles(Collections.emptySet());
        loginUser.setPermissions(Collections.emptySet());
        return loginUser;
    }
}
