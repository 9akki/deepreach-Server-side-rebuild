package com.deepreach.common.core.mapper;

import com.deepreach.common.core.domain.entity.SysRole;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 系统角色Mapper接口
 *
 * 负责角色相关的数据库操作，包括：
 * 1. 角色基本信息CRUD操作
 * 2. 角色菜单权限关联管理
 * 3. 角色数据权限管理
 * 4. 角色用户关联查询
 * 5. 角色权限验证相关查询
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Mapper
public interface SysRoleMapper {

    /**
     * 根据角色ID查询角色信息
     *
     * 查询角色的基本信息，包括名称、权限范围等
     * 不包含菜单和用户关联信息
     *
     * @param roleId 角色ID
     * @return 角色实体对象，如果不存在则返回null
     */
    SysRole selectRoleById(@Param("roleId") Long roleId);

    /**
     * 根据角色标识查询角色信息
     *
     * 通过角色的唯一标识roleKey查询角色信息
     * 角色标识在系统中是唯一的
     *
     * @param roleKey 角色标识
     * @return 角色实体对象，如果不存在则返回null
     */
    SysRole selectRoleByKey(@Param("roleKey") String roleKey);

    /**
     * 查询角色的完整信息（包含菜单权限）
     *
     * 用于角色管理界面的详细信息展示
     * 包含角色基本信息和关联的菜单权限列表
     *
     * @param roleId 角色ID
     * @return 角色完整信息对象，包含菜单权限
     */
    SysRole selectRoleWithMenus(@Param("roleId") Long roleId);

    /**
     * 查询角色列表（分页）
     *
     * 支持多条件查询，包括角色名称、状态等
     * 自动过滤已删除的角色
     *
     * @param role 查询条件对象，包含各种查询参数
     * @return 角色列表，按排序字段升序排列
     */
    List<SysRole> selectRoleList(SysRole role);

    /**
     * 查询所有正常状态的角色
     *
     * 用于角色分配下拉框等场景
     * 只返回正常且未删除的角色
     *
     * @return 正常角色列表
     */
    List<SysRole> selectAllNormalRoles();

    /**
     * 查询指定用户拥有的角色列表
     *
     * 获取用户当前分配的所有角色
     * 用于用户管理界面和权限验证
     *
     * @param userId 用户ID
     * @return 用户角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 查询角色关联的菜单ID列表
     *
     * 获取角色拥有的所有菜单权限ID
     * 用于角色权限管理界面的权限显示
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询角色关联的部门ID列表
     *
     * 获取角色的自定义数据权限部门ID
     * 只有当数据权限为自定义时才有意义
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询角色关联的用户数量
     *
     * 统计分配了该角色的用户数量
     * 用于角色删除前的依赖检查
     *
     * @param roleId 角色ID
     * @return 用户数量
     */
    int countUsersByRoleId(@Param("roleId") Long roleId);

    /**
     * 统计角色总数
     *
     * 用于后台统计功能，统计符合条件的角色数量
     * 支持多条件统计，自动过滤已删除的角色
     *
     * @param role 查询条件对象
     * @return 角色总数
     */
    int countRoles(SysRole role);

    /**
     * 插入新角色
     *
     * 创建新角色记录，包含基本信息和权限配置
     * 创建后需要手动分配菜单权限和数据权限
     *
     * @param role 角色对象，包含必要的基本信息
     * @return 成功插入的记录数，通常为1
     */
    int insertRole(SysRole role);

    /**
     * 更新角色信息
     *
     * 更新角色的基本信息和权限配置
     * 不会影响现有的菜单和用户关联
     *
     * @param role 角色对象，包含要更新的信息
     * @return 成功更新的记录数
     */
    int updateRole(SysRole role);

    /**
     * 更新角色状态
     *
     * 用于启用/停用角色
     * 状态变更不会影响已分配该角色的用户
     *
     * @param roleId 角色ID
     * @param status 新状态（0正常 1停用）
     * @return 成功更新的记录数
     */
    int updateRoleStatus(@Param("roleId") Long roleId, @Param("status") String status);

    /**
     * 删除角色
     *
     * 根据角色ID删除角色记录
     * 会级联删除角色的菜单关联关系和用户关联关系
     * 操作不可逆，请谨慎使用
     *
     * @param roleId 角色ID
     * @return 成功删除的记录数
     */
    int deleteRoleById(@Param("roleId") Long roleId);

    /**
     * 批量删除角色
     *
     * 根据角色ID列表批量删除角色
     * 用于批量管理功能，提高操作效率
     * 会级联删除所有相关关联关系
     *
     * @param roleIds 角色ID列表
     * @return 成功删除的记录数
     */
    int deleteRoleByIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 检查角色名称是否唯一
     *
     * 用于角色创建和修改时的唯一性验证
     * 排除指定角色ID的角色，用于更新时的验证
     *
     * @param roleName 角色名称
     * @param roleId 排除的角色ID，用于更新验证
     * @return 存在相同角色名称的记录数，0表示唯一
     */
    int checkRoleNameUnique(@Param("roleName") String roleName, @Param("roleId") Long roleId);

    /**
     * 检查角色标识是否唯一
     *
     * 用于角色创建和修改时的唯一性验证
     * 角色标识在系统中必须唯一
     *
     * @param roleKey 角色标识
     * @param roleId 排除的角色ID
     * @return 存在相同角色标识的记录数，0表示唯一
     */
    int checkRoleKeyUnique(@Param("roleKey") String roleKey, @Param("roleId") Long roleId);

    /**
     * 分配角色菜单权限
     *
     * 为角色分配指定的菜单权限，支持批量分配
     * 先删除角色原有的所有菜单权限，再分配新权限
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 成功分配的记录数
     */
    int assignRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    /**
     * 删除角色的所有菜单权限
     *
     * 清空角色的所有菜单权限分配
     * 通常在重新分配权限前调用
     *
     * @param roleId 角色ID
     * @return 成功删除的记录数
     */
    int deleteRoleMenus(@Param("roleId") Long roleId);

    /**
     * 分配角色数据权限（部门权限）
     *
     * 为角色分配自定义数据权限的部门范围
     * 只有当数据权限为自定义时才有意义
     *
     * @param roleId 角色ID
     * @param deptIds 部门ID列表
     * @return 成功分配的记录数
     */
    int assignRoleDepts(@Param("roleId") Long roleId, @Param("deptIds") List<Long> deptIds);

    /**
     * 删除角色的所有数据权限
     *
     * 清空角色的自定义数据权限部门分配
     * 通常在重新分配数据权限前调用
     *
     * @param roleId 角色ID
     * @return 成功删除的记录数
     */
    int deleteRoleDepts(@Param("roleId") Long roleId);

    /**
     * 查询角色的菜单权限标识列表
     *
     * 获取角色通过菜单获得的所有权限标识
     * 用于权限验证和功能控制
     *
     * @param roleId 角色ID
     * @return 权限标识集合，如果角色不存在或无权限则返回空集合
     */
    Set<String> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 检查角色是否有指定菜单权限
     *
     * 验证角色是否拥有特定的菜单权限
     * 用于细粒度的权限控制
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     * @return 是否有权限，true表示有权限，false表示无权限
     */
    boolean checkRoleHasMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    /**
     * 检查角色是否有指定权限标识
     *
     * 验证角色是否拥有特定的权限标识
     * 用于权限验证和功能控制
     *
     * @param roleId 角色ID
     * @param permission 权限标识
     * @return 是否有权限，true表示有权限，false表示无权限
     */
    boolean checkRoleHasPermission(@Param("roleId") Long roleId, @Param("permission") String permission);

    /**
     * 查询指定菜单权限的所有角色
     *
     * 获取拥有指定菜单权限的所有角色列表
     * 用于权限管理和反向查询
     *
     * @param menuId 菜单ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByMenuId(@Param("menuId") Long menuId);

    /**
     * 查询指定权限标识的所有角色
     *
     * 获取拥有指定权限标识的所有角色列表
     * 用于权限管理和反向查询
     *
     * @param permission 权限标识
     * @return 角色列表
     */
    List<SysRole> selectRolesByPermission(@Param("permission") String permission);

    /**
     * 复制角色权限
     *
     * 将源角色的所有权限复制到目标角色
     * 包括菜单权限和数据权限
     * 用于快速创建相似角色
     *
     * @param sourceRoleId 源角色ID
     * @param targetRoleId 目标角色ID
     * @return 成功复制的权限数量
     */
    int copyRolePermissions(@Param("sourceRoleId") Long sourceRoleId, @Param("targetRoleId") Long roleId);

    /**
     * 获取角色统计信息
     *
     * 获取角色的使用统计，包括用户数量、权限数量等
     * 用于角色管理界面的统计显示
     *
     * @param roleId 角色ID
     * @return 统计信息Map，包含userCount、menuCount、deptCount等
     */
    java.util.Map<String, Object> getRoleStatistics(@Param("roleId") Long roleId);

    // ==================== 基于部门类型的角色查询方法 ====================

    /**
     * 根据部门类型查询角色列表
     *
     * 根据简化的部门类型查询适用的角色列表：
     * 1 - 系统部门：admin, system_admin, tech_admin, ops_admin
     * 2 - 代理部门：agent
     * 3 - 买家总账户部门：buyer_main
     * 4 - 买家子账户部门：buyer_sub
     *
     * @param deptType 部门类型（1系统 2代理 3买家总账户 4买家子账户）
     * @return 适用于指定部门类型的角色列表
     */
    List<SysRole> selectRolesByDeptType(@Param("deptType") String deptType);

    /**
     * 根据数据权限范围查询角色列表
     *
     * 根据数据权限范围查询角色列表
     * 用于权限管理和统计分析
     *
     * @param dataScope 数据权限范围（1全部 2自定义 3本部门 4本部门及以下 5本人）
     * @return 指定数据权限范围的角色列表
     */
    List<SysRole> selectRolesByDataScope(@Param("dataScope") String dataScope);

    /**
     * 查询所有可以创建用户的后台角色
     *
     * 查询所有具有用户创建权限的后台角色
     * 包括系统管理员、代理、买家总账户角色
     *
     * @return 可以创建用户的角色列表
     */
    List<SysRole> selectRolesCanCreateUsers();

    /**
     * 查询所有可以创建下级代理的角色
     *
     * 查询所有具有下级代理创建权限的角色
     * 主要是系统管理员和代理角色
     *
     * @return 可以创建下级代理的角色列表
     */
    List<SysRole> selectRolesCanCreateAgents();

    /**
     * 查询所有可以创建买家账户的角色
     *
     * 查询所有具有买家账户创建权限的角色
     * 主要是系统管理员和代理角色
     *
     * @return 可以创建买家账户的角色列表
     */
    List<SysRole> selectRolesCanCreateBuyers();

    /**
     * 查询所有可以创建子账户的角色
     *
     * 查询所有具有子账户创建权限的角色
     * 主要是买家总账户角色
     *
     * @return 可以创建子账户的角色列表
     */
    List<SysRole> selectRolesCanCreateSubAccounts();

    /**
     * 查询指定代理层级的角色
     *
     * 查询适用于指定代理层级的角色
     * 用于代理层级权限管理
     *
     * @param agentLevel 代理层级（1-3级）
     * @return 适用于指定代理层级的角色列表
     */
    List<SysRole> selectRolesByAgentLevel(@Param("agentLevel") Integer agentLevel);

    /**
     * 获取部门类型角色统计信息
     *
     * 统计各部门类型适用的角色数量和分布
     * 用于角色管理统计报表
     *
     * @return 部门类型角色统计信息Map列表
     */
    @MapKey("dept_type")
    List<java.util.Map<String, Object>> selectDeptTypeRoleStatistics();

    /**
     * 获取数据权限角色统计信息
     *
     * 统计各数据权限范围的角色数量和分布
     * 用于数据权限管理统计报表
     *
     * @return 数据权限角色统计信息Map列表
     */
    @MapKey("data_scope")
    List<java.util.Map<String, Object>> selectDataScopeRoleStatistics();

    /**
     * 查询角色权限配置信息
     *
     * 查询指定角色的详细权限配置信息
     * 包括菜单权限数量、用户数量、适用部门类型等
     *
     * @param roleId 角色ID
     * @return 角色权限配置信息Map
     */
    @MapKey("role_id")
    java.util.Map<String, Object> selectRolePermissionConfig(@Param("roleId") Long roleId);

    /**
     * 检查角色是否适用于指定部门类型
     *
     * 验证角色是否可以分配给指定部门类型的用户
     * 用于角色分配权限验证
     *
     * @param roleId 角色ID
     * @param deptType 部门类型
     * @return 是否适用，true表示适用，false表示不适用
     */
    boolean checkRoleApplicableToDeptType(@Param("roleId") Long roleId, @Param("deptType") String deptType);

    /**
     * 检查角色是否可以分配给指定层级的代理
     *
     * 验证角色是否可以分配给指定层级的代理用户
     * 用于代理角色分配权限验证
     *
     * @param roleId 角色ID
     * @param agentLevel 代理层级
     * @return 是否可以分配，true表示可以，false表示不可以
     */
    boolean checkRoleCanAssignToAgentLevel(@Param("roleId") Long roleId, @Param("agentLevel") Integer agentLevel);

    // ==================== 用户角色关联方法 ====================

    /**
     * 为用户分配角色
     *
     * 批量为用户分配多个角色
     * 用于用户管理中的角色分配功能
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 成功分配的记录数
     */
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 取消用户角色分配
     *
     * 批量取消用户的角色分配
     * 如果roleIds为null，则取消该用户的所有角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表，为null时取消所有角色
     * @return 成功取消的记录数
     */
    int deleteUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 查询用户的角色ID列表
     *
     * 获取用户当前拥有的所有角色ID
     * 用于用户角色查询和权限验证
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> selectUserRoleIds(@Param("userId") Long userId);

    /**
     * 根据角色标识查询角色ID
     *
     * @param roleKey 角色标识
     * @return 角色ID
     */
    Long selectRoleIdByKey(@Param("roleKey") String roleKey);
}