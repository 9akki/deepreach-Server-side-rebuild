package com.deepreach.common.core.mapper;

import com.deepreach.common.core.domain.entity.SysDept;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统部门Mapper接口
 *
 * 负责部门相关的数据库操作，包括：
 * 1. 部门基本信息CRUD操作
 * 2. 部门树形结构查询
 * 3. 部门层级关系管理
 * 4. 部门数据权限相关查询
 * 5. 部门统计分析功能
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-25
 */
@Mapper
public interface SysDeptMapper {

    /**
     * 根据部门ID查询部门信息
     *
     * 查询部门的基本信息，包括父部门、层级等
     * 不包含子部门列表，仅用于基本的部门信息获取
     *
     * @param deptId 部门ID
     * @return 部门实体对象，如果不存在则返回null
     */
    SysDept selectDeptById(@Param("deptId") Long deptId);

    /**
     * 查询部门列表
     *
     * 查询所有部门，支持多条件查询
     * 自动过滤已删除的部门
     *
     * @param dept 查询条件对象，包含部门名称、状态等查询参数
     * @return 部门列表，按排序号和创建时间排序
     */
    List<SysDept> selectDeptList(SysDept dept);

    /**
     * 根据父部门ID查询子部门列表
     *
     * 查询指定父部门下的所有直接子部门
     * 用于构建部门树形结构
     *
     * @param parentId 父部门ID
     * @return 子部门列表，按排序号排序
     */
    List<SysDept> selectChildrenByParentId(@Param("parentId") Long parentId);

    /**
     * 插入新部门
     *
     * 创建新部门记录，包含基本信息
     * 祖级路径需要在Service层计算后传入
     *
     * @param dept 部门对象，包含必要的基本信息
     * @return 成功插入的记录数，通常为1
     */
    int insertDept(SysDept dept);

    /**
     * 更新部门信息
     *
     * 更新部门的基本信息，包括层级关系
     * 祖级路径变更需要在Service层重新计算
     *
     * @param dept 部门对象，包含要更新的信息
     * @return 成功更新的记录数
     */
    int updateDept(SysDept dept);

    /**
     * 删除部门
     *
     * 根据部门ID删除部门记录
     * 删除前需要检查是否存在子部门和用户关联
     *
     * @param deptId 部门ID
     * @return 成功删除的记录数
     */
    int deleteDeptById(@Param("deptId") Long deptId);

    /**
     * 批量删除部门
     *
     * 根据部门ID列表批量删除部门
     * 用于批量管理功能，提高操作效率
     *
     * @param deptIds 部门ID列表
     * @return 成功删除的记录数
     */
    int deleteDeptByIds(@Param("deptIds") List<Long> deptIds);

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
    int checkDeptNameUnique(@Param("deptName") String deptName, 
                           @Param("parentId") Long parentId, 
                           @Param("deptId") Long deptId);

    /**
     * 统计指定部门的子部门数量
     *
     * 检查指定部门是否有子部门
     * 用于删除前的依赖检查
     *
     * @param deptId 部门ID
     * @return 子部门数量
     */
    int countChildrenByDeptId(@Param("deptId") Long deptId);

    /**
     * 统计指定部门的用户数量
     *
     * 统计指定部门及其子部门的用户数量
     * 用于删除前的依赖检查和部门统计
     *
     * @param deptId 部门ID
     * @return 用户数量
     */
    int countUsersByDeptId(@Param("deptId") Long deptId);

    /**
     * 统计所有用户数量
     *
     * 用于数据权限统计功能
     * 当数据权限为"全部数据权限"时使用
     *
     * @return 所有用户数量
     */
    int countAllUsers();

    /**
     * 统计自定义部门权限范围内的用户数量
     *
     * 用于数据权限统计功能
     * 当数据权限为"自定义数据权限"时使用
     *
     * @param deptId 部门ID
     * @return 用户数量
     */
    int countUsersByCustomDept(@Param("deptId") Long deptId);

    /**
     * 根据部门ID列表统计用户数量
     *
     * 统计指定部门列表中的用户数量
     * 用于"本部门及以下数据权限"的统计
     *
     * @param deptIds 部门ID列表
     * @return 用户数量
     */
    int countUsersByDeptIds(@Param("deptIds") List<Long> deptIds);

    /**
     * 查询部门的所有子部门ID（递归）
     *
     * 递归查询指定部门的所有子部门ID，包含自己
     * 用于数据权限控制和部门范围查询
     *
     * @param deptId 部门ID
     * @return 子部门ID列表（包含自己）
     */
    List<Long> selectChildDeptIdsRecursive(@Param("deptId") Long deptId);

    /**
     * 查询指定层级的部门列表
     *
     * 根据祖级路径查询指定层级的所有部门
     * 用于层级管理和统计分析
     *
     * @param level 层级深度（1为根部门）
     * @return 指定层级的部门列表
     */
    List<SysDept> selectDeptsByLevel(@Param("level") Integer level);

    /**
     * 查询根部门列表
     *
     * 查询所有顶级部门（父部门ID为0或null）
     * 用于构建部门树的根节点
     *
     * @return 根部门列表
     */
    List<SysDept> selectRootDepts();

    /**
     * 根据负责人查询部门列表
     *
     * 查询指定用户负责的所有部门
     * 用于负责人管理功能
     *
     * @param leaderUserId 负责人用户ID
     * @return 部门列表
     */
    List<SysDept> selectDeptsByLeader(@Param("leaderUserId") Long leaderUserId);

    /**
     * 根据负责人用户ID查询部门列表
     *
     * 查询指定用户作为负责人的所有部门
     * 基于leader_user_id字段查询
     *
     * @param leaderUserId 负责人用户ID
     * @return 部门列表
     */
    List<SysDept> selectDeptsByLeaderUserId(@Param("leaderUserId") Long leaderUserId);

    /**
     * 根据部门ID列表查询部门列表
     *
     * 根据指定部门ID列表查询部门信息
     * 用于批量查询部门
     *
     * @param deptIds 部门ID列表
     * @return 部门列表
     */
    List<SysDept> selectDeptsByIds(@Param("deptIds") java.util.Set<Long> deptIds);

    // ==================== 统计相关查询方法 ====================

    /**
     * 根据部门ID列表统计各部门类型数量
     *
     * 统计指定部门ID列表中各种类型部门的数量
     *
     * @param deptIds 部门ID列表
     * @return 部门类型统计结果List (List of Map containing dept_type and count)
     */
    java.util.List<java.util.Map<String, Object>> countDeptsByTypeAndIds(@Param("deptIds") java.util.Set<Long> deptIds);

    /**
     * 根据部门ID列表统计代理部门各层级数量
     *
     * 统计指定部门ID列表中代理部门的层级分布
     *
     * @param deptIds 部门ID列表
     * @return 代理层级统计结果List (List of Map containing level and count)
     */
    java.util.List<java.util.Map<String, Object>> countAgentDeptsByLevelAndIds(@Param("deptIds") java.util.Set<Long> deptIds);

    /**
     * 根据部门ID列表统计买家账户数量
     *
     * 统计指定部门ID列表中买家总账户和子账户的数量
     *
     * @param deptIds 部门ID列表
     * @return 买家账户统计结果List (List of Map containing account_type and count)
     */
    java.util.List<java.util.Map<String, Object>> countBuyerAccountsByDeptIds(@Param("deptIds") java.util.Set<Long> deptIds);

    /**
     * 获取买家总账户详细信息
     *
     * 查询指定部门ID列表中买家总账户的详细信息，包括其子账户数量
     *
     * @param deptIds 部门ID列表
     * @return 买家总账户详细信息列表
     */
    java.util.List<java.util.Map<String, Object>> getBuyerMainAccountDetails(@Param("deptIds") java.util.Set<Long> deptIds);

    /**
     * 更新部门状态
     *
     * 批量更新部门的状态（启用/停用）
     * 用于部门状态管理
     *
     * @param deptIds 部门ID列表
     * @param status 新状态（0正常 1停用）
     * @return 成功更新的记录数
     */
    int updateDeptStatus(@Param("deptIds") List<Long> deptIds, @Param("status") String status);

    /**
     * 更新部门排序
     *
     * 批量更新部门的显示顺序
     * 用于部门排序管理
     *
     * @param deptId 部门ID
     * @param orderNum 新的排序号
     * @return 成功更新的记录数
     */
    int updateDeptOrderNum(@Param("deptId") Long deptId, @Param("orderNum") Integer orderNum);

    /**
     * 查询部门的最大排序号
     *
     * 查询指定父部门下子部门的最大排序号
     * 用于自动生成新部门的排序号
     *
     * @param parentId 父部门ID
     * @return 最大排序号，如果没有子部门则返回0
     */
    int selectMaxOrderNumByParentId(@Param("parentId") Long parentId);

    /**
     * 检查部门是否存在循环引用
     *
     * 检查将部门移动到指定父部门是否会产生循环引用
     * 用于部门移动操作的前置验证
     *
     * @param deptId 部门ID
     * @param parentId 目标父部门ID
     * @return 是否会产生循环引用（true表示会）
     */
    boolean checkDeptCycle(@Param("deptId") Long deptId, @Param("parentId") Long parentId);

    /**
     * 查询部门的完整路径信息
     *
     * 查询从根部门到当前部门的完整路径
     * 用于部门路径显示和权限控制
     *
     * @param deptId 部门ID
     * @return 部门路径列表，按层级从根到当前部门
     */
    List<SysDept> selectDeptPath(@Param("deptId") Long deptId);

    /**
     * 统计部门层级深度
     *
     * 统计组织架构的最大层级深度
     * 用于组织架构分析
     *
     * @return 最大层级深度
     */
    int selectMaxDeptLevel();

    /**
     * 查询所有启用的部门ID列表
     *
     * 查询所有状态为正常的部门ID
     * 用于数据权限过滤和部门选择器
     *
     * @return 启用的部门ID列表
     */
    List<Long> selectEnabledDeptIds();

    /**
     * 根据祖级路径查询部门
     *
     * 根据祖级路径查询匹配的部门
     * 用于层级关系查询和数据权限控制
     *
     * @param ancestors 祖级路径
     * @return 匹配的部门列表
     */
    List<SysDept> selectDeptsByAncestors(@Param("ancestors") String ancestors);

    // ==================== 基于部门类型的查询方法 ====================

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
     */
    List<SysDept> selectDeptsByDeptType(@Param("deptType") String deptType);

    /**
     * 根据部门类型和层级查询部门列表
     *
     * 查询指定部门类型和层级的部门列表
     * 主要用于代理部门的层级查询
     *
     * @param deptType 部门类型
     * @param level 层级
     * @return 指定类型和层级的部门列表
     */
    List<SysDept> selectDeptsByDeptTypeAndLevel(@Param("deptType") String deptType, @Param("level") Integer level);

    /**
     * 查询代理层级结构
     *
     * 查询所有代理部门，按层级排序
     * 用于代理层级展示和管理
     *
     * @return 代理层级结构列表
     */
    List<SysDept> selectAgentHierarchy();

    /**
     * 根据层级查询代理部门
     *
     * 查询指定层级的所有代理部门
     * 用于代理层级管理
     *
     * @param level 代理层级（1-3级）
     * @return 指定层级的代理部门列表
     */
    List<SysDept> selectAgentsByLevel(@Param("level") Integer level);

    /**
     * 查询指定代理部门的所有下级代理（递归）
     *
     * 递归查询指定代理部门下的所有下级代理部门
     * 用于代理权限控制和业绩统计
     *
     * @param deptId 部门ID
     * @return 所有下级代理部门列表
     */
    List<SysDept> selectChildAgentsRecursive(@Param("deptId") Long deptId);

    /**
     * 根据父部门ID查询买家总账户部门
     *
     * 查询指定代理部门下的所有买家总账户部门
     * 用于买家账户管理
     *
     * @param parentId 父部门ID（代理部门ID）
     * @return 买家总账户部门列表
     */
    List<SysDept> selectBuyerMainAccountsByParentId(@Param("parentId") Long parentId);

    /**
     * 根据买家总账户部门查询子账户部门
     *
     * 查询指定买家总账户部门下的所有子账户部门
     * 用于买家子账户管理
     *
     * @param parentBuyerDeptId 买家总账户部门ID
     * @return 买家子账户部门列表
     */
    List<SysDept> selectBuyerSubAccountsByParentId(@Param("parentBuyerDeptId") Long parentBuyerDeptId);

    /**
     * 统计指定代理部门的下级代理数量
     *
     * 统计指定代理部门下的直接下级代理部门数量
     * 用于代理管理和权限控制
     *
     * @param deptId 代理部门ID
     * @return 下级代理数量
     */
    int countChildAgentsByDeptId(@Param("deptId") Long deptId);

    /**
     * 统计指定代理部门的买家总账户数量
     *
     * 统计指定代理部门下的买家总账户数量
     * 用于代理管理和权限控制
     *
     * @param deptId 代理部门ID
     * @return 买家总账户数量
     */
    int countBuyerMainAccountsByDeptId(@Param("deptId") Long deptId);

    /**
     * 统计指定买家总账户的子账户数量
     *
     * 统计指定买家总账户部门下的子账户数量
     * 用于买家账户管理
     *
     * @param buyerMainDeptId 买家总账户部门ID
     * @return 子账户数量
     */
    int countBuyerSubAccountsByDeptId(@Param("buyerMainDeptId") Long buyerMainDeptId);

    /**
     * 获取部门类型统计信息
     *
     * 统计各类型部门的数量和用户分布
     * 用于组织架构分析和管理
     *
     * @return 部门类型统计信息Map
     */
    @MapKey("dept_type")
    java.util.Map<String, Object> selectDeptTypeStatistics();

    /**
     * 获取代理层级统计信息
     *
     * 统计各代理层级的部门数量和用户分布
     * 用于代理层级分析和管理
     *
     * @return 代理层级统计信息Map
     */
    @MapKey("agent_level")
    java.util.Map<String, Object> selectAgentLevelStatistics();

    /**
     * 查询部门的组织架构信息
     *
     * 查询指定部门的完整组织架构信息
     * 包括上级部门、下级部门、层级、类型等
     *
     * @param deptId 部门ID
     * @return 组织架构信息Map
     */
    @MapKey("dept_id")
    java.util.Map<String, Object> selectDeptOrgInfo(@Param("deptId") Long deptId);
}
