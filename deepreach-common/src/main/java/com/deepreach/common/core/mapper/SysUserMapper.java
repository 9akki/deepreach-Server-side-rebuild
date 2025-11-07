package com.deepreach.common.core.mapper;

import com.deepreach.common.core.domain.dto.UserHierarchyNodeDTO;
import com.deepreach.common.core.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统用户Mapper接口
 *
 * 基于部门类型的简化用户Mapper，负责用户相关的数据库操作，包括：
 * 1. 用户基本信息CRUD操作
 * 2. 用户角色关联管理
 * 3. 用户权限查询
 * 4. 用户认证相关查询
 * 5. 基于组织架构的数据权限过滤查询
 *
 * 设计理念：
 * - 部门决定用户类型：用户类型由部门类型自动决定
 * - 简化查询逻辑：移除复杂的业务字段查询
 * - 组织架构优先：基于部门类型和层级进行权限控制
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Mapper
public interface SysUserMapper {

    /**
     * 根据用户ID查询用户信息
     *
     * 查询用户的基本信息，包括用户名、密码、邮箱等
     * 不包含角色和权限信息，仅用于基本的用户信息获取
     *
     * @param userId 用户ID
     * @return 用户实体对象，如果不存在则返回null
     */
    SysUser selectUserById(@Param("userId") Long userId);

    /**
     * 根据用户名查询用户信息
     *
     * 主要用于用户登录认证，查询用户的基本信息
     * 用户名是唯一的，因此最多返回一条记录
     *
     * @param username 用户名
     * @return 用户实体对象，如果不存在则返回null
     */
    SysUser selectUserByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户信息
     *
     * 用于邮箱登录、找回密码等功能
     * 邮箱在系统中是唯一的
     *
     * @param email 用户邮箱
     * @return 用户实体对象，如果不存在则返回null
     */
    SysUser selectUserByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用户信息
     *
     * 用于手机号登录、短信验证等功能
     * 手机号在系统中是唯一的
     *
     * @param phone 用户手机号
     * @return 用户实体对象，如果不存在则返回null
     */
    SysUser selectUserByPhone(@Param("phone") String phone);

    /**
     * 查询用户的完整信息（包含角色和权限）
     *
     * 用于登录认证后的用户信息构建
     * 包含用户基本信息、角色列表、权限列表
     * 通过关联查询获取完整的数据
     *
     * @param username 用户名
     * @return 用户完整信息对象，包含角色和权限
     */
    SysUser selectUserWithRolesAndPermissions(@Param("username") String username);

    /**
     * 查询用户的完整信息（包含部门和角色）
     *
     * 用于构建返回给前端的完整用户信息
     * 包含用户基本信息、部门信息、角色列表
     * 通过关联查询获取完整的数据，用于UserVO构建
     *
     * @param userId 用户ID
     * @return 用户完整信息对象，包含部门和角色
     */
    com.deepreach.common.core.domain.vo.UserVO selectCompleteUserInfo(@Param("userId") Long userId);

    /**
     * 查询用户的完整信息（包含部门和角色）
     *
     * 用于需要用户完整信息的业务场景
     * 包含用户基本信息、部门信息、角色列表
     *
     * @param userId 用户ID
     * @return 用户完整信息对象，包含部门和角色
     */
    SysUser selectUserWithDept(@Param("userId") Long userId);

    /**
     * 查询用户拥有的角色集合
     *
     * 获取用户的所有角色对象，用于权限判断
     * 包含角色的完整信息，如角色ID、角色名称、角色标识等
     *
     * @param userId 用户ID
     * @return 角色对象集合，如果用户不存在或无角色则返回空集合
     */
    Set<com.deepreach.common.core.domain.entity.SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户拥有的角色标识集合
     *
     * 获取用户的所有角色标识（role_key），用于权限判断
     * 角色标识用于Spring Security的角色验证
     *
     * @param userId 用户ID
     * @return 角色标识集合，如果用户不存在或无角色则返回空集合
     */
    Set<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /**
     * 查询用户拥有的权限标识集合
     *
     * 获取用户通过角色获得的所有权限标识（perms）
     * 权限标识用于细粒度的权限控制
     *
     * @param userId 用户ID
     * @return 权限标识集合，如果用户不存在或无权限则返回空集合
     */
    Set<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户列表（分页）
     *
     * 支持多条件查询，包括用户名、状态、部门等
     * 自动应用数据权限过滤，确保用户只能查看有权限的用户
     *
     * @param user 查询条件对象，包含各种查询参数
     * @return 用户列表，按创建时间倒序排列
     */
    List<SysUser> selectUserList(SysUser user);

    /**
     * 根据部门ID查询用户列表
     *
     * 查询指定部门及其子部门的所有用户
     * 支持数据权限过滤，确保只能查看有权限的部门用户
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    List<SysUser> selectUsersByDeptId(@Param("deptId") Long deptId,
                                      @Param("user") SysUser user);

    /**
     * 根据部门ID查询用户列表（仅当前部门）
     *
     * 查询指定部门的用户列表，不包含子部门用户
     *
     * @param deptId 部门ID
     * @param user   查询条件
     * @return 用户列表
     */
    List<SysUser> selectUsersByDeptOnly(@Param("deptId") Long deptId, @Param("user") SysUser user);

    /**
     * 根据部门条件查询用户列表
     *
     * 支持通过部门ID及用户信息联合筛选。部门类型字段已下线。
     *
     * @param deptId 部门ID
     * @param user 用户查询条件
     * @return 用户列表
     */
    List<SysUser> searchUsersByDept(@Param("deptId") Long deptId,
                                    @Param("user") SysUser user);

    /**
     * 查询所有用户的父子关联关系
     *
     * @return 用户与父用户的关系列表
     */
    List<UserHierarchyNodeDTO> selectAllUserHierarchyRelations();

    /**
     * 查询指定部门ID列表中的用户
     *
     * 根据多个部门ID查询用户，用于部门权限范围内的用户查询
     * 通常配合数据权限功能使用
     *
     * @param deptIds 部门ID列表
     * @return 用户列表
     */
    List<SysUser> selectUsersByDeptIds(@Param("deptIds") List<Long> deptIds);

    /**
     * 统计指定部门的累计充值金额
     *
     * 用于根据买家部门ID集合聚合查询累计充值金额
     * 聚合数据来源于 user_dr_balance.total_recharge 字段
     *
     * @param deptIds 买家部门ID集合
     * @return key: dept_id, value: total_recharge
     */
    List<Map<String, Object>> sumTotalRechargeByDeptIds(@Param("deptIds") Set<Long> deptIds);

    /**
     * 汇总指定用户的累计充值金额
     *
     * 用于根据用户ID集合聚合查询累计充值金额
     * 聚合数据来源于 user_dr_balance.total_recharge 字段
     *
     * @param userIds 用户ID集合
     * @return key: user_id, value: total_recharge
     */
    List<Map<String, Object>> sumTotalRechargeByUserIds(@Param("userIds") Set<Long> userIds);

    /**
     * 统计指定用户集合的实例数量
     */
    List<Map<String, Object>> countInstancesByType(@Param("userIds") Set<Long> userIds);

    /**
     * 按平台统计指定类型实例数量
     */
    List<Map<String, Object>> countInstancesByPlatform(@Param("userIds") Set<Long> userIds,
                                                       @Param("instanceType") String instanceType);

    /**
     * 统计父用户的直属子用户角色分布。
     */
    List<Map<String, Object>> countChildrenByRoleKey(@Param("userId") Long userId);

    /**
     * 统计指定用户集合的AI人设数量
     */
    List<Map<String, Object>> countAiCharactersByType(@Param("userIds") Set<Long> userIds);

    /**
     * 查询平台列表
     */
    List<Map<String, Object>> selectAllPlatforms();

    /**
     * 查询用户DR余额
     *
     * @param userId 用户ID
     * @return DR余额
     */
    java.math.BigDecimal selectDrBalanceByUserId(@Param("userId") Long userId);

    /**
     * 统计用户总数
     *
     * 用于后台统计功能，统计符合条件的用户数量
     * 支持多条件统计，自动应用数据权限过滤
     *
     * @param user 查询条件对象
     * @return 用户总数
     */
    int countUsers(SysUser user);

    /**
     * 统计指定部门的用户数量
     *
     * 统计指定部门及其子部门的用户数量
     * 用于部门管理中的用户统计功能
     *
     * @param deptId 部门ID
     * @return 用户数量
     */
    int countUsersByDeptId(@Param("deptId") Long deptId);

    /**
     * 插入新用户
     *
     * 创建新用户记录，包含基本信息
     * 创建后需要手动分配角色和部门
     * 密码需要在Service层进行加密处理
     *
     * @param user 用户对象，包含必要的基本信息
     * @return 成功插入的记录数，通常为1
     */
    int insertUser(SysUser user);

    /**
     * 更新用户信息
     *
     * 更新用户的基本信息，不包括密码
     * 密码修改需要使用专门的updateUserPassword方法
     *
     * @param user 用户对象，包含要更新的信息
     * @return 成功更新的记录数
     */
    int updateUser(SysUser user);

    /**
     * 更新用户密码
     *
     * 专门用于密码修改，包含密码强度验证
     * 新密码需要在Service层进行加密处理
     *
     * @param userId 用户ID
     * @param password 加密后的新密码
     * @return 成功更新的记录数
     */
    int updateUserPassword(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 更新用户状态
     *
     * 用于启用/停用用户账号
     * 状态变更会立即影响用户的登录权限
     *
     * @param userId 用户ID
     * @param status 新状态（0正常 1停用）
     * @return 成功更新的记录数
     */
    int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 更新用户最后登录信息
     *
     * 记录用户最后登录的IP地址和时间
     * 用于安全审计和用户活动跟踪
     *
     * @param userId 用户ID
     * @param loginIp 登录IP地址
     * @param loginTime 登录时间
     * @return 成功更新的记录数
     */
    int updateUserLoginInfo(@Param("userId") Long userId,
                           @Param("loginIp") String loginIp,
                           @Param("loginTime") LocalDateTime loginTime);

    /**
     * 删除用户
     *
     * 根据用户ID删除用户记录
     * 会级联删除用户的角色关联关系
     * 操作不可逆，请谨慎使用
     *
     * @param userId 用户ID
     * @return 成功删除的记录数
     */
    int deleteUserById(@Param("userId") Long userId);

    /**
     * 批量删除用户
     *
     * 根据用户ID列表批量删除用户
     * 用于批量管理功能，提高操作效率
     * 会级联删除所有相关关联关系
     *
     * @param userIds 用户ID列表
     * @return 成功删除的记录数
     */
    int deleteUserByIds(@Param("userIds") List<Long> userIds);

    /**
     * 检查用户名是否唯一
     *
     * 用于用户注册和用户名修改时的唯一性验证
     * 排除指定用户ID的用户，用于更新时的验证
     *
     * @param username 用户名
     * @param userId 排除的用户ID，用于更新验证
     * @return 存在相同用户名的记录数，0表示唯一
     */
    int checkUsernameUnique(@Param("username") String username, @Param("userId") Long userId);

    /**
     * 检查邮箱是否唯一
     *
     * 用于邮箱注册和修改时的唯一性验证
     * 排除指定用户ID的用户
     *
     * @param email 邮箱地址
     * @param userId 排除的用户ID
     * @return 存在相同邮箱的记录数，0表示唯一
     */
    int checkEmailUnique(@Param("email") String email, @Param("userId") Long userId);

    /**
     * 检查手机号是否唯一
     *
     * 用于手机号注册和修改时的唯一性验证
     * 排除指定用户ID的用户
     *
     * @param phone 手机号码
     * @param userId 排除的用户ID
     * @return 存在相同手机号的记录数，0表示唯一
     */
    int checkPhoneUnique(@Param("phone") String phone, @Param("userId") Long userId);

    /**
     * 分配用户角色
     *
     * 为用户分配指定的角色，支持多角色分配
     * 先删除用户原有的所有角色，再分配新角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 成功分配的记录数
     */
    int assignUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 删除用户的所有角色
     *
     * 清空用户的所有角色分配
     * 通常在重新分配角色前调用
     *
     * @param userId 用户ID
     * @return 成功删除的记录数
     */
    int deleteUserRoles(@Param("userId") Long userId);

    /**
     * 查询用户角色ID列表
     *
     * 获取用户当前拥有的所有角色ID
     * 用于角色管理界面的角色显示和编辑
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> selectUserRoleIds(@Param("userId") Long userId);

    /**
     * 检查部门下是否存在用户
     *
     * 在删除部门前检查是否还有用户归属于该部门
     * 如果存在用户，则不允许删除部门
     *
     * @param deptId 部门ID
     * @return 该部门下的用户数量
     */
    int countUsersInDept(@Param("deptId") Long deptId);

    /**
     * 根据父用户ID查询子账号列表
     *
     * 查询指定商家总账号下的所有员工
     *
     * @param parentUserId 父用户ID（商家总账号用户ID）
     * @return 子账号列表
     */
    List<SysUser> selectSubAccountsByParentUserId(@Param("parentUserId") Long parentUserId);

    /**
     * 查询下级用户列表（根据父用户ID）
     *
     * 查询指定商户ID的所有下级用户（parent_id为该商户ID的用户）
     *
     * @param parentId 父用户ID（商户ID）
     * @return 下级用户列表
     */
    List<SysUser> selectSubUsersByParentId(@Param("parentId") Long parentId);

    /**
     * 查询买家账户树（总账户及其子账户）
     *
     * 查询指定商家总账号及其所有子账户的完整树形结构
     *
     * @param userId 商家总账号用户ID
     * @return 买家账户列表（包含总账户和所有子账户）
     */
    List<SysUser> selectBuyerAccountTree(@Param("userId") Long userId);

    /**
     * 统计指定用户的子账号数量
     *
     * @param parentUserId 父用户ID（商家总账号用户ID）
     * @return 子账号数量
     */
    int countSubAccountsByParentUserId(@Param("parentUserId") Long parentUserId);

    /**
     * 查询同级部门用户
     *
     * 查询与指定用户同级部门的用户
     * 用于同级权限控制
     *
     * @param userId 用户ID
     * @return 同级部门用户列表
     */
    List<SysUser> selectSameDeptLevelUsers(@Param("userId") Long userId);

    /**
     * 检查用户是否可以创建子账号
     *
     * 基于角色身份检查是否具备创建员工的权限。
     * 当前仅允许拥有 buyer_main 身份的用户创建子账号。
     *
     * @param userId 用户ID
     * @return true如果可以创建，false否则
     */
    boolean checkCanCreateSubAccount(@Param("userId") Long userId);

    // ==================== 用户角色关联方法 ====================

    /**
     * 为用户分配角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 成功插入的记录数
     */
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 查询指定用户集合的角色映射。
     *
     * @param userIds 用户ID集合
     * @return 每条记录包含用户ID与角色标识
     */
    java.util.List<java.util.Map<String, Object>> selectUserRoleMappings(@Param("userIds") Set<Long> userIds);

    /**
     * 根据用户ID集合批量查询用户。
     *
     * @param userIds 用户ID集合
     * @return 用户列表
     */
    List<SysUser> selectUsersByIds(@Param("userIds") Set<Long> userIds);

    /**
     * 统计指定用户集合的角色分布。
     *
     * @param userIds 用户ID集合
     * @return 每个角色对应的用户数量
     */
    java.util.List<java.util.Map<String, Object>> countUsersByRoleKeys(@Param("userIds") Set<Long> userIds);

    /**
     * 统计指定用户集合中处于启用状态的用户数量。
     *
     * @param userIds 用户ID集合
     * @return 启用状态用户数量
     */
    Long countActiveUsersByIds(@Param("userIds") Set<Long> userIds);
}
