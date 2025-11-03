package com.deepreach.web.mapper;

import com.deepreach.web.entity.AiInstance;
import com.deepreach.common.core.domain.entity.Instance;
import com.deepreach.web.entity.dto.InstanceTypeStatistics;
import com.deepreach.web.entity.dto.PlatformUsageStatistics;
import com.deepreach.web.entity.dto.CharacterUsageStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI实例Mapper接口
 *
 * 负责AI实例相关的数据库操作，包括：
 * 1. 实例基本信息CRUD操作
 * 2. 实例类型和平台关联查询
 * 3. 用户实例关联查询
 * 4. 实例统计和管理查询
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Mapper
public interface AiInstanceMapper {

    /**
     * 根据实例ID查询实例信息
     *
     * 查询实例的基本信息，包括名称、类型、绑定信息等
     * 不包含用户详细信息，仅用于基本的信息获取
     *
     * @param instanceId 实例ID
     * @return 实例实体对象，如果不存在则返回null
     */
    AiInstance selectById(@Param("instanceId") Long instanceId);

    /**
     * 根据用户ID查询实例列表
     *
     * 查询指定用户创建的所有实例
     * 按创建时间倒序排列
     *
     * @param userId 用户ID
     * @return 实例列表
     */
    List<AiInstance> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID列表批量查询实例
     *
     * 查询多个用户创建的实例集合
     *
     * @param userIds 用户ID列表
     * @return 实例列表
     */
    List<AiInstance> selectByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 根据实例类型查询实例列表
     *
     * 查询指定类型的所有实例
     *
     * @param instanceType 实例类型（0营销 1拓客）
     * @return 实例列表
     */
    List<AiInstance> selectByInstanceType(@Param("instanceType") String instanceType);

    /**
     * 根据平台ID查询实例列表
     *
     * 查询指定平台的所有实例
     *
     * @param platformId 平台ID
     * @return 实例列表
     */
    List<AiInstance> selectByPlatformId(@Param("platformId") Integer platformId);

    /**
     * 根据人设ID查询实例列表
     *
     * 查询使用了指定AI人设的所有实例
     *
     * @param characterId AI人设ID
     * @return 实例列表
     */
    List<AiInstance> selectByCharacterId(@Param("characterId") Integer characterId);

    /**
     * 根据条件查询实例列表
     *
     * 支持多条件查询，包括实例名称、类型、平台、用户等
     *
     * @param instance 查询条件对象
     * @return 实例列表，按创建时间倒序排列
     */
    List<AiInstance> selectList(AiInstance instance);

    /**
     * 根据条件查询实例列表（带用户权限控制）
     *
     * 支持多条件查询，自动应用权限过滤
     * 权限限制：只能查询当前用户创建的实例
     *
     * @param instance 查询条件对象
     * @param currentUserId 当前用户ID
     * @return 实例列表，按创建时间倒序排列
     */
    List<AiInstance> selectListWithUserPermission(@Param("instance") AiInstance instance,
                                                   @Param("currentUserId") Long currentUserId);

    /**
     * 根据条件查询实例列表（带平台名称和人设名称，用户权限控制）
     *
     * 支持多条件查询，自动应用权限过滤
     * 返回包含平台名称和人设名称的VO对象
     * 权限限制：只能查询当前用户创建的实例
     *
     * @param instance 查询条件对象
     * @param currentUserId 当前用户ID
     * @return 实例VO列表，按创建时间倒序排列
     */
    List<com.deepreach.web.domain.vo.AiInstanceVO> selectListVOWithUserPermission(@Param("instance") AiInstance instance,
                                                                                @Param("currentUserId") Long currentUserId);

    /**
     * 查询实例总数
     *
     * 用于后台统计功能，统计符合条件的实例数量
     *
     * @param instance 查询条件对象
     * @return 实例总数
     */
    int countInstances(AiInstance instance);

    /**
     * 统计用户的实例数量
     *
     * 统计指定用户创建的实例数量
     *
     * @param userId 用户ID
     * @return 实例数量
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 统计指定类型的实例数量
     *
     * @param instanceType 实例类型
     * @return 实例数量
     */
    int countByInstanceType(@Param("instanceType") String instanceType);

    /**
     * 统计指定平台的实例数量
     *
     * @param platformId 平台ID
     * @return 实例数量
     */
    int countByPlatformId(@Param("platformId") Integer platformId);

    /**
     * 插入新实例
     *
     * 创建新实例记录，包含基本信息
     * 创建时间由数据库自动设置
     *
     * @param instance 实例对象，包含必要的基本信息
     * @return 成功插入的记录数，通常为1
     */
    int insert(AiInstance instance);

    /**
     * 更新实例信息
     *
     * 更新实例的基本信息，不包括创建时间
     * 更新时间由数据库自动更新
     *
     * @param instance 实例对象，包含要更新的信息
     * @return 成功更新的记录数
     */
    int update(AiInstance instance);

    /**
     * 删除实例
     *
     * 根据实例ID删除实例记录
     * 操作不可逆，请谨慎使用
     *
     * @param instanceId 实例ID
     * @return 成功删除的记录数
     */
    int deleteById(@Param("instanceId") Long instanceId);

    /**
     * 批量删除实例
     *
     * 根据实例ID列表批量删除实例
     * 用于批量管理功能，提高操作效率
     *
     * @param instanceIds 实例ID列表
     * @return 成功删除的记录数
     */
    int deleteByIds(@Param("instanceIds") List<Long> instanceIds);

    /**
     * 检查实例名称是否唯一
     *
     * 用于实例创建和修改时的唯一性验证
     * 同一用户下实例名称不能重复
     *
     * @param instanceName 实例名称
     * @param userId 用户ID
     * @param instanceId 排除的实例ID（用于更新验证）
     * @return 存在相同名称的记录数，0表示唯一
     */
    int checkInstanceNameUnique(@Param("instanceName") String instanceName,
                                @Param("userId") Long userId,
                                @Param("instanceId") Long instanceId);

    /**
     * 检查用户是否可以删除实例
     *
     * 验证用户是否为实例的创建者
     *
     * @param instanceId 实例ID
     * @param userId 用户ID
     * @return 是否可以删除
     */
    boolean canDelete(@Param("instanceId") Long instanceId, @Param("userId") Long userId);

    /**
     * 查询最新实例列表
     *
     * 查询最新创建的实例
     * 用于发现和管理
     *
     * @param limit 限制数量
     * @return 最新实例列表
     */
    List<AiInstance> selectLatestInstances(@Param("limit") Integer limit);

    /**
     * 查询实例的完整信息
     *
     * 获取实例的完整信息，包括创建者信息
     * 用于实例详情展示
     *
     * @param instanceId 实例ID
     * @return 实例完整信息对象
     */
    AiInstance selectCompleteInfo(@Param("instanceId") Long instanceId);

    /**
     * 更新实例绑定的人设
     *
     * 专门用于更新实例的AI人设绑定
     *
     * @param instanceId 实例ID
     * @param characterId AI人设ID
     * @return 成功更新的记录数
     */
    int updateCharacterId(@Param("instanceId") Long instanceId, @Param("characterId") Integer characterId);

    /**
     * 更新实例代理ID
     *
     * 专门用于更新实例的代理绑定
     *
     * @param instanceId 实例ID
     * @param proxyId 代理ID
     * @return 成功更新的记录数
     */
    int updateProxyId(@Param("instanceId") Long instanceId, @Param("proxyId") Integer proxyId);

    /**
     * 更新实例平台绑定
     *
     * 专门用于更新实例的平台绑定
     *
     * @param instanceId 实例ID
     * @param platformId 平台ID
     * @return 成功更新的记录数
     */
    int updatePlatformId(@Param("instanceId") Long instanceId, @Param("platformId") Integer platformId);

    /**
     * 查询指定时间范围内的实例
     *
     * 用于统计和分析
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param userId 用户ID（可选）
     * @return 实例列表
     */
    List<AiInstance> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       @Param("userId") Long userId);

    /**
     * 搜索实例
     *
     * 根据关键词搜索实例
     * 搜索范围：实例名称、代理地址
     *
     * @param keyword 关键词
     * @param userId 用户ID（可选，限制搜索范围）
     * @param limit 限制数量
     * @return 搜索结果
     */
    List<AiInstance> searchInstances(@Param("keyword") String keyword,
                                     @Param("userId") Long userId,
                                     @Param("limit") Integer limit);

    /**
     * 获取实例统计信息
     *
     * 获取实例的统计数据
     *
     * @return 统计信息Map

    java.util.Map<String, Object> getInstanceStatistics();
     */

    /**
     * 获取用户的实例统计信息
     *
     * 获取指定用户的实例统计数据
     *
     * @param userId 用户ID
     * @return 统计信息Map

    java.util.Map<String, Object> getUserInstanceStatistics(@Param("userId") Long userId);
     */

    /**
     * 检查实例是否存在
     *
     * @param instanceId 实例ID
     * @return 是否存在
     */
    boolean existsById(@Param("instanceId") Long instanceId);

    /**
     * 检查用户是否有权限访问实例
     *
     * @param instanceId 实例ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean hasAccessPermission(@Param("instanceId") Long instanceId, @Param("userId") Long userId);

    /**
     * 查询用户可访问的实例列表
     *
     * 查询用户可以访问的所有实例
     * 目前只查询用户自己创建的实例
     *
     * @param userId 用户ID
     * @return 可访问的实例列表
     */
    List<AiInstance> selectAccessibleInstances(@Param("userId") Long userId);

    /**
     * 获取实例类型统计
     *
     * 按类型统计实例数量
     *
     * @param userId 用户ID（可选，限制统计范围）
     * @return 类型统计信息列表
     */
    List<InstanceTypeStatistics> getInstanceTypeStatistics(@Param("userId") Long userId);

    /**
     * 获取平台使用统计
     *
     * 按平台统计实例数量
     *
     * @param userId 用户ID（可选，限制统计范围）
     * @return 平台统计信息列表
     */
    List<PlatformUsageStatistics> getPlatformUsageStatistics(@Param("userId") Long userId);

    /**
     * 获取人设使用统计
     *
     * 按AI人设统计实例数量
     *
     * @param userId 用户ID（可选，限制统计范围）
     * @return 人设统计信息列表
     */
    List<CharacterUsageStatistics> getCharacterUsageStatistics(@Param("userId") Long userId);

    /**
     * 查询未绑定人设的实例
     *
     * @param userId 用户ID（可选）
     * @return 未绑定人设的实例列表
     */
    List<AiInstance> selectUnboundCharacterInstances(@Param("userId") Long userId);

    /**
     * 查询配置了代理的实例
     *
     * @param userId 用户ID（可选）
     * @return 配置了代理的实例列表
     */
    List<AiInstance> selectWithProxyInstances(@Param("userId") Long userId);

    /**
     * 批量更新实例状态
     *
     * 批量更新实例的平台或人设绑定
     *
     * @param instanceIds 实例ID列表
     * @param platformId 新的平台ID（可选）
     * @param characterId 新的人设ID（可选）
     * @return 成功更新的记录数
     */
    int batchUpdateInstanceConfig(@Param("instanceIds") List<Long> instanceIds,
                                 @Param("platformId") Integer platformId,
                                 @Param("characterId") Integer characterId);

    /**
     * 获取实例活动统计
     *
     * 获取实例最近的活动统计
     *
     * @param days 统计天数
     * @param userId 用户ID（可选）
     * @return 活动统计信息

    java.util.Map<String, Object> getInstanceActivityStatistics(@Param("days") Integer days,
                                                                @Param("userId") Long userId);
     */

    /**
     * 查询所有实例（用于计费任务）
     *
     * 获取所有实例信息，转换为Instance实体类格式
     * 用于定时计费任务统计实例数量
     *
     * @return 所有实例列表
     */
    List<Instance> selectAllInstances();

    /**
     * 根据用户ID、实例类型和平台ID查询实例列表
     *
     * 查询指定用户下特定类型和平台的实例
     * 用于拓客实例创建时的数量限制检查
     *
     * @param params 包含userId、instanceType、platformId的参数Map
     * @return 实例列表
     */
    List<AiInstance> selectByUserIdAndTypeAndPlatform(@Param("params") Map<String, Object> params);

    /**
     * 根据用户ID和实例类型查询实例列表
     *
     * 查询指定用户下特定类型的所有实例（不限制平台）
     * 用于拓客实例创建时的全局营销实例数量统计
     *
     * @param params 包含userId、instanceType的参数Map
     * @return 实例列表
     */
    List<AiInstance> selectByUserIdAndType(@Param("params") Map<String, Object> params);
}
