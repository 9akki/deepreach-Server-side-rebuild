package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.deepreach.common.security.enums.UserIdentity;
import java.util.HashSet;
import java.util.Set;

/**
 * 系统角色实体类 sys_role
 *
 * 基于部门类型的简化角色实体，包含：
 * 1. 角色基本信息（名称、标识、排序等）
 * 2. 数据权限范围配置
 * 3. 角色状态和菜单关联配置
 * 4. 权限控制相关属性
 * 5. 基于部门类型的角色分类
 *
 * 设计理念：
 * - 部门类型决定角色类型：角色与部门类型绑定
 * - 简化角色体系：统一的代理角色，层级权限在代码中控制
 * - 标准RBAC模式：角色作为权限载体，通过菜单关联控制访问权限
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     *
     * 角色的主键标识，系统内部使用
     * 自增长主键，数据库自动生成
     */
    private Long roleId;

    /**
     * 角色名称
     *
     * 角色的显示名称，用于系统界面显示
     * 可以包含中文，如"超级管理员"、"代理"、"商家总账号"
     * 长度限制：最多30个字符
     */
    private String roleName;

    /**
     * 角色权限字符串
     *
     * 角色的唯一标识符，用于程序中的权限判断
     * 标准角色标识：admin, system_admin, tech_admin, ops_admin, agent, buyer_main, buyer_sub
     * 在Spring Security中会添加"ROLE_"前缀
     */
    private String roleKey;

    /**
     * 显示顺序
     *
     * 角色在列表中的排序权重
     * 数值越小排序越靠前
     * 用于角色管理界面的排序显示
     */
    private Integer roleSort;

    /**
     * 数据权限范围
     *
     * 控制角色能够访问的数据范围：
     * 1 - 全部数据权限：可以访问所有数据（系统管理员）
     * 2 - 自定义数据权限：根据自定义规则访问数据
     * 3 - 本部门数据权限：只能访问本部门的数据
     * 4 - 本部门及以下数据权限：可以访问本部门及所有子部门的数据（代理、商家总账号）
     * 5 - 本人数据权限：只能访问自己的数据（员工）
     *
     * 这个字段决定了用户的数据访问范围
     */
    private String dataScope;

    /**
     * 菜单树选择项是否关联显示
     *
     * 控制菜单权限选择时的关联行为：
     * 1 - 关联显示：选择父菜单时自动选择子菜单
     * 0 - 独立选择：父子菜单独立选择
     *
     * 用于角色权限分配界面的用户体验优化
     */
    private Boolean menuCheckStrictly;

    /**
     * 部门树选择项是否关联显示
     *
     * 控制数据权限选择时的关联行为：
     * 1 - 关联显示：选择父部门时自动选择子部门
     * 0 - 独立选择：父子部门独立选择
     *
     * 用于数据权限分配界面的用户体验优化
     */
    private Boolean deptCheckStrictly;

    /**
     * 角色状态
     *
     * 角色的启用状态：
     * 0 - 正常：角色可以使用，可以分配给用户
     * 1 - 停用：角色暂停使用，不能分配给新用户，已分配的用户仍可使用
     *
     * 状态变更不会影响已分配该角色的用户
     */
    private String status;

    /**
     * 删除标志
     *
     * 软删除标识：
     * 0 - 未删除：角色正常存在
     * 2 - 已删除：角色被删除，不能使用
     *
     * 删除的角色不能分配给用户，已分配的用户会失去该角色
     */
    private String delFlag;

    // ==================== 关联数据 ====================

    /**
     * 菜单权限列表
     *
     * 该角色拥有的所有菜单权限
     * 用于权限验证和界面菜单显示
     * 通过sys_role_menu关联表获取
     */
    private Set<SysMenu> menus = new HashSet<>();

    /**
     * 部门权限列表
     *
     * 当数据权限为自定义时的部门权限列表
     * 用于自定义数据权限的范围控制
     * 只有当dataScope为"2"时才有效
     */
    private Set<SysDept> depts = new HashSet<>();

    // ==================== 业务判断方法 ====================

    /**
     * 判断角色是否为超级管理员角色
     *
     * 超级管理员角色拥有系统的所有权限
     * 通常roleKey为"admin"的角色为超级管理员
     *
     * @return true如果是超级管理员角色，false否则
     */
    public boolean isAdmin() {
        return "admin".equals(this.roleKey);
    }

    /**
     * 判断角色是否正常状态
     *
     * 检查角色状态是否为正常启用状态
     * 只有正常状态的角色才能分配给用户
     *
     * @return true如果角色正常，false如果角色被停用
     */
    public boolean isNormal() {
        return "0".equals(this.status);
    }

    /**
     * 判断角色是否被停用
     *
     * 检查角色状态是否为停用状态
     * 被停用的角色不能分配给新用户
     *
     * @return true如果角色被停用，false如果角色正常
     */
    public boolean isDisabled() {
        return "1".equals(this.status);
    }

    /**
     * 判断角色是否被删除
     *
     * 检查角色是否被软删除
     * 被删除的角色不能使用
     *
     * @return true如果角色被删除，false如果角色正常
     */
    public boolean isDeleted() {
        return "2".equals(this.delFlag);
    }

    /**
     * 判断角色是否可以分配给用户
     *
     * 综合判断角色状态，确定是否可以分配给用户
     * 需要角色正常且未被删除
     *
     * @return true如果可以分配，false如果不能分配
     */
    public boolean canAssign() {
        return isNormal() && !isDeleted();
    }

    /**
     * 判断是否为全部数据权限
     *
     * @return true如果是全部数据权限，false否则
     */
    public boolean isAllDataScope() {
        return "1".equals(this.dataScope);
    }

    /**
     * 判断是否为自定义数据权限
     *
     * @return true如果是自定义数据权限，false否则
     */
    public boolean isCustomDataScope() {
        return "2".equals(this.dataScope);
    }

    /**
     * 判断是否为本部门数据权限
     *
     * @return true如果是本部门数据权限，false否则
     */
    public boolean isDeptDataScope() {
        return "3".equals(this.dataScope);
    }

    /**
     * 判断是否为本部门及以下数据权限
     *
     * @return true如果是本部门及以下数据权限，false否则
     */
    public boolean isDeptAndChildDataScope() {
        return "4".equals(this.dataScope);
    }

    /**
     * 判断是否为本人数据权限
     *
     * @return true如果是本人数据权限，false否则
     */
    public boolean isSelfDataScope() {
        return "5".equals(this.dataScope);
    }

    
    // ==================== 基于部门类型的业务判断方法 ====================

    /**
     * 判断是否为系统角色
     *
     * @return true如果是系统角色，false否则
     */

    /**
     * 判断是否为代理角色
     *
     * @return true如果是代理角色，false否则
     */

    /**
     * 判断是否为商家总账号角色
     *
     * @return true如果是商家总账号角色，false否则
     */

    /**
     * 判断是否为员工角色
     *
     * @return true如果是员工角色，false否则
     */

    /**
     * 判断是否为买家角色（总账户或子账户）
     *
     * @return true如果是买家角色，false否则
     */

    /**
     * 判断是否为管理类角色
     *
     * @return true如果是管理类角色（系统管理员、技术管理员、运营管理员），false否则
     */
    public boolean isManagementRole() {
        return "system_admin".equals(this.roleKey)
            || "tech_admin".equals(this.roleKey)
            || "ops_admin".equals(this.roleKey);
    }

    /**
     * 判断是否为业务类角色
     *
     * @return true如果是业务类角色（代理、买家），false否则
     */
    public boolean isBusinessRole() {
        return isAgentRole() || isBuyerRole();
    }

    private UserIdentity resolveIdentity() {
        return UserIdentity.fromRoleKey(this.roleKey).orElse(null);
    }

    /**
     * 判断是否为系统角色
     */
    public boolean isSystemRole() {
        return resolveIdentity() == UserIdentity.ADMIN;
    }

    /**
     * 判断是否为代理角色（任意代理层级）
     */
    public boolean isAgentRole() {
        UserIdentity identity = resolveIdentity();
        return identity == UserIdentity.AGENT_LEVEL_1
            || identity == UserIdentity.AGENT_LEVEL_2
            || identity == UserIdentity.AGENT_LEVEL_3;
    }

    /**
     * 判断是否为商家总账号角色
     */
    public boolean isBuyerMainRole() {
        return resolveIdentity() == UserIdentity.BUYER_MAIN;
    }

    /**
     * 判断是否为员工角色
     */
    public boolean isBuyerSubRole() {
        return resolveIdentity() == UserIdentity.BUYER_SUB;
    }

    /**
     * 判断是否为买家角色（总账号或子账号）。
     */
    public boolean isBuyerRole() {
        UserIdentity identity = resolveIdentity();
        return identity == UserIdentity.BUYER_MAIN || identity == UserIdentity.BUYER_SUB;
    }

    // ==================== 显示方法 ====================

    /**
     * 获取数据权限范围显示文本
     *
     * @return 数据权限范围显示文本
     */
    public String getDataScopeDisplay() {
        switch (this.dataScope) {
            case "1":
                return "全部数据权限";
            case "2":
                return "自定义数据权限";
            case "3":
                return "本部门数据权限";
            case "4":
                return "本部门及以下数据权限";
            case "5":
                return "本人数据权限";
            default:
                return "未知权限范围";
        }
    }

    /**
     * 获取角色状态显示文本
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

    // ==================== 权限检查方法 ====================

    /**
     * 检查角色是否有指定菜单权限
     *
     * @param menuId 菜单ID
     * @return true如果有该菜单权限，false否则
     */
    public boolean hasMenu(Long menuId) {
        if (this.menus == null || this.menus.isEmpty()) {
            return false;
        }
        return this.menus.stream()
                .anyMatch(menu -> menuId.equals(menu.getMenuId()));
    }

    /**
     * 检查角色是否有指定部门权限
     *
     * @param deptId 部门ID
     * @return true如果有该部门权限，false否则
     */
    public boolean hasDept(Long deptId) {
        if (this.depts == null || this.depts.isEmpty()) {
            return false;
        }
        return this.depts.stream()
                .anyMatch(dept -> deptId.equals(dept.getDeptId()));
    }

    /**
     * 检查角色是否适用于指定部门类型
     *
     * @param targetDeptType 目标部门类型
     * @return true如果适用，false否则
     */

    // ==================== 操作方法 ====================

    /**
     * 添加菜单权限到角色
     *
     * @param menu 菜单对象
     */
    public void addMenu(SysMenu menu) {
        if (this.menus == null) {
            this.menus = new HashSet<>();
        }
        this.menus.add(menu);
    }

    /**
     * 添加部门权限到角色
     *
     * @param dept 部门对象
     */
    public void addDept(SysDept dept) {
        if (this.depts == null) {
            this.depts = new HashSet<>();
        }
        this.depts.add(dept);
    }

    // ==================== 数据转换方法 ====================

    /**
     * 创建默认的超级管理员角色
     *
     * @return 超级管理员角色对象
     */
    public static SysRole createAdminRole() {
        SysRole role = new SysRole();
        role.setRoleName("超级管理员");
        role.setRoleKey("admin");
        role.setRoleSort(1);
        role.setDataScope("1"); // 全部数据权限
        role.setStatus("0"); // 正常状态
        role.setMenuCheckStrictly(true);
        role.setDeptCheckStrictly(true);
        return role;
    }

    /**
     * 创建默认的系统管理员角色
     *
     * @return 系统管理员角色对象
     */
    public static SysRole createSystemAdminRole() {
        SysRole role = new SysRole();
        role.setRoleName("系统管理员");
        role.setRoleKey("system_admin");
        role.setRoleSort(2);
        role.setDataScope("1"); // 全部数据权限
        role.setStatus("0"); // 正常状态
        role.setMenuCheckStrictly(true);
        role.setDeptCheckStrictly(true);
        return role;
    }

    /**
     * 创建默认的技术管理员角色
     *
     * @return 技术管理员角色对象
     */
    public static SysRole createTechAdminRole() {
        SysRole role = new SysRole();
        role.setRoleName("技术管理员");
        role.setRoleKey("tech_admin");
        role.setRoleSort(3);
        role.setDataScope("1"); // 全部数据权限
        role.setStatus("0"); // 正常状态
        role.setMenuCheckStrictly(true);
        role.setDeptCheckStrictly(true);
        return role;
    }

    /**
     * 创建默认的运营管理员角色
     *
     * @return 运营管理员角色对象
     */
    public static SysRole createOpsAdminRole() {
        SysRole role = new SysRole();
        role.setRoleName("运营管理员");
        role.setRoleKey("ops_admin");
        role.setRoleSort(4);
        role.setDataScope("1"); // 全部数据权限
        role.setStatus("0"); // 正常状态
        role.setMenuCheckStrictly(true);
        role.setDeptCheckStrictly(true);
        return role;
    }

    /**
     * 创建默认的代理角色
     *
     * @return 代理角色对象
     */
    public static SysRole createAgentRole() {
        SysRole role = new SysRole();
        role.setRoleName("代理");
        role.setRoleKey("agent");
        role.setRoleSort(10);
        role.setDataScope("4"); // 本部门及以下数据权限
        role.setStatus("0"); // 正常状态
        role.setMenuCheckStrictly(false);
        role.setDeptCheckStrictly(false);
        return role;
    }

    /**
     * 创建默认的商家总账号角色
     *
     * @return 商家总账号角色对象
     */
    public static SysRole createBuyerMainRole() {
        SysRole role = new SysRole();
        role.setRoleName("商家总账号");
        role.setRoleKey("buyer_main");
        role.setRoleSort(20);
        role.setDataScope("4"); // 本部门及以下数据权限
        role.setStatus("0"); // 正常状态
        role.setMenuCheckStrictly(false);
        role.setDeptCheckStrictly(false);
        return role;
    }

    /**
     * 创建默认的员工角色
     *
     * @return 员工角色对象
     */
    public static SysRole createBuyerSubRole() {
        SysRole role = new SysRole();
        role.setRoleName("员工");
        role.setRoleKey("buyer_sub");
        role.setRoleSort(21);
        role.setDataScope("5"); // 本人数据权限
        role.setStatus("0"); // 正常状态
        role.setMenuCheckStrictly(false);
        role.setDeptCheckStrictly(false);
        return role;
    }
}
