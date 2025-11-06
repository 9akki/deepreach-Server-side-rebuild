package com.deepreach.common.core.service;

import com.deepreach.common.core.domain.dto.UserHierarchyNodeDTO;
import com.deepreach.common.core.domain.dto.UserHierarchyGroupDTO;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.security.enums.UserIdentity;

import java.util.List;
import java.util.Set;

/**
 * 系统用户Service接口
 *
 * 基于部门类型的简化用户Service接口，负责：
 * 1. 用户基本信息管理（增删改查）
 * 2. 用户认证和授权相关业务
 * 3. 用户角色和权限管理
 * 4. 基于组织架构的数据权限控制
 * 5. 用户登录和安全相关业务
 *
 * 设计理念：
 * - 部门决定用户类型：用户类型由部门类型自动决定
 * - 简化业务逻辑：移除复杂的业务字段和操作
 * - 组织架构优先：基于部门类型和层级进行权限控制
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
public interface SysUserService {

    /**
     * 根据用户ID查询用户
     *
     * 获取用户的基本信息，不包含敏感信息
     * 用于用户信息展示和基本信息管理
     *
     * @param userId 用户ID
     * @return 用户对象，如果不存在则返回null
     */
    SysUser selectUserById(Long userId);

    /**
     * 查询用户的完整信息（包含部门）
     *
     * 获取用户完整信息，包括关联的部门对象和角色集合
     * 主要用于权限控制、组织架构判断等需要用户部门信息的业务场景
     *
     * @param userId 用户ID
     * @return 包含完整关联信息的用户对象，如果不存在则返回null
     */
    SysUser selectUserWithDept(Long userId);

    /**
     * 根据用户名查询用户
     *
     * 通过用户名获取用户基本信息
     * 主要用于用户登录认证的用户查询
     *
     * @param username 用户名
     * @return 用户对象，如果不存在则返回null
     */
    SysUser selectUserByUsername(String username);

    /**
     * 根据条件查询用户列表
     *
     * 支持多条件查询，包括用户名、状态、部门等
     * 自动应用数据权限过滤，确保只能查看有权限的用户
     *
     * @param user 查询条件对象
     * @return 用户列表
     */
    List<SysUser> selectUserList(SysUser user);

    /**
     * 基于用户层级和角色身份的组合条件查询。
     *
     * @param request 查询请求
     * @return 用户列表
     */
    List<SysUser> searchUsers(com.deepreach.common.core.domain.dto.UserListRequest request);

    /**
     * 在用户层级范围内查询用户列表
     *
     * @param rootUserId 根用户ID，可为空（表示只按身份过滤）
     * @param filter 查询条件
     * @return 用户列表
     */
    List<SysUser> selectUsersWithinHierarchy(Long rootUserId, SysUser filter);

    /**
     * 在用户层级范围内按身份过滤查询
     */
    List<SysUser> selectUsersWithinHierarchy(Long rootUserId, String identity, SysUser filter);

    /**
     * 获取指定负责人直接管理的子用户分组信息（按身份分类）。
     *
     * @param leaderUserId 负责人用户ID
     * @return 按身份聚合的直属用户分组
     */
    List<UserHierarchyGroupDTO> listUsersByLeaderDirectDepts(Long leaderUserId);

    /**
     * 获取所有用户的父子关系列表.
     *
     * @return 用户与父用户的关系列表
     */
    List<UserHierarchyNodeDTO> listAllUserHierarchyRelations();

    /**
     * 重新构建并缓存用户层级树.
     */
    void rebuildUserHierarchyCache();

    /**
     * 检查当前用户是否拥有指定用户的数据权限（基于用户树）。
     *
     * @param targetUserId 目标用户ID
     * @return true 有权限，false 无权限
     */
    boolean hasUserHierarchyPermission(Long targetUserId);

    /**
     * 创建新用户
     *
     * 创建新用户账号，包含完整的业务逻辑：
     * 1. 参数验证和唯一性检查
     * 2. 密码加密处理
     * 3. 默认角色分配（如果指定）
     * 4. 创建记录和日志记录
     *
     * @param user 用户对象，包含必要信息
     * @return 创建成功后的用户对象，包含生成的ID
     * @throws Exception 当参数验证失败或数据冲突时抛出异常
     */
    SysUser insertUser(SysUser user) throws Exception;

    /**
     * 更新用户信息
     *
     * 更新用户的基本信息，不包含密码修改
     * 包含参数验证和数据权限检查
     *
     * @param user 用户对象，包含要更新的信息
     * @return 是否更新成功
     * @throws Exception 当参数验证失败或无权限时抛出异常
     */
    boolean updateUser(SysUser user) throws Exception;

    /**
     * 删除用户
     *
     * 根据用户ID删除用户记录
     * 包含依赖检查和权限验证
     *
     * @param userId 用户ID
     * @return 是否删除成功
     * @throws Exception 当用户不存在或有依赖关系时抛出异常
     */
    boolean deleteUserById(Long userId) throws Exception;

    /**
     * 批量删除用户
     *
     * 根据用户ID列表批量删除用户
     * 包含批量依赖检查和权限验证
     *
     * @param userIds 用户ID列表
     * @return 是否删除成功
     * @throws Exception 当有用户不存在或有依赖关系时抛出异常
     */
    boolean deleteUserByIds(List<Long> userIds) throws Exception;

    /**
     * 重置用户密码
     *
     * 管理员重置用户密码功能
     * 需要管理员权限和操作审计
     *
     * @param userId 用户ID
     * @param newPassword 新密码（明文）
     * @return 是否重置成功
     * @throws Exception 当参数验证失败或无权限时抛出异常
     */
    boolean resetPassword(Long userId, String newPassword) throws Exception;

    /**
     * 修改用户密码
     *
     * 用户自己修改密码功能
     * 需要验证原密码和新密码强度
     *
     * @param userId 用户ID
     * @param oldPassword 原密码（明文）
     * @param newPassword 新密码（明文）
     * @return 是否修改成功
     * @throws Exception 当原密码错误或新密码不符合要求时抛出异常
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword) throws Exception;

    /**
     * 更新用户状态
     *
     * 启用或停用用户账号
     * 状态变更会影响用户的登录权限
     *
     * @param userId 用户ID
     * @param status 新状态（0正常 1停用）
     * @return 是否更新成功
     * @throws Exception 当用户不存在或无权限时抛出异常
     */
    boolean updateUserStatus(Long userId, String status) throws Exception;

    /**
     * 分配用户角色
     *
     * 为用户分配指定的角色列表
     * 会清除用户原有的所有角色，然后分配新角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否分配成功
     * @throws Exception 当角色不存在或无权限时抛出异常
     */
    boolean assignUserRoles(Long userId, List<Long> roleIds) throws Exception;

    /**
     * 获取用户角色ID列表
     *
     * 获取用户当前拥有的所有角色ID
     * 用于角色管理界面的显示和编辑
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> getUserRoleIds(Long userId);

    /**
     * 调整代理用户身份。
     *
     * 限定在代理层级之间升降级，会根据目标身份重新分配身份角色。
     *
     * @param userId 用户ID
     * @param targetIdentity 目标身份（仅支持代理层级）
     * @return 是否调整成功
     * @throws Exception 当参数或权限校验失败时抛出异常
     */
    boolean adjustAgentIdentity(Long userId, UserIdentity targetIdentity) throws Exception;

    /**
     * 获取用户权限标识集合
     *
     * 获取用户通过角色获得的所有权限标识
     * 用于权限验证和功能控制
     *
     * @param userId 用户ID
     * @return 权限标识集合
     */
    Set<String> getUserPermissions(Long userId);

    /**
     * 获取用户角色标识集合
     *
     * 获取用户的所有角色标识
     * 用于Spring Security的角色验证
     *
     * @param userId 用户ID
     * @return 角色标识集合
     */
    Set<String> getUserRoles(Long userId);

    /**
     * 用户登录认证
     *
     * 验证用户身份并构建登录用户对象
     * 包含完整的认证流程和权限加载
     *
     * @param username 用户名
     * @param password 密码（明文）
     * @param loginIp 登录IP地址
     * @return 登录用户对象，包含权限信息
     * @throws Exception 当认证失败时抛出异常
     */
    LoginUser authenticate(String username, String password, String loginIp) throws Exception;

    /**
     * 根据用户ID获取登录用户信息
     *
     * 查询用户信息并构建LoginUser对象，用于权限检查等场景
     *
     * @param userId 用户ID
     * @return 登录用户对象，如果用户不存在则返回null
     */
    LoginUser selectLoginUserById(Long userId);

    /**
     * 获取用户的完整信息
     *
     * 获取用户的完整信息，包含基本信息、部门信息、角色权限等
     * 用于返回给前端的完整用户视图数据
     *
     * @param userId 用户ID
     * @return 用户完整信息对象，如果用户不存在则返回null
     */
    com.deepreach.common.core.domain.vo.UserVO getCompleteUserInfo(Long userId);

    /**
     * 用户注册
     *
     * 新用户注册功能
     * 包含参数验证、唯一性检查和默认配置
     *
     * @param user 用户对象，包含注册信息
     * @return 注册成功后的用户对象
     * @throws Exception 当参数验证失败或用户名已存在时抛出异常
     */
    SysUser register(SysUser user) throws Exception;

    /**
     * 检查用户名是否唯一
     *
     * 用于用户注册和修改时的唯一性验证
     *
     * @param username 用户名
     * @param userId 排除的用户ID（用于更新验证）
     * @return true如果唯一，false如果已存在
     */
    boolean checkUsernameUnique(String username, Long userId);

    /**
     * 检查邮箱是否唯一
     *
     * 用于用户注册和修改时的唯一性验证
     *
     * @param email 邮箱地址
     * @param userId 排除的用户ID
     * @return true如果唯一，false如果已存在
     */
    boolean checkEmailUnique(String email, Long userId);

    /**
     * 检查手机号是否唯一
     *
     * 用于用户注册和修改时的唯一性验证
     *
     * @param phone 手机号码
     * @param userId 排除的用户ID
     * @return true如果唯一，false如果已存在
     */
    boolean checkPhoneUnique(String phone, Long userId);

    /**
     * 记录用户登录信息
     *
     * 更新用户最后登录的IP和时间
     * 用于安全审计和用户活动跟踪
     *
     * @param userId 用户ID
     * @param loginIp 登录IP地址
     * @param loginLocation 登录地点
     */
    void recordLoginInfo(Long userId, String loginIp, String loginLocation);

    /**
     * 获取用户统计信息
     *
     * 获取用户相关的统计数据
     * 用于管理界面的统计展示
     *
     * @param userId 用户ID
     * @return 统计信息Map
     */
    java.util.Map<String, Object> getUserStatistics(Long userId);

    /**
     * 检查用户是否有数据权限
     *
     * 验证当前登录用户是否有权限访问指定用户的数据
     * 用于数据权限控制
     *
     * @param targetUserId 目标用户ID
     * @return true如果有权限，false否则
     */
    boolean hasUserDataPermission(Long targetUserId);

    /**
     * 获取可访问的用户ID列表
     *
     * 根据当前用户的数据权限范围，获取可访问的用户ID列表
     * 用于数据权限过滤
     *
     * @return 用户ID列表
     */
    List<Long> getAccessibleUserIds();

    /**
     * 更新用户头像
     *
     * 更新用户的头像地址
     * 需要验证头像文件的有效性
     *
     * @param userId 用户ID
     * @param avatarUrl 头像URL地址
     * @return 是否更新成功
     * @throws Exception 当参数验证失败时抛出异常
     */
    boolean updateUserAvatar(Long userId, String avatarUrl) throws Exception;

    /**
     * 更新用户基本信息
     *
     * 用户自己更新基本信息（昵称、邮箱、手机号等）
     * 不包含敏感信息修改
     *
     * @param userId 用户ID
     * @param user 用户对象，包含要更新的信息
     * @return 是否更新成功
     * @throws Exception 当参数验证失败时抛出异常
     */
    boolean updateUserInfo(Long userId, SysUser user) throws Exception;

    /**
     * 导入用户数据
     *
     * 批量导入用户数据
     * 支持Excel等格式的批量导入
     *
     * @param users 用户列表
     * @param updateSupport 是否支持更新已存在的用户
     * @return 导入结果，包含成功和失败信息
     * @throws Exception 当导入过程中发生错误时抛出异常
     */
    java.util.Map<String, Object> importUsers(List<SysUser> users, boolean updateSupport) throws Exception;

    /**
     * 导出用户数据
     *
     * 导出用户数据为指定格式
     * 支持Excel等格式的数据导出
     *
     * @param users 用户列表
     * @return 导出文件的字节数组
     * @throws Exception 当导出过程中发生错误时抛出异常
     */
    byte[] exportUsers(List<SysUser> users) throws Exception;

    /**
     * 获取用户在线状态
     *
     * 检查用户是否在线
     * 通过检查用户的会话或token状态来判断
     *
     * @param userId 用户ID
     * @return true如果在线，false否则
     */
    boolean isUserOnline(Long userId);

    /**
     * 强制用户下线
     *
     * 管理员强制指定用户下线
     * 用于安全管理和异常处理
     *
     * @param userId 用户ID
     * @return 是否强制下线成功
     * @throws Exception 当操作失败时抛出异常
     */
    boolean forceUserOffline(Long userId) throws Exception;

    // ==================== 基于用户树的业务方法 ====================
    
    /**
     * 根据父用户ID查询子账号列表
     *
     * 查询指定买家总账户下的所有买家子账户
     *
     * @param parentUserId 父用户ID（买家总账户用户ID）
     * @return 子账号列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<SysUser> selectSubAccountsByParentUserId(Long parentUserId) throws Exception;

    /**
     * 查询下级用户列表（根据父用户ID）
     *
     * 查询指定商户ID的所有下级用户（parent_id为该商户ID的用户）
     * 主要用于商户管理其下级用户
     *
     * @param parentId 父用户ID（商户ID）
     * @return 下级用户列表
     */
    List<SysUser> selectSubUsersByParentId(Long parentId);

    /**
     * 查询买家账户树（总账户及其子账户）
     *
     * 查询指定买家总账户及其所有子账户的完整树形结构
     *
     * @param userId 买家总账户用户ID
     * @return 买家账户列表（包含总账户和所有子账户）
     * @throws Exception 当查询失败时抛出异常
     */
    List<SysUser> selectBuyerAccountTree(Long userId) throws Exception;

    /**
     * 检查用户是否可以创建子账号
     *
     * 基于部门类型检查用户是否有权限创建子账号
     * 只有买家总账户用户可以创建买家子账户
     *
     * @param userId 用户ID
     * @return true如果可以创建，false否则
     * @throws Exception 当查询失败时抛出异常
     */
    boolean checkCanCreateSubAccount(Long userId) throws Exception;

    // ==================== 基于简化角色体系的权限控制方法 ====================

    /**
     * 验证用户创建权限
     *
     * 根据当前用户的角色和部门类型，验证是否有权限创建指定类型的用户
     * 基于部门类型权限矩阵进行验证
     *
     * @param user 要创建的用户对象
     * @throws Exception 当无权限创建该类型用户时抛出异常
     */
    void validateUserCreatePermission(SysUser user) throws Exception;

    /**
     * 检查是否可以创建下级代理
     *
     * 检查当前代理用户是否可以创建下级代理
     * 只有代理角色且层级小于3级的用户可以创建下级代理
     *
     * @param user 当前登录用户
     * @return true如果可以创建下级代理，false否则
     */
    boolean canCreateSubAgent(com.deepreach.common.core.domain.model.LoginUser user);

    /**
     * 检查是否可以创建买家总账户
     *
     * 检查当前用户是否可以创建买家总账户
     * 系统部门和代理部门（2级及以上）可以创建买家总账户
     *
     * @param user 当前登录用户
     * @return true如果可以创建买家总账户，false否则
     */
    boolean canCreateBuyerAccount(com.deepreach.common.core.domain.model.LoginUser user);

    /**
     * 检查是否可以创建买家子账户
     *
     * 检查当前用户是否可以创建买家子账户
     * 只有买家总账户用户可以创建买家子账户
     *
     * @param user 当前登录用户
     * @return true如果可以创建买家子账户，false否则
     */
    boolean canCreateSubAccount(com.deepreach.common.core.domain.model.LoginUser user);

    // ==================== 统计相关方法 ====================

    /**
     * 获取用户管理的用户统计信息
     *
     * 基于用户层级树统计指定用户及其所有下级用户的数量，
     * 使用角色身份（admin/agent/buyer/sub_buyer）进行分组。
     * 结果同时包含代理分级明细与无法解析身份的数量。
     *
     * @param userId 用户ID
     * @return 用户统计信息
     */
    java.util.Map<String, Object> getManagedUsersStatistics(Long userId);
}
