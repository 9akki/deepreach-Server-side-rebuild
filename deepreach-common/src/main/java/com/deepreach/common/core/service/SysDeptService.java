package com.deepreach.common.core.service;

import com.deepreach.common.core.domain.entity.SysDept;

import java.util.List;

/**
 * 部门Service接口
 *
 * 部门管理的业务逻辑接口，负责：
 * 1. 部门基本信息管理（增删改查）
 * 2. 部门树形结构管理
 * 3. 部门层级关系处理
 * 4. 部门数据权限相关查询
 * 5. 部门统计分析功能
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
public interface SysDeptService {

    /**
     * 根据部门ID查询部门信息
     *
     * 获取部门的基本信息，包括父部门、层级等
     *
     * @param deptId 部门ID
     * @return 部门对象，如果不存在则返回null
     */
    SysDept selectDeptById(Long deptId);

    /**
     * 查询部门列表（树形结构）
     *
     * 查询所有部门并构建树形结构
     * 支持多条件查询，自动过滤已删除的部门
     *
     * @param dept 查询条件对象
     * @return 部门树形结构列表
     */
    List<SysDept> selectDeptTreeList(SysDept dept);

    /**
     * 查询部门列表
     *
     * 查询所有部门，不构建树形结构
     *
     * @param dept 查询条件对象
     * @return 部门列表
     */
    List<SysDept> selectDeptList(SysDept dept);

    /**
     * 查询当前用户作为负责人的部门树形结构
     *
     * 根据当前用户ID查询其作为负责人的所有部门，并构建树形结构
     * 用于用户权限范围内的部门管理
     *
     * @param userId 用户ID
     * @return 部门树形结构列表
     */
    List<SysDept> selectManagedDeptTreeByUserId(Long userId);

    /**
     * 根据负责人用户ID查询部门列表
     *
     * 查询指定用户作为负责人的所有部门
     * 基于leader_user_id字段查询
     *
     * @param leaderUserId 负责人用户ID
     * @return 部门列表
     */
    List<SysDept> selectDeptsByLeaderUserId(Long leaderUserId);

    // ==================== 统计相关方法 ====================

    /**
     * 获取用户管理部门的统计信息
     *
     * 统计指定用户作为负责人的部门树中各种类型部门的数量
     *
     * @param userId 用户ID
     * @return 统计信息Map，包含各种类型部门的数量
     */
    java.util.Map<String, Object> getManagedDeptsStatistics(Long userId);

    /**
     * 获取用户管理的代理层级统计信息
     *
     * 统计指定用户管理的代理层级分布
     *
     * @param userId 用户ID
     * @return 代理层级统计信息
     */
    java.util.Map<String, Object> getManagedAgentLevelsStatistics(Long userId);

    /**
     * 获取用户管理的买家账户统计信息
     *
     * 统计指定用户管理的买家总账户和子账户数量
     *
     * @param userId 用户ID
     * @return 买家账户统计信息
     */
    java.util.Map<String, Object> getManagedBuyerAccountsStatistics(Long userId);

    /**
     * 获取综合统计仪表板数据
     *
     * 提供综合的统计仪表板数据
     *
     * @param userId 用户ID
     * @return 综合统计信息
     */
    java.util.Map<String, Object> getDashboardStatistics(Long userId);

    /**
     * 根据父部门ID查询子部门列表
     *
     * 查询指定父部门下的所有直接子部门
     *
     * @param parentId 父部门ID
     * @return 子部门列表
     */
    List<SysDept> selectChildrenByParentId(Long parentId);

    /**
     * 根据部门ID查询所有子部门ID列表
     *
     * 递归查询指定部门的所有子部门ID，包含自己
     * 用于数据权限控制和部门范围查询
     *
     * @param deptId 部门ID
     * @return 子部门ID列表（包含自己）
     */
    List<Long> selectChildDeptIds(Long deptId);

    /**
     * 构建部门树形结构
     *
     * 将部门列表构建成树形结构
     * 自动计算层级关系和子部门数量
     *
     * @param depts 部门列表
     * @return 树形结构的部门列表
     */
    List<SysDept> buildDeptTree(List<SysDept> depts);

    /**
     * 创建新部门
     *
     * 创建新部门，包含完整的业务逻辑：
     * 1. 参数验证和层级关系检查
     * 2. 祖级路径自动构建
     * 3. 部门编码唯一性检查
     * 4. 排序号自动生成
     *
     * @param dept 部门对象，包含必要信息
     * @return 创建成功后的部门对象，包含生成的ID
     * @throws Exception 当参数验证失败或数据冲突时抛出异常
     */
    SysDept insertDept(SysDept dept) throws Exception;

    /**
     * 更新部门信息
     *
     * 更新部门的基本信息，包含业务逻辑验证：
     * 1. 层级关系变更检查
     * 2. 祖级路径重新计算
     * 3. 循环引用检查
     * 4. 数据权限影响分析
     *
     * @param dept 部门对象，包含要更新的信息
     * @return 是否更新成功
     * @throws Exception 当参数验证失败或业务规则冲突时抛出异常
     */
    boolean updateDept(SysDept dept) throws Exception;

    /**
     * 删除部门
     *
     * 根据部门ID删除部门记录，包含依赖检查：
     * 1. 检查是否存在子部门
     * 2. 检查是否存在用户关联
     * 3. 检查是否存在角色权限关联
     * 4. 级联删除相关数据
     *
     * @param deptId 部门ID
     * @return 是否删除成功
     * @throws Exception 当存在依赖关系或无权限时抛出异常
     */
    boolean deleteDeptById(Long deptId) throws Exception;

    /**
     * 批量删除部门
     *
     * 根据部门ID列表批量删除部门
     * 包含批量依赖检查和权限验证
     *
     * @param deptIds 部门ID列表
     * @return 是否删除成功
     * @throws Exception 当存在依赖关系或无权限时抛出异常
     */
    boolean deleteDeptByIds(List<Long> deptIds) throws Exception;

    /**
     * 检查部门名称是否唯一
     *
     * 用于部门创建和修改时的唯一性验证
     * 在同一父部门下部门名称不能重复
     *
     * @param deptName 部门名称
     * @param parentId 父部门ID
     * @param deptId 排除的部门ID（用于更新验证）
     * @return 存在相同部门名称的记录数，0表示唯一
     */
    int checkDeptNameUnique(String deptName, Long parentId, Long deptId);

    /**
     * 检查是否存在子部门
     *
     * 检查指定部门是否有子部门
     * 用于删除前的依赖检查
     *
     * @param deptId 部门ID
     * @return 子部门数量
     */
    int countChildrenByDeptId(Long deptId);

    /**
     * 检查部门下是否存在用户
     *
     * 检查指定部门下是否有用户
     * 用于删除前的依赖检查
     *
     * @param deptId 部门ID
     * @return 用户数量
     */
    int countUsersByDeptId(Long deptId);

    /**
     * 获取部门的完整路径
     *
     * 获取从根部门到当前部门的完整路径名称
     * 用于显示和权限控制
     *
     * @param deptId 部门ID
     * @return 完整路径，如："总部/深圳总公司/研发中心"
     */
    String getDeptFullPath(Long deptId);

    /**
     * 获取部门层级深度
     *
     * 获取部门在组织架构中的层级深度
     * 根部门为第1级，依次递增
     *
     * @param deptId 部门ID
     * @return 层级深度
     */
    int getDeptLevel(Long deptId);

    /**
     * 检查部门是否为叶子节点
     *
     * 检查部门是否有子部门
     *
     * @param deptId 部门ID
     * @return true如果没有子部门，false如果有子部门
     */
    boolean isLeafDept(Long deptId);

    /**
     * 移动部门
     *
     * 将部门移动到新的父部门下
     * 包含层级关系重新计算和权限范围更新
     *
     * @param deptId 部门ID
     * @param newParentId 新父部门ID
     * @return 是否移动成功
     * @throws Exception 当移动操作无效或产生循环引用时抛出异常
     */
    boolean moveDept(Long deptId, Long newParentId) throws Exception;

    /**
     * 获取部门统计信息
     *
     * 获取部门相关的统计数据，包括：
     * 1. 子部门数量
     * 2. 用户数量
     * 3. 角色权限关联数量
     *
     * @param deptId 部门ID
     * @return 统计信息Map
     */
    java.util.Map<String, Object> getDeptStatistics(Long deptId);

    /**
     * 获取部门数据权限范围内的用户数量
     *
     * 根据部门数据权限范围统计用户数量
     * 用于数据权限分析和统计
     *
     * @param deptId 部门ID
     * @param dataScope 数据权限范围
     * @return 用户数量
     */
    int countUsersByDataScope(Long deptId, String dataScope);

    /**
     * 同步部门层级数据
     *
     * 重新计算和更新部门的层级相关数据：
     * 1. 祖级路径
     * 2. 层级深度
     * 3. 子部门数量
     *
     * @param deptId 部门ID
     * @return 是否同步成功
     */
    boolean syncDeptHierarchy(Long deptId);

    /**
     * 验证部门数据完整性
     *
     * 验证部门数据的完整性和一致性：
     * 1. 祖级路径是否正确
     * 2. 层级关系是否合理
     * 3. 是否存在循环引用
     *
     * @param deptId 部门ID
     * @return 验证结果，包含问题和建议
     */
    java.util.Map<String, Object> validateDeptData(Long deptId);

    // ==================== 部门权限相关方法 ====================

    /**
     * 检查用户是否有指定部门的数据权限
     *
     * 根据用户的数据权限范围和部门关系判断权限
     * 集成Spring Security获取当前用户信息
     *
     * @param deptId 目标部门ID
     * @return true如果有权限，false否则
     */
    boolean hasDeptDataPermission(Long deptId);

    /**
     * 检查用户是否有指定部门的数据权限
     *
     * 根据用户ID和目标部门ID进行权限检查
     * 不依赖当前登录用户，可以用于系统权限验证
     *
     * @param userId 用户ID
     * @param targetDeptId 目标部门ID
     * @return true如果有权限，false否则
     */
    boolean checkUserDataPermission(Long userId, Long targetDeptId);

    /**
     * 获取当前用户可以访问的部门ID列表
     *
     * 根据当前用户的数据权限范围获取可访问的部门ID
     * 用于数据权限过滤和查询范围限制
     *
     * @return 部门ID列表，如果没有权限则返回空列表
     */
    java.util.List<Long> getAccessibleDeptIds();

    /**
     * 获取用户可以访问的部门ID列表
     *
     * 根据指定用户的数据权限范围获取可访问的部门ID
     *
     * @param userId 用户ID
     * @return 部门ID列表，如果没有权限则返回空列表
     */
    java.util.List<Long> getAccessibleDeptIds(Long userId);

    /**
     * 检查用户是否可以管理指定部门
     *
     * 检查用户是否有部门管理权限，包括：
     * 1. 基本数据权限
     * 2. 部门管理功能权限
     *
     * @param deptId 部门ID
     * @return true如果可以管理，false否则
     */
    boolean canManageDept(Long deptId);

    /**
     * 检查用户是否可以查看指定部门的用户
     *
     * 用于用户列表查询时的权限控制
     *
     * @param userDeptId 用户所在部门ID
     * @return true如果可以查看，false否则
     */
    boolean canViewDeptUsers(Long userDeptId);

    /**
     * 获取部门显示名称
     *
     * 根据部门ID获取部门显示名称，提供默认值处理
     *
     * @param deptId 部门ID
     * @return 部门显示名称，如果部门不存在则返回默认值
     */
    String getDeptDisplayName(Long deptId);

    /**
     * 检查两个部门是否存在子部门关系
     *
     * 检查目标部门是否为父部门的子部门（包括自己）
     * 用于权限检查和层级关系判断
     *
     * @param parentDeptId 父部门ID
     * @param childDeptId 子部门ID
     * @return true如果存在子部门关系，false否则
     */
    boolean isChildDept(Long parentDeptId, Long childDeptId);

    /**
     * 获取用户的数据权限范围
     *
     * 根据用户角色获取数据权限范围
     *
     * @param loginUser 登录用户信息
     * @return 数据权限范围
     */
    String getUserDataScope(com.deepreach.common.core.domain.model.LoginUser loginUser);

    // ==================== 基于部门类型的业务方法 ====================

    /**
     * 根据部门类型查询部门列表
     *
     * 根据简化的部门类型查询部门列表：
     * 1 - 系统部门：系统管理员、技术管理员、运营管理员等后台用户
     * 2 - 代理部门：代理用户，只有代理部门有层级（1-3级）
     * 3 - 买家总账户部门：买家总账户用户，可以创建买家子账户
     * 4 - 买家子账户部门：买家子账户用户，只能操作自己的数据
     *
     * @param deptType 部门类型（1系统 2代理 3买家总账户 4买家子账户）
     * @return 指定类型的部门列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<SysDept> selectDeptsByDeptType(String deptType) throws Exception;

    /**
     * 根据部门类型和层级查询部门列表
     *
     * 查询指定部门类型和层级的部门列表
     * 主要用于代理部门的层级查询
     *
     * @param deptType 部门类型
     * @param level 层级
     * @return 指定类型和层级的部门列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<SysDept> selectDeptsByDeptTypeAndLevel(String deptType, Integer level) throws Exception;

    /**
     * 查询代理层级结构
     *
     * 查询所有代理部门，按层级排序
     * 用于代理层级展示和管理
     *
     * @return 代理层级结构列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<SysDept> selectAgentHierarchy() throws Exception;

    /**
     * 根据层级查询代理部门
     *
     * 查询指定层级的所有代理部门
     * 用于代理层级管理
     *
     * @param level 代理层级（1-3级）
     * @return 指定层级的代理部门列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<SysDept> selectAgentsByLevel(Integer level) throws Exception;

    /**
     * 查询指定代理部门的所有下级代理（递归）
     *
     * 递归查询指定代理部门下的所有下级代理部门
     * 用于代理权限控制和业绩统计
     *
     * @param deptId 部门ID
     * @return 所有下级代理部门列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<SysDept> selectChildAgentsRecursive(Long deptId) throws Exception;

    /**
     * 根据父部门ID查询买家总账户部门
     *
     * 查询指定代理部门下的所有买家总账户部门
     * 用于买家账户管理
     *
     * @param parentId 父部门ID（代理部门ID）
     * @return 买家总账户部门列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<SysDept> selectBuyerMainAccountsByParentId(Long parentId) throws Exception;

    /**
     * 根据买家总账户部门查询子账户部门
     *
     * 查询指定买家总账户部门下的所有子账户部门
     * 用于买家子账户管理
     *
     * @param parentBuyerDeptId 买家总账户部门ID
     * @return 买家子账户部门列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<SysDept> selectBuyerSubAccountsByParentId(Long parentBuyerDeptId) throws Exception;

    /**
     * 验证部门创建权限
     *
     * 根据当前用户的权限和部门类型规则，验证是否可以创建指定类型的部门
     * 基于部门类型权限矩阵进行验证
     *
     * @param dept 要创建的部门对象
     * @throws Exception 当无权限创建该类型部门时抛出异常
     */
    void validateDeptCreatePermission(SysDept dept) throws Exception;

    /**
     * 检查是否可以创建下级代理
     *
     * 检查当前代理部门是否可以创建下级代理部门
     * 只有1级和2级代理可以创建下级代理
     *
     * @param deptId 当前部门ID
     * @return true如果可以创建下级代理，false否则
     * @throws Exception 当查询失败时抛出异常
     */
    boolean canCreateChildAgent(Long deptId) throws Exception;

    /**
     * 检查是否可以创建买家总账户
     *
     * 检查当前部门是否可以创建买家总账户部门
     * 系统部门和2级及以上代理可以创建买家总账户
     *
     * @param deptId 当前部门ID
     * @return true如果可以创建买家总账户，false否则
     * @throws Exception 当查询失败时抛出异常
     */
    boolean canCreateBuyerAccount(Long deptId) throws Exception;

    /**
     * 检查是否可以创建买家子账户
     *
     * 检查当前部门是否可以创建买家子账户部门
     * 只有买家总账户可以创建买家子账户
     *
     * @param deptId 当前部门ID
     * @return true如果可以创建买家子账户，false否则
     * @throws Exception 当查询失败时抛出异常
     */
    boolean canCreateSubAccount(Long deptId) throws Exception;

    /**
     * 获取部门类型统计信息
     *
     * 统计各类型部门的数量和用户分布
     * 用于组织架构分析和管理
     *
     * @return 部门类型统计信息Map
     * @throws Exception 当查询失败时抛出异常
     */
    java.util.Map<String, Object> getDeptTypeStatistics() throws Exception;

    /**
     * 获取代理层级统计信息
     *
     * 统计各代理层级的部门数量和用户分布
     * 用于代理层级分析和管理
     *
     * @return 代理层级统计信息Map
     * @throws Exception 当查询失败时抛出异常
     */
    java.util.Map<String, Object> getAgentLevelStatistics() throws Exception;

    /**
     * 获取部门的组织架构信息
     *
     * 查询指定部门的完整组织架构信息
     * 包括上级部门、下级部门、层级、类型等
     *
     * @param deptId 部门ID
     * @return 组织架构信息Map
     * @throws Exception 当查询失败时抛出异常
     */
    java.util.Map<String, Object> getDeptOrgInfo(Long deptId) throws Exception;

    /**
     * 创建代理部门
     *
     * 创建新的代理部门，包含层级控制逻辑
     * 自动设置正确的层级和权限
     *
     * @param dept 代理部门对象
     * @param parentDeptId 父部门ID
     * @return 创建成功后的部门对象
     * @throws Exception 当参数验证失败或无权限时抛出异常
     */
    SysDept createAgentDept(SysDept dept, Long parentDeptId) throws Exception;

    /**
     * 创建买家总账户部门
     *
     * 创建新的买家总账户部门
     * 关联到指定的代理部门
     *
     * @param dept 买家总账户部门对象
     * @param parentAgentDeptId 代理部门ID
     * @return 创建成功后的部门对象
     * @throws Exception 当参数验证失败或无权限时抛出异常
     */
    SysDept createBuyerMainAccountDept(SysDept dept, Long parentAgentDeptId) throws Exception;

    /**
     * 创建买家子账户部门
     *
     * 创建新的买家子账户部门
     * 关联到指定的买家总账户部门
     *
     * @param dept 买家子账户部门对象
     * @param parentBuyerDeptId 买家总账户部门ID
     * @return 创建成功后的部门对象
     * @throws Exception 当参数验证失败或无权限时抛出异常
     */
    SysDept createBuyerSubAccountDept(SysDept dept, Long parentBuyerDeptId) throws Exception;
}