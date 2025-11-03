package com.deepreach.web.mapper;

import com.deepreach.common.core.domain.entity.Instance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实例Mapper接口
 *
 * 负责实例相关的数据库操作，包括：
 * 1. 实例的CRUD操作
 * 2. 实例状态管理
 * 3. 实例计费相关操作
 * 4. 实例统计和分析
 * 5. 实例与用户、平台的关联查询
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface InstanceMapper {

    /**
     * 根据ID查询实例
     *
     * @param instanceId 实例ID
     * @return 实例对象，如果不存在则返回null
     */
    Instance selectById(@Param("instanceId") Long instanceId);

    /**
     * 根据实例名称查询实例
     *
     * @param instanceName 实例名称
     * @return 实例对象，如果不存在则返回null
     */
    Instance selectByName(@Param("instanceName") String instanceName);

    /**
     * 根据用户ID查询实例列表
     *
     * @param userId 用户ID
     * @return 实例列表
     */
    List<Instance> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据平台查询实例列表
     *
     * @param platform 平台类型（1: Facebook, 2: Google, 3: TikTok）
     * @return 实例列表
     */
    List<Instance> selectByPlatform(@Param("platform") Integer platform);

    /**
     * 根据状态查询实例列表
     *
     * @param status 实例状态
     * @return 实例列表
     */
    List<Instance> selectByStatus(@Param("status") Integer status);

    /**
     * 根据类型查询实例列表
     *
     * @param type 实例类型（1: 营销实例, 2: 侦查实例）
     * @return 实例列表
     */
    List<Instance> selectByType(@Param("type") Integer type);

    /**
     * 查询指定用户的营销实例
     *
     * @param userId 用户ID
     * @return 营销实例列表
     */
    List<Instance> selectMarketingInstancesByUserId(@Param("userId") Long userId);

    /**
     * 查询指定用户的侦查实例
     *
     * @param userId 用户ID
     * @return 侦查实例列表
     */
    List<Instance> selectProspectingInstancesByUserId(@Param("userId") Long userId);

    /**
     * 查询所有实例列表
     *
     * @return 实例列表，按创建时间倒序排列
     */
    List<Instance> selectAll();

    /**
     * 根据条件查询实例列表
     *
     * @param instance 查询条件对象
     * @return 符合条件的实例列表
     */
    List<Instance> selectList(Instance instance);

    /**
     * 查询指定时间范围内创建的实例
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间范围内的实例列表
     */
    List<Instance> selectByCreateTimeRange(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 查询需要续费的实例
     *
     * @param days 距离到期的天数
     * @return 即将到期的实例列表
     */
    List<Instance> selectInstancesNeedingRenewal(@Param("days") Integer days);

    /**
     * 查询异常状态的实例
     *
     * @return 异常实例列表
     */
    List<Instance> selectAbnormalInstances();

    /**
     * 查询活跃实例
     *
     * @return 活跃实例列表
     */
    List<Instance> selectActiveInstances();

    /**
     * 插入新实例
     *
     * @param instance 实例对象
     * @return 成功插入的记录数
     */
    int insert(Instance instance);

    /**
     * 批量插入实例
     *
     * @param instances 实例列表
     * @return 成功插入的记录数
     */
    int batchInsert(@Param("instances") List<Instance> instances);

    /**
     * 更新实例信息
     *
     * @param instance 实例对象
     * @return 成功更新的记录数
     */
    int update(Instance instance);

    /**
     * 更新实例状态
     *
     * @param instanceId 实例ID
     * @param status 新状态
     * @return 成功更新的记录数
     */
    int updateStatus(@Param("instanceId") Long instanceId, @Param("status") Integer status);

    /**
     * 启动实例
     *
     * @param instanceId 实例ID
     * @return 成功更新的记录数
     */
    int startInstance(@Param("instanceId") Long instanceId);

    /**
     * 停止实例
     *
     * @param instanceId 实例ID
     * @return 成功更新的记录数
     */
    int stopInstance(@Param("instanceId") Long instanceId);

    /**
     * 重启实例
     *
     * @param instanceId 实例ID
     * @return 成功更新的记录数
     */
    int restartInstance(@Param("instanceId") Long instanceId);

    /**
     * 删除实例
     *
     * @param instanceId 实例ID
     * @return 成功删除的记录数
     */
    int deleteById(@Param("instanceId") Long instanceId);

    /**
     * 批量删除实例
     *
     * @param instanceIds 实例ID列表
     * @return 成功删除的记录数
     */
    int deleteByIds(@Param("instanceIds") List<Long> instanceIds);

    /**
     * 统计实例总数
     *
     * @return 实例总数
     */
    int countAll();

    /**
     * 统计指定用户的实例数量
     *
     * @param userId 用户ID
     * @return 实例数量
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 统计指定平台的实例数量
     *
     * @param platform 平台类型
     * @return 实例数量
     */
    int countByPlatform(@Param("platform") Integer platform);

    /**
     * 统计指定状态的实例数量
     *
     * @param status 实例状态
     * @return 实例数量
     */
    int countByStatus(@Param("status") Integer status);

    /**
     * 统计指定类型的实例数量
     *
     * @param type 实例类型
     * @return 实例数量
     */
    int countByType(@Param("type") Integer type);

    /**
     * 统计活跃实例数量
     *
     * @return 活跃实例数量
     */
    int countActiveInstances();

    /**
     * 统计用户营销实例数量
     *
     * @param userId 用户ID
     * @return 营销实例数量
     */
    int countMarketingInstancesByUserId(@Param("userId") Long userId);

    /**
     * 统计用户侦查实例数量
     *
     * @param userId 用户ID
     * @return 侦查实例数量
     */
    int countProspectingInstancesByUserId(@Param("userId") Long userId);

    /**
     * 获取实例的累计计费天数
     *
     * @param instanceId 实例ID
     * @return 累计计费天数
     */
    Integer getBilledDays(@Param("instanceId") Long instanceId);

    /**
     * 获取实例的累计计费金额
     *
     * @param instanceId 实例ID
     * @return 累计计费金额
     */
    BigDecimal getBilledAmount(@Param("instanceId") Long instanceId);

    /**
     * 更新实例计费信息
     *
     * @param instanceId 实例ID
     * @param billedDays 计费天数
     * @param billedAmount 计费金额
     * @return 成功更新的记录数
     */
    int updateBillingInfo(@Param("instanceId") Long instanceId,
                         @Param("billedDays") Integer billedDays,
                         @Param("billedAmount") BigDecimal billedAmount);

    /**
     * 增加实例计费天数
     *
     * @param instanceId 实例ID
     * @param days 增加的天数
     * @return 成功更新的记录数
     */
    int addBilledDays(@Param("instanceId") Long instanceId, @Param("days") Integer days);

    /**
     * 增加实例计费金额
     *
     * @param instanceId 实例ID
     * @param amount 增加的金额
     * @return 成功更新的记录数
     */
    int addBilledAmount(@Param("instanceId") Long instanceId, @Param("amount") BigDecimal amount);

    /**
     * 检查实例名称是否唯一
     *
     * @param instanceName 实例名称
     * @param instanceId 排除的实例ID（用于更新时验证）
     * @return 存在相同名称的记录数，0表示唯一
     */
    int checkInstanceNameUnique(@Param("instanceName") String instanceName, @Param("instanceId") Long instanceId);

    /**
     * 检查用户是否可以创建更多实例
     *
     * @param userId 用户ID
     * @param maxLimit 最大限制数量
     * @return 当前实例数量
     */
    int checkUserInstanceLimit(@Param("userId") Long userId, @Param("maxLimit") Integer maxLimit);

    /**
     * 获取用户实例统计信息
     *
     * @param userId 用户ID
     * @return 实例统计信息
     */
    Instance getInstanceStatistics(@Param("userId") Long userId);

    /**
     * 获取平台实例统计信息
     *
     * @param platform 平台类型
     * @return 实例统计信息
     */
    Instance getPlatformStatistics(@Param("platform") Integer platform);

    /**
     * 获取实例每日统计
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日统计列表
     */
    List<Instance> getDailyStatistics(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * 获取实例使用时长排行
     *
     * @param limit 排行榜长度
     * @return 使用时长排行
     */
    List<Instance> getInstanceUsageRanking(@Param("limit") Integer limit);

    /**
     * 获取实例消费排行
     *
     * @param limit 排行榜长度
     * @return 消费排行
     */
    List<Instance> getInstanceConsumptionRanking(@Param("limit") Integer limit);

    /**
     * 同步实例信息到缓存
     *
     * @param instanceId 实例ID
     * @return 同步结果
     */
    boolean syncToCache(@Param("instanceId") Long instanceId);

    /**
     * 从缓存刷新实例信息
     *
     * @param userId 用户ID
     * @return 刷新结果
     */
    boolean refreshFromCache(@Param("userId") Long userId);

    /**
     * 获取实例关联的计费记录
     *
     * @param instanceId 实例ID
     * @return 关联的计费记录列表
     */
    List<Instance> getRelatedBillingRecords(@Param("instanceId") Long instanceId);

    /**
     * 获取实例的创建价格
     *
     * @param instanceId 实例ID
     * @return 创建时的价格
     */
    BigDecimal getInstanceCreationPrice(@Param("instanceId") Long instanceId);

    /**
     * 检查实例是否需要计费
     *
     * @param instanceId 实例ID
     * @return true如果需要计费，false否则
     */
    boolean checkInstanceNeedsBilling(@Param("instanceId") Long instanceId);

    /**
     * 获取实例下次计费时间
     *
     * @param instanceId 实例ID
     * @return 下次计费时间
     */
    LocalDateTime getNextBillingTime(@Param("instanceId") Long instanceId);

    /**
     * 更新实例最后活跃时间
     *
     * @param instanceId 实例ID
     * @param activeTime 活跃时间
     * @return 成功更新的记录数
     */
    int updateLastActiveTime(@Param("instanceId") Long instanceId, @Param("activeTime") LocalDateTime activeTime);

    /**
     * 批量更新实例状态
     *
     * @param instanceIds 实例ID列表
     * @param status 新状态
     * @return 成功更新的记录数
     */
    int batchUpdateStatus(@Param("instanceIds") List<Long> instanceIds, @Param("status") Integer status);

    /**
     * 获取待处理的实例任务
     *
     * @return 待处理的实例列表
     */
    List<Instance> getPendingInstanceTasks();

    /**
     * 处理实例任务
     *
     * @param instanceId 实例ID
     * @param taskResult 任务结果
     * @return 处理结果
     */
    int processInstanceTask(@Param("instanceId") Long instanceId, @Param("taskResult") String taskResult);

    /**
     * 验证实例数据完整性
     *
     * @param instanceId 实例ID
     * @return 验证结果
     */
    boolean validateInstanceData(@Param("instanceId") Long instanceId);

    /**
     * 修复实例数据
     *
     * @param instanceId 实例ID
     * @return 修复结果
     */
    boolean repairInstanceData(@Param("instanceId") Long instanceId);

    /**
     * 获取实例性能指标
     *
     * @param instanceId 实例ID
     * @return 性能指标信息
     */
    Instance getInstancePerformanceMetrics(@Param("instanceId") Long instanceId);

    /**
     * 导出实例数据
     *
     * @param instance 查询条件
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 导出的实例列表
     */
    List<Instance> exportInstances(@Param("instance") Instance instance,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询实例的完整信息（包含用户和平台信息）
     *
     * @param instanceId 实例ID
     * @return 完整的实例信息
     */
    Instance selectCompleteInstanceInfo(@Param("instanceId") Long instanceId);

    /**
     * 查询用户实例的完整信息（包含平台和计费信息）
     *
     * @param userId 用户ID
     * @return 用户实例完整信息列表
     */
    List<Instance> selectCompleteInstancesByUserId(@Param("userId") Long userId);
}