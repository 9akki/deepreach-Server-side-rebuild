package com.deepreach.common.core.service;

import com.deepreach.common.core.domain.entity.SysRole;

import java.util.List;
import java.util.Set;

/**
 * 角色Service接口
 *
 * 基于部门类型的角色管理业务逻辑接口，负责：
 * 1. 角色基本信息管理（增删改查）
 * 2. 角色权限关联管理
 * 3. 角色数据权限管理
 * 4. 基于部门类型的角色分类管理
 * 5. 角色分配和权限验证
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-29
 */
public interface SysRoleService {

    /**
     * 根据角色ID查询角色信息
     *
     * 获取角色的完整信息，包括权限和部门关联
     *
     * @param roleId 角色ID
     * @return 角色对象，如果不存在则返回null
     */
    SysRole selectRoleById(Long roleId);

    /**
     * 查询角色列表
     *
     * 支持多条件查询，自动过滤已删除的角色
     *
     * @param role 查询条件对象
     * @return 角色列表
     */
    List<SysRole> selectRoleList(SysRole role);

    /**
     * 根据用户ID查询角色列表
     *
     * 查询指定用户拥有的所有角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByUserId(Long userId);

    /**
     * 根据身份标识查询角色列表（兼容旧部门类型编码）。
     *
     * @param identityAlias 身份别名（支持 1/2/3/4 或 admin/agent_level_1/buyer_main 等）
     * @return 角色列表
     */
    List<SysRole> selectRolesByIdentity(String identityAlias);

    /**
     * 创建新角色
     *
     * 创建新角色，包含完整的业务逻辑验证：
     * 1. 角色名称唯一性检查
     * 2. 角色标识唯一性检查
     * 3. 部门类型权限验证
     * 4. 数据权限范围验证
     *
     * @param role 角色对象，包含必要信息
     * @return 创建成功后的角色对象，包含生成的ID
     * @throws Exception 当参数验证失败或业务规则冲突时抛出异常
     */
    SysRole insertRole(SysRole role) throws Exception;

    /**
     * 更新角色信息
     *
     * 更新角色的基本信息，包含业务逻辑验证：
     * 1. 角色名称唯一性检查
     * 2. 角色标识唯一性检查
     * 3. 权限范围变更检查
     * 4. 用户关联影响分析
     *
     * @param role 角色对象，包含要更新的信息
     * @return 是否更新成功
     * @throws Exception 当参数验证失败或业务规则冲突时抛出异常
     */
    boolean updateRole(SysRole role) throws Exception;

    /**
     * 删除角色
     *
     * 根据角色ID删除角色记录，包含依赖检查：
     * 1. 检查是否存在用户关联
     * 2. 检查是否存在菜单权限关联
     * 3. 检查是否存在部门权限关联
     * 4. 级联删除相关数据
     *
     * @param roleId 角色ID
     * @return 是否删除成功
     * @throws Exception 当存在依赖关系或无权限时抛出异常
     */
    boolean deleteRoleById(Long roleId) throws Exception;

    /**
     * 批量删除角色
     *
     * 根据角色ID列表批量删除角色
     * 包含批量依赖检查和权限验证
     *
     * @param roleIds 角色ID列表
     * @return 是否删除成功
     * @throws Exception 当存在依赖关系或无权限时抛出异常
     */
    boolean deleteRoleByIds(List<Long> roleIds) throws Exception;

    /**
     * 检查角色名称是否唯一
     *
     * 用于角色创建和修改时的唯一性验证
     *
     * @param roleName 角色名称
     * @param roleId 排除的角色ID（用于更新验证）
     * @return 存在相同角色名称的记录数，0表示唯一
     */
    int checkRoleNameUnique(String roleName, Long roleId);

    /**
     * 检查角色标识是否唯一
     *
     * 用于角色创建和修改时的唯一性验证
     *
     * @param roleKey 角色标识
     * @param roleId 排除的角色ID（用于更新验证）
     * @return 存在相同角色标识的记录数，0表示唯一
     */
    int checkRoleKeyUnique(String roleKey, Long roleId);

    /**
     * 检查角色下是否存在用户
     *
     * 检查指定角色下是否有用户
     * 用于删除前的依赖检查
     *
     * @param roleId 角色ID
     * @return 用户数量
     */
    int countUsersByRoleId(Long roleId);

    /**
     * 为用户分配角色
     *
     * 为指定用户分配角色列表
     * 包含角色类型验证和权限检查
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否分配成功
     * @throws Exception 当角色类型不匹配或无权限时抛出异常
     */
    boolean assignUserRoles(Long userId, List<Long> roleIds) throws Exception;

    /**
     * 取消用户角色分配
     *
     * 取消指定用户的角色分配
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否取消成功
     */
    boolean cancelUserRoles(Long userId, List<Long> roleIds);

    /**
     * 获取用户角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> getUserRoleIds(Long userId);

    /**
     * 为角色分配菜单权限
     *
     * 为指定角色分配菜单权限列表
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 是否分配成功
     */
    boolean assignRoleMenus(Long roleId, List<Long> menuIds);

    /**
     * 取消角色菜单权限分配
     *
     * 取消指定角色的菜单权限分配
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 是否取消成功
     */
    boolean cancelRoleMenus(Long roleId, List<Long> menuIds);

    /**
     * 获取角色菜单ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 获取角色菜单权限标识列表
     *
     * @param roleId 角色ID
     * @return 菜单权限标识列表
     */
    Set<String> getRolePermissions(Long roleId);

    /**
     * 为角色分配数据权限（部门）
     *
     * 为指定角色分配部门数据权限
     * 仅当数据权限为自定义时有效
     *
     * @param roleId 角色ID
     * @param deptIds 部门ID列表
     * @return 是否分配成功
     */
    boolean assignRoleDepts(Long roleId, List<Long> deptIds);

    /**
     * 获取角色部门ID列表
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    List<Long> getRoleDeptIds(Long roleId);

    /**
     * 更新角色状态
     *
     * 启用或停用角色
     *
     * @param roleId 角色ID
     * @param status 状态（0正常 1停用）
     * @return 是否更新成功
     */
    boolean updateRoleStatus(Long roleId, String status);

    /**
     * 获取角色统计信息
     *
     * 获取角色相关的统计数据，包括：
     * 1. 用户数量
     * 2. 菜单权限数量
     * 3. 部门权限数量
     *
     * @param roleId 角色ID
     * @return 统计信息Map
     */
    java.util.Map<String, Object> getRoleStatistics(Long roleId);

    /**
     * 获取所有角色的统计信息
     *
     * 统计各类型角色的数量和用户分布
     *
     * @return 角色统计信息Map
     */
    java.util.Map<String, Object> getAllRoleStatistics();

    /**
     * 验证角色创建权限（基于身份规则）。
     */
    void validateRoleCreatePermission(SysRole role) throws Exception;

    /**
     * 检查角色是否适用于指定身份别名。
     */
    boolean isRoleApplicableToIdentity(Long roleId, String identityAlias);

    /**
     * 根据身份别名获取默认角色。
     */
    SysRole getDefaultRoleByIdentity(String identityAlias);

    /**
     * 初始化默认角色数据
     *
     * 创建系统默认角色数据
     * 包括各种部门类型的基础角色
     *
     * @return 是否初始化成功
     */
    boolean initDefaultRoles();

    /**
     * 同步角色权限数据
     *
     * 重新计算和更新角色的权限相关数据：
     * 1. 菜单权限缓存
     * 2. 数据权限范围
     * 3. 用户权限关联
     *
     * @param roleId 角色ID
     * @return 是否同步成功
     */
    boolean syncRolePermissions(Long roleId);

    /**
     * 验证角色数据完整性
     *
     * 验证角色数据的完整性和一致性：
     * 1. 权限关联是否正确
     * 2. 用户关联是否合理
     * 3. 部门类型是否匹配
     *
     * @param roleId 角色ID
     * @return 验证结果，包含问题和建议
     */
    java.util.Map<String, Object> validateRoleData(Long roleId);
}
