package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import com.deepreach.common.security.UserRoleUtils;
import com.deepreach.common.security.enums.UserIdentity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 系统用户实体类 sys_user
 *
 * 基于部门类型的简化用户实体，包含：
 * 1. 用户基本信息（账号、密码、联系方式等）
 * 2. 用户状态和权限信息
 * 3. 组织架构信息（部门类型决定用户类型）
 * 4. 登录和审计信息
 * 5. 角色和权限关联数据
 *
 * 设计理念：
 * - 部门决定用户类型：用户类型完全由所在部门的类型决定
 * - 层级关系简化：通过部门的parent_id和level字段体现层级关系
 * - 去除复杂业务字段：只关注组织架构和权限控制
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     *
     * 用户的主键标识，系统内部使用
     * 自增长主键，数据库自动生成
     */
    private Long userId;

    /**
     * 用户账号
     *
     * 用户的登录账号，系统内唯一
     * 用于用户登录和身份识别
     * 长度限制：3-20个字符，只能包含字母、数字、下划线
     */
    private String username;

    /**
     * 用户密码
     *
     * 用户登录密码，经过BCrypt加密存储
     * 永远不在前端显示，仅用于后端验证
     * 加密算法：BCrypt，自动加盐
     */
    private String password;

    /**
     * 用户昵称
     *
     * 用户的显示名称，可以包含中文
     * 用于系统界面显示，非唯一字段
     * 长度限制：最多30个字符
     */
    private String nickname;

    /**
     * 真实姓名
     *
     * 用户的真实姓名，用于实名认证
     * 可选字段，支持中英文姓名
     * 长度限制：最多30个字符
     */
    private String realName;

    /**
     * 用户邮箱
     *
     * 用户的电子邮箱地址，系统内唯一
     * 用于找回密码、通知等业务功能
     * 需要进行邮箱格式验证
     */
    private String email;

    /**
     * 手机号码
     *
     * 用户的手机号码，系统内唯一
     * 用于短信验证、登录等业务功能
     * 需要进行手机号格式验证（中国大陆11位）
     */
    private String phone;

    /**
     * 用户性别
     *
     * 用户的性别信息：
     * 1 - 男性
     * 2 - 女性
     * 0 - 未知/不愿透露
     */
    private String gender;

    /**
     * 头像地址
     *
     * 用户头像的URL地址
     * 支持网络图片和本地上传的图片
     * 可选字段，不设置则使用默认头像
     */
    private String avatar;

    /**
     * 用户类型
     *
     * 区分不同类型的用户，采用不同权限策略：
     * 1 - 后台管理用户：需要完整RBAC权限控制
     * 2 - 客户端用户：只需要基本身份验证
     *
     * 这个字段决定了用户的权限管理模式
     */
    private Integer userType;

    /**
     * 账号状态
     *
     * 用户账号的启用状态：
     * 0 - 正常：可以正常登录和使用系统
     * 1 - 停用：无法登录，账号被暂停使用
     *
     * 状态变更会立即影响用户的系统访问权限
     */
    private String status;

    /**
     * 部门ID
     *
     * 用户所属部门的ID，关联sys_dept表
     * 用于组织架构管理和数据权限控制
     * 用户类型由部门类型自动决定：
     * - dept_type = 1: 系统部门 → 后台用户
     * - dept_type = 2: 代理部门 → 后台用户
     * - dept_type = 3: 买家总账户 → 后台用户
     * - dept_type = 4: 买家子账户 → 客户端用户
     *
     * 注意：此字段为必填项，用户必须属于某个部门
     */
    private Long deptId;

    /**
     * 父用户ID（用于买家子账户）
     *
     * 仅当用户所在部门类型为买家子账户(dept_type=4)时有效
     * 用于建立买家总账户与子账户的父子关系
     * 买家子账户的父用户必须是买家总账户类型的用户
     */
    private Long parentUserId;

    /**
     * 父用户账号（基于 parent_user_id 回查）
     *
     * 重构后用于列表/详情展示，不再依赖部门。
     */
    private String parentUsername;
    /**
     * 父用户角色标识集合，用于前端快速识别上级身份。
     */
    private Set<String> parentRoles = new LinkedHashSet<>();

    /**
     * 邀请码
     *
     * 用户邀请下级账号时使用的唯一邀请码
     */
    private String invitationCode;

    /**
     * 最后登录IP地址
     *
     * 用户最后一次成功登录的IP地址
     * 用于安全审计和异常登录检测
     * 由系统自动记录，用户无法修改
     */
    private String loginIp;

    /**
     * 最后登录时间
     *
     * 用户最后一次成功登录的时间
     * 用于用户活动跟踪和安全审计
     * 由系统自动记录，用户无法修改
     */
    private LocalDateTime loginTime;

    /**
     * 用户角色标识列表
     *
     * 用户当前拥有的所有角色标识(roleKey)
     * 用于权限控制和功能访问
     * 通过sys_user_role关联表获取
     */
    private Set<String> roles = new HashSet<>();

    /**
     * 用户权限标识列表
     *
     * 用户通过角色获得的所有权限标识
     * 用于细粒度的功能权限控制
     * 通过角色权限关联表获取
     */
    private Set<String> permissions = new HashSet<>();

    /**
     * 获取用户身份集合（根据角色解析，结果不可修改）。
     *
     * @return 用户身份集合
     */
    @JsonIgnore
    public Set<UserIdentity> getIdentities() {
        return UserRoleUtils.resolveIdentities(this.roles);
    }

    /**
     * 判断是否包含指定身份。
     */
    public boolean hasIdentity(UserIdentity identity) {
        return UserRoleUtils.hasIdentity(this.roles, identity);
    }

    /**
     * 判断是否包含任一身份。
     */
    public boolean hasAnyIdentity(UserIdentity... identities) {
        return UserRoleUtils.hasAnyIdentity(this.roles, identities);
    }

    /**
     * 判断是否为管理员身份。
     */
    public boolean isAdminIdentity() {
        return hasIdentity(UserIdentity.ADMIN);
    }

    /**
     * 判断是否为代理身份（任意代理层级）。
     */
    public boolean isAgentIdentity() {
        return hasAnyIdentity(UserIdentity.AGENT_LEVEL_1, UserIdentity.AGENT_LEVEL_2, UserIdentity.AGENT_LEVEL_3);
    }

    /**
     * 判断是否为买家总账户身份。
     */
    public boolean isBuyerMainIdentity() {
        return hasIdentity(UserIdentity.BUYER_MAIN);
    }

    /**
     * 判断是否为买家子账户身份。
     */
    public boolean isBuyerSubIdentity() {
        return hasIdentity(UserIdentity.BUYER_SUB);
    }

    /**
     * 判断是否为买家身份（总账户或子账户）。
     */
    public boolean isBuyerIdentity() {
        return isBuyerMainIdentity() || isBuyerSubIdentity();
    }

  
    /**
     * 部门显示名称（用于前端显示）
     *
     * 部门的完整显示名称
     * 用于前端展示用户所属部门
     */
    private transient String deptDisplayName;

    /**
     * 部门信息
     *
     * 用户所属部门的详细信息
     * 包含部门名称、层级等组织架构信息
     * 通过deptId关联查询获得
     */
    private transient SysDept dept;

    /**
     * 直属部门负责人ID
     *
     * 通过用户所属部门的leader_user_id获取
     * 用于前端展示和业务计算
     */
    private transient Long leaderId;

    /**
     * 直属部门负责人昵称
     *
     * 通过负责人ID查询用户昵称
     */
    private transient String leaderNickname;

    // ==================== 业务判断方法 ====================

    /**
     * 判断用户是否为超级管理员
     *
     * 超级管理员拥有系统的所有权限
     * 通常userId为1的用户为超级管理员
     *
     * @return true如果是超级管理员，false否则
     */
    public boolean isAdmin() {
        return this.userId != null && this.userId == 1L;
    }

    /**
     * 判断用户是否为后台管理用户
     *
     * 后台管理用户需要完整的RBAC权限控制
     * 可以访问系统管理功能
     *
     * @return true如果是后台用户，false如果是客户端用户
     */
    public boolean isBackendUser() {
        return Integer.valueOf(1).equals(this.userType);
    }

    /**
     * 判断用户是否为客户端用户
     *
     * 客户端用户只需要基本身份验证
     * 通常只能访问自己的数据
     *
     * @return true如果是客户端用户，false如果是后台用户
     */
    public boolean isClientUser() {
        return Integer.valueOf(2).equals(this.userType);
    }

  
    /**
     * 判断用户账号是否正常
     *
     * 检查用户状态是否为正常启用状态
     * 只有正常状态的用户才能登录系统
     *
     * @return true如果账号正常，false如果账号被停用
     */
    public boolean isNormal() {
        return "0".equals(this.status);
    }

    /**
     * 判断用户账号是否被停用
     *
     * 检查用户状态是否为停用状态
     * 被停用的用户无法登录系统
     *
     * @return true如果账号被停用，false如果账号正常
     */
    public boolean isDisabled() {
        return "1".equals(this.status);
    }

    /**
     * 判断用户是否已分配部门
     *
     * 检查用户是否归属于某个部门
     * 用于数据权限控制和组织管理
     * 注意：在新的设计中，用户必须属于某个部门
     *
     * @return true如果已分配部门，false如果未分配部门
     */
    public boolean hasDept() {
        return this.deptId != null && this.deptId > 0;
    }

    /**
     * 判断用户是否为系统部门用户
     *
     * 根据用户所属部门类型判断
     * 系统部门用户拥有系统管理权限
     *
     * @return true如果是系统部门用户，false否则
     */
    /**
     * @deprecated 使用 {@link #isAdminIdentity()}。
     */
    @Deprecated
    public boolean isSystemDeptUser() {
        return isAdminIdentity();
    }

    /**
     * 判断用户是否为代理部门用户
     *
     * 根据用户所属部门类型判断
     * 代理部门用户拥有代理管理权限
     *
     * @return true如果是代理部门用户，false否则
     */
    /**
     * @deprecated 使用 {@link #isAgentIdentity()}。
     */
    @Deprecated
    public boolean isAgentDeptUser() {
        return isAgentIdentity();
    }

    /**
     * 判断用户是否为买家总账户用户
     *
     * 根据用户所属部门类型判断
     * 买家总账户用户可以创建和管理子账户
     *
     * @return true如果是买家总账户用户，false否则
     */
    /**
     * @deprecated 使用 {@link #isBuyerMainIdentity()}。
     */
    @Deprecated
    public boolean isBuyerMainAccountUser() {
        return isBuyerMainIdentity();
    }

    /**
     * 判断用户是否为买家子账户用户
     *
     * 根据用户所属部门类型判断
     * 买家子账户用户只能访问自己的数据
     *
     * @return true如果是买家子账户用户，false否则
     */
    /**
     * @deprecated 使用 {@link #isBuyerSubIdentity()}。
     */
    @Deprecated
    public boolean isBuyerSubAccountUser() {
        return isBuyerSubIdentity();
    }

    /**
     * 判断用户是否为买家用户（总账户或子账户）
     *
     * @return true如果是买家用户，false否则
     */
    /**
     * @deprecated 使用 {@link #isBuyerIdentity()}。
     */
    @Deprecated
    public boolean isBuyerUser() {
        return isBuyerIdentity();
    }

    /**
     * 判断用户是否有父用户（是否为子账号）
     *
     * @return true如果有父用户，false否则
     */
    public boolean hasParentUser() {
        return this.parentUserId != null && this.parentUserId > 0;
    }

    /**
     * 判断用户是否为男性
     *
     * @return true如果是男性，false否则
     */
    public boolean isMale() {
        return "1".equals(this.gender);
    }

    /**
     * 判断用户是否为女性
     *
     * @return true如果是女性，false否则
     */
    public boolean isFemale() {
        return "2".equals(this.gender);
    }

    /**
     * 获取用户显示名称
     *
     * 获取用于界面显示的用户名称
     * 优先级：昵称 > 真实姓名 > 用户名
     *
     * @return 显示名称，如果都没有则返回"未知用户"
     */
    public String getDisplayName() {
        if (this.nickname != null && !this.nickname.trim().isEmpty()) {
            return this.nickname;
        }
        if (this.realName != null && !this.realName.trim().isEmpty()) {
            return this.realName;
        }
        if (this.username != null && !this.username.trim().isEmpty()) {
            return this.username;
        }
        return "未知用户";
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

    /**
     * 检查用户是否有指定角色
     *
     * @param roleKey 角色标识
     * @return true如果有该角色，false否则
     */
    public boolean hasRole(String roleKey) {
        if (this.roles == null || this.roles.isEmpty()) {
            return false;
        }
        return this.roles.contains(roleKey);
    }

    /**
     * 检查用户是否有指定权限
     *
     * @param permission 权限标识
     * @return true如果有该权限，false否则
     */
    public boolean hasPermission(String permission) {
        if (this.permissions == null || this.permissions.isEmpty()) {
            return false;
        }
        return this.permissions.contains(permission);
    }

    /**
     * 添加角色到用户
     *
     * @param roleKey 角色标识
     */
    public void addRole(String roleKey) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(roleKey);
    }

    /**
     * 添加权限到用户
     *
     * @param permission 权限标识
     */
    public void addPermission(String permission) {
        if (this.permissions == null) {
            this.permissions = new HashSet<>();
        }
        this.permissions.add(permission);
    }

    // ==================== 数据转换方法 ====================

    /**
     * 创建用于注册的用户对象
     *
     * 注意：在新设计中，用户必须指定部门ID，用户类型由部门类型自动决定
     *
     * @param username 用户名
     * @param password 密码（明文，需要后续加密）
     * @param email 邮箱
     * @param deptId 部门ID（必填）
     * @return 用户对象
     */
    public static SysUser createForRegister(String username, String password, String email, Long deptId) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setDeptId(deptId);
        user.setStatus("0"); // 默认正常状态
        // 用户类型将由部门类型自动决定，这里不需要设置
        return user;
    }

    /**
     * 创建用于导入的用户对象
     *
     * @param username 用户名
     * @param nickname 昵称
     * @param email 邮箱
     * @param phone 手机号
     * @param deptId 部门ID（必填）
     * @return 用户对象
     */
    public static SysUser createForImport(String username, String nickname, String email, String phone, Long deptId) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        user.setDeptId(deptId);
        user.setStatus("0"); // 默认正常状态
        // 用户类型将由部门类型自动决定，这里不需要设置
        return user;
    }

    /**
     * 创建买家子账户用户对象
     *
     * @param username 用户名
     * @param password 密码（明文，需要后续加密）
     * @param nickname 昵称
     * @param email 邮箱
     * @param deptId 买家子账户部门ID
     * @param parentUserId 父用户ID（买家总账户用户ID）
     * @return 用户对象
     */
    public static SysUser createBuyerSubUser(String username, String password, String nickname, String email, Long deptId, Long parentUserId) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setDeptId(deptId);
        user.setParentUserId(parentUserId);
        user.setStatus("0"); // 默认正常状态
        // 用户类型将由部门类型自动决定为客户端用户
        return user;
    }

    // 为了兼容性，添加loginDate方法
    public void setLoginDate(LocalDateTime loginDate) {
        this.loginTime = loginDate;
    }

    public LocalDateTime getLoginDate() {
        return this.loginTime;
    }

    public LocalDateTime getLoginTime() {
        return this.loginTime;
    }

    public Long getParentUserId() {
        return this.parentUserId;
    }

    // ==================== 简化字段的Getter和Setter方法 ====================

    
    /**
     * 获取部门显示名称
     */
    public String getDeptDisplayName() {
        return deptDisplayName;
    }

    /**
     * 设置部门显示名称
     */
    public void setDeptDisplayName(String deptDisplayName) {
        this.deptDisplayName = deptDisplayName;
    }

}
