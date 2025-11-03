package com.deepreach.web.service;

import com.deepreach.web.entity.AiInstance;
import com.deepreach.web.entity.dto.InstanceTypeStatistics;
import com.deepreach.web.entity.dto.PlatformUsageStatistics;
import com.deepreach.web.entity.dto.CharacterUsageStatistics;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * AI实例Service接口
 *
 * 负责AI实例相关的业务逻辑，包括：
 * 1. 实例基本信息管理（增删改查）
 * 2. 实例类型和平台管理
 * 3. 实例权限控制
 * 4. 实例搜索和推荐
 * 5. 实例统计和分析
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
public interface AiInstanceService {

    /**
     * 根据实例ID查询实例
     *
     * 获取实例的基本信息，不包含敏感信息
     * 用于实例信息展示和基本信息管理
     *
     * @param instanceId 实例ID
     * @return 实例对象，如果不存在则返回null
     */
    AiInstance selectById(Long instanceId);

    /**
     * 查询实例的完整信息
     *
     * 获取实例的完整信息，包括创建者等关联信息
     * 用于实例详情展示和权限验证
     *
     * @param instanceId 实例ID
     * @return 包含完整关联信息的实例对象，如果不存在则返回null
     */
    AiInstance selectCompleteInfo(Long instanceId);

    /**
     * 根据实例ID查询实例的完整信息（包含平台名称和人设名称）
     *
     * 获取实例的完整信息，包括平台名称、人设名称等关联信息
     * 用于实例详情展示
     *
     * @param instanceId 实例ID
     * @param currentUserId 当前用户ID（用于权限验证）
     * @return 包含完整关联信息的实例VO对象，如果不存在则返回null
     */
    com.deepreach.web.domain.vo.AiInstanceVO selectCompleteInfoVO(Long instanceId, Long currentUserId);

    /**
     * 根据用户ID查询实例列表
     *
     * 查询指定用户创建的所有实例
     * 用于用户个人实例管理
     *
     * @param userId 用户ID
     * @return 实例列表
     */
    List<AiInstance> selectByUserId(Long userId);

    /**
     * 根据用户ID查询实例列表（包含平台名称和人设名称）
     *
     * 查询指定用户创建的所有实例，包含平台名称和人设名称
     * 用于用户个人实例管理
     *
     * @param userId 用户ID
     * @return 实例VO列表
     */
    List<com.deepreach.web.domain.vo.AiInstanceVO> selectByUserIdVO(Long userId);

    /**
     * 根据条件查询实例列表
     *
     * 支持多条件查询，包括实例名称、类型、平台等
     * 自动应用权限过滤，确保只能查看有权限的实例
     *
     * @param instance 查询条件对象
     * @return 实例列表
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
     * @return 实例列表
     */
    List<AiInstance> selectListWithUserPermission(AiInstance instance, Long currentUserId);

    /**
     * 根据条件查询实例列表（带平台名称和人设名称，用户权限控制）
     *
     * 支持多条件查询，自动应用权限过滤
     * 返回包含平台名称和人设名称的VO对象
     * 权限限制：只能查询当前用户创建的实例
     *
     * @param instance 查询条件对象
     * @param currentUserId 当前用户ID
     * @return 实例VO列表
     */
    List<com.deepreach.web.domain.vo.AiInstanceVO> selectListVOWithUserPermission(AiInstance instance, Long currentUserId);

    /**
     * 创建新实例
     *
     * 创建新的实例，包含完整的业务逻辑：
     * 1. 参数验证和唯一性检查
     * 2. 权限验证
     * 3. 创建记录和日志记录
     *
     * @param instance 实例对象，包含必要信息
     * @return 创建成功后的实例对象，包含生成的ID
     * @throws Exception 当参数验证失败或数据冲突时抛出异常
     */
    AiInstance insert(AiInstance instance) throws Exception;

    /**
     * 更新实例信息
     *
     * 更新实例的基本信息
     * 包含参数验证和权限检查
     *
     * @param instance 实例对象，包含要更新的信息
     * @return 是否更新成功
     * @throws Exception 当参数验证失败或无权限时抛出异常
     */
    boolean update(AiInstance instance) throws Exception;

    /**
     * 删除实例
     *
     * 根据实例ID删除实例记录
     * 包含依赖检查和权限验证
     *
     * @param instanceId 实例ID
     * @return 是否删除成功
     * @throws Exception 当实例不存在或有依赖关系时抛出异常
     */
    boolean deleteById(Long instanceId) throws Exception;

    /**
     * 批量删除实例
     *
     * 根据实例ID列表批量删除实例
     * 包含批量依赖检查和权限验证
     *
     * @param instanceIds 实例ID列表
     * @return 是否删除成功
     * @throws Exception 当有实例不存在或有依赖关系时抛出异常
     */
    boolean deleteByIds(List<Long> instanceIds) throws Exception;

    /**
     * 根据实例类型查询实例列表
     *
     * @param instanceType 实例类型（0营销 1拓客）
     * @return 实例列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiInstance> selectByInstanceType(String instanceType) throws Exception;

    /**
     * 根据平台ID查询实例列表
     *
     * @param platformId 平台ID
     * @return 实例列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiInstance> selectByPlatformId(Integer platformId) throws Exception;

    /**
     * 根据人设ID查询实例列表
     *
     * @param characterId AI人设ID
     * @return 实例列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiInstance> selectByCharacterId(Integer characterId) throws Exception;

    /**
     * 查询用户可访问的实例列表
     *
     * 查询用户可以访问的所有实例
     * 目前只查询用户自己创建的实例
     *
     * @param userId 用户ID
     * @return 可访问的实例列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiInstance> selectAccessibleInstances(Long userId) throws Exception;

    /**
     * 查询用户可访问的实例列表（VO版本）
     *
     * 查询用户可以访问的所有实例，包含平台名称和人设名称
     * 目前只查询用户自己创建的实例
     *
     * @param userId 用户ID
     * @return 可访问的实例VO列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<com.deepreach.web.domain.vo.AiInstanceVO> selectAccessibleInstancesVO(Long userId) throws Exception;

    /**
     * 检查实例名称是否唯一
     *
     * 用于实例创建和修改时的唯一性验证
     *
     * @param instanceName 实例名称
     * @param userId 用户ID
     * @param instanceId 排除的实例ID（用于更新验证）
     * @return true如果唯一，false如果已存在
     */
    boolean checkInstanceNameUnique(String instanceName, Long userId, Long instanceId);

    /**
     * 更新实例绑定的人设
     *
     * 更新实例的AI人设绑定
     *
     * @param instanceId 实例ID
     * @param characterId AI人设ID
     * @return 是否更新成功
     * @throws Exception 当参数验证失败时抛出异常
     */
    boolean updateCharacterId(Long instanceId, Integer characterId) throws Exception;

    /**
     * 更新实例代理ID
     *
     * 更新实例的代理绑定
     *
     * @param instanceId 实例ID
     * @param proxyId 代理ID
     * @return 是否更新成功
     * @throws Exception 当参数验证失败时抛出异常
     */
    boolean updateProxyId(Long instanceId, Integer proxyId) throws Exception;

    /**
     * 更新实例平台绑定
     *
     * 更新实例的平台绑定
     *
     * @param instanceId 实例ID
     * @param platformId 平台ID
     * @return 是否更新成功
     * @throws Exception 当参数验证失败时抛出异常
     */
    boolean updatePlatformId(Long instanceId, Integer platformId) throws Exception;

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
     * @throws Exception 当搜索失败时抛出异常
     */
    List<AiInstance> searchInstances(String keyword, Long userId, Integer limit) throws Exception;

    /**
     * 查询最新实例列表
     *
     * 查询最新创建的实例
     * 用于发现和管理
     *
     * @param limit 限制数量
     * @return 最新实例列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiInstance> selectLatestInstances(Integer limit) throws Exception;

//    /**
//     * 获取实例统计信息
//     *
//     * 获取实例相关的统计数据
//     * 用于管理界面的统计展示
//     *
//     * @return 统计信息Map
//     * @throws Exception 当统计失败时抛出异常
//     */
//    Map<String, Object> getStatistics() throws Exception;

//    /**
//     * 获取用户的实例统计信息
//     *
//     * 获取指定用户的实例统计数据
//     *
//     * @param userId 用户ID
//     * @return 统计信息Map
//     * @throws Exception 当统计失败时抛出异常
//     */
//    Map<String, Object> getUserStatistics(Long userId) throws Exception;

    /**
     * 检查用户是否有权限访问实例
     *
     * 验证当前用户是否有权限访问指定实例
     * 用于权限控制
     *
     * @param instanceId 实例ID
     * @param userId 用户ID
     * @return true如果有权限，false否则
     */
    boolean hasAccessPermission(Long instanceId, Long userId);

    /**
     * 检查用户是否可以删除实例
     *
     * 验证用户是否有权限删除指定实例
     *
     * @param instanceId 实例ID
     * @param userId 用户ID
     * @return true如果可以删除，false否则
     */
    boolean canDelete(Long instanceId, Long userId);

    /**
     * 创建营销类型实例
     *
     * 创建营销推广类型的实例
     *
     * @param instance 实例对象
     * @param userId 创建者用户ID
     * @return 创建成功后的实例对象
     * @throws Exception 当创建失败时抛出异常
     */
    AiInstance createMarketingInstance(AiInstance instance, Long userId) throws Exception;

    /**
     * 创建拓客类型实例
     *
     * 创建客户拓展类型的实例
     *
     * @param instance 实例对象
     * @param userId 创建者用户ID
     * @return 创建成功后的实例对象
     * @throws Exception 当创建失败时抛出异常
     */
    AiInstance createProspectingInstance(AiInstance instance, Long userId) throws Exception;

    /**
     * 验证实例数据
     *
     * 验证实例数据的完整性和有效性
     *
     * @param instance 实例对象
     * @return 验证结果
     */
    boolean validateInstance(AiInstance instance);

    /**
     * 复制实例
     *
     * 基于现有实例创建副本
     * 用于快速创建相似配置的实例
     *
     * @param sourceId 源实例ID
     * @param newInstanceName 新实例名称
     * @param userId 目标用户ID
     * @return 复制成功后的实例对象
     * @throws Exception 当复制失败时抛出异常
     */
    AiInstance copyInstance(Long sourceId, String newInstanceName, Long userId) throws Exception;

    /**
     * 获取实例类型统计
     *
     * 获取按类型分类的实例统计信息
     *
     * @param userId 用户ID（可选，限制统计范围）
     * @return 类型统计信息列表
     * @throws Exception 当统计失败时抛出异常
     */
    List<InstanceTypeStatistics> getTypeStatistics(Long userId) throws Exception;

    /**
     * 获取平台使用统计
     *
     * 获取按平台分类的实例使用统计
     *
     * @param userId 用户ID（可选，限制统计范围）
     * @return 平台统计信息列表
     * @throws Exception 当统计失败时抛出异常
     */
    List<PlatformUsageStatistics> getPlatformUsageStatistics(Long userId) throws Exception;

    /**
     * 获取人设使用统计
     *
     * 获取按AI人设分类的实例使用统计
     *
     * @param userId 用户ID（可选，限制统计范围）
     * @return 人设统计信息列表
     * @throws Exception 当统计失败时抛出异常
     */
    List<CharacterUsageStatistics> getCharacterUsageStatistics(Long userId) throws Exception;

    /**
     * 查询未绑定人设的实例
     *
     * @param userId 用户ID（可选）
     * @return 未绑定人设的实例列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiInstance> selectUnboundCharacterInstances(Long userId) throws Exception;

    /**
     * 查询配置了代理的实例
     *
     * @param userId 用户ID（可选）
     * @return 配置了代理的实例列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<AiInstance> selectWithProxyInstances(Long userId) throws Exception;

    /**
     * 批量更新实例配置
     *
     * 批量更新实例的平台或人设绑定
     *
     * @param instanceIds 实例ID列表
     * @param platformId 新的平台ID（可选）
     * @param characterId 新的人设ID（可选）
     * @return 是否更新成功
     * @throws Exception 当更新失败时抛出异常
     */
    boolean batchUpdateInstanceConfig(List<Long> instanceIds, Integer platformId, Integer characterId) throws Exception;

//    /**
//     * 获取实例活动统计
//     *
//     * 获取实例最近的活动统计
//     *
//     * @param days 统计天数
//     * @param userId 用户ID（可选）
//     * @return 活动统计信息
//     * @throws Exception 当统计失败时抛出异常
//     */
//    Map<String, Object> getActivityStatistics(Integer days, Long userId) throws Exception;

    /**
     * 导入实例数据
     *
     * 批量导入实例数据
     * 支持JSON等格式的批量导入
     *
     * @param instances 实例列表
     * @param updateSupport 是否支持更新已存在的实例
     * @return 导入结果，包含成功和失败信息
     * @throws Exception 当导入过程中发生错误时抛出异常
     */
    Map<String, Object> importInstances(List<AiInstance> instances, boolean updateSupport) throws Exception;

    /**
     * 导出实例数据
     *
     * 导出实例数据为指定格式
     * 支持JSON等格式的数据导出
     *
     * @param instances 实例列表
     * @return 导出数据的字符串
     * @throws Exception 当导出过程中发生错误时抛出异常
     */
    String exportInstances(List<AiInstance> instances) throws Exception;

    /**
     * 获取实例配置完整度分析
     *
     * 分析实例配置的完整情况
     *
     * @param userId 用户ID（可选）
     * @return 配置完整度分析结果
     * @throws Exception 当分析失败时抛出异常
     */
    Map<String, Object> getConfigurationAnalysis(Long userId) throws Exception;

    /**
     * 检查实例是否可以运行
     *
     * 验证实例是否具备运行的基本条件
     *
     * @param instanceId 实例ID
     * @return 是否可以运行
     * @throws Exception 当检查失败时抛出异常
     */
    boolean canRun(Long instanceId) throws Exception;

    /**
     * 获取实例运行状态
     *
     * 获取实例的运行状态信息
     * 包括配置状态、绑定状态等
     *
     * @param instanceId 实例ID
     * @return 运行状态信息
     * @throws Exception 当获取失败时抛出异常
     */
    Map<String, Object> getInstanceStatus(Long instanceId) throws Exception;

    /**
     * 根据用户ID、实例类型和平台ID查询实例列表
     *
     * 查询指定用户下特定类型和平台的实例
     * 用于拓客实例创建时的数量限制检查
     *
     * @param userId 用户ID
     * @param instanceType 实例类型（"0"营销，"1"拓客）
     * @param platformId 平台ID
     * @return 实例列表
     */
    List<AiInstance> selectByUserIdAndTypeAndPlatform(Long userId, String instanceType, Integer platformId);

    /**
     * 创建实例并进行完整验证
     *
     * 包含用户权限验证、余额检查、数量限制检查等业务逻辑
     * 创建成功后自动扣除当天费用
     * 使用事务管理确保数据一致性
     *
     * @param instance 实例对象
     * @param currentUserId 当前用户ID
     * @return 创建的实例对象
     * @throws IllegalArgumentException 当验证失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    AiInstance createInstanceWithValidation(AiInstance instance, Long currentUserId);

    /**
     * 根据用户ID和实例类型查询实例列表
     *
     * 查询指定用户下特定类型的所有实例（不限制平台）
     * 用于拓客实例创建时的全局营销实例数量统计
     *
     * @param userId 用户ID
     * @param instanceType 实例类型（"0"营销，"1"拓客）
     * @return 实例列表
     */
    List<AiInstance> selectByUserIdAndType(Long userId, String instanceType);
}