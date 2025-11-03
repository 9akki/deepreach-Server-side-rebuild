package com.deepreach.web.service;

import com.deepreach.web.domain.dto.BatchDeleteResultDTO;
import com.deepreach.web.entity.Proxy;

import java.util.List;
import java.util.Map;

/**
 * 代理池Service接口
 *
 * 负责代理配置相关的业务逻辑，包括：
 * 1. 代理配置管理（增删改查）
 * 2. 代理配置验证和测试
 * 3. 用户代理权限管理
 * 4. 代理状态管理和监控
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
public interface ProxyService {

    /**
     * 根据代理ID查询代理配置
     *
     * 获取代理的完整配置信息
     * 包含权限验证，确保用户只能查看自己的代理配置
     *
     * @param proxyId 代理ID
     * @return 代理对象，如果不存在或无权限则返回null
     * @throws Exception 当查询失败时抛出异常
     */
    Proxy selectProxyById(Long proxyId) throws Exception;

    /**
     * 根据用户ID查询代理配置列表
     *
     * 查询指定用户的所有代理配置
     * 自动应用当前用户的权限过滤
     *
     * @param userId 用户ID
     * @return 代理配置列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<Proxy> selectProxiesByUserId(Long userId) throws Exception;

    /**
     * 根据条件查询代理配置列表（分页）
     *
     * 支持多条件查询，包括代理类型、状态、主机地址等
     * 自动应用用户权限过滤
     *
     * @param proxy 查询条件对象
     * @return 代理配置列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<Proxy> selectProxyList(Proxy proxy) throws Exception;

    /**
     * 创建新的代理配置
     *
     * 创建新的代理配置，包含完整的业务逻辑：
     * 1. 参数验证和唯一性检查
     * 2. 代理连接测试
     * 3. 密码加密处理
     * 4. 权限验证和审计记录
     *
     * @param proxy 代理配置对象，包含完整信息
     * @return 创建成功后的代理对象，包含生成的ID
     * @throws Exception 当参数验证失败、代理不可用或无权限时抛出异常
     */
    Proxy insertProxy(Proxy proxy) throws Exception;

    /**
     * 更新代理配置
     *
     * 更新代理的配置信息
     * 包含参数验证、权限检查和连接测试
     *
     * @param proxy 代理配置对象，包含要更新的信息
     * @return 是否更新成功
     * @throws Exception 当参数验证失败、代理不可用或无权限时抛出异常
     */
    boolean updateProxy(Proxy proxy) throws Exception;

    /**
     * 删除代理配置
     *
     * 根据代理ID删除代理配置
     * 包含权限验证和依赖检查
     *
     * @param proxyId 代理ID
     * @return 是否删除成功
     * @throws Exception 当代理不存在或有依赖关系时抛出异常
     */
    boolean deleteProxyById(Long proxyId) throws Exception;

    /**
     * 批量删除代理配置
     *
     * 根据代理ID列表批量删除代理配置
     * 包含批量权限验证和依赖检查
     *
     * @param proxyIds 代理ID列表
     * @return 是否删除成功
     * @throws Exception 当有代理不存在或有依赖关系时抛出异常
     */
    boolean deleteProxyByIds(List<Long> proxyIds) throws Exception;

    /**
     * 批量删除代理配置（详细结果）
     *
     * 根据代理ID列表批量删除代理配置，返回详细的删除结果
     * 支持部分成功的情况
     *
     * @param proxyIds 代理ID列表
     * @return 批量删除结果，包含成功和失败的详细信息
     * @throws Exception 当参数验证失败时抛出异常
     */
    BatchDeleteResultDTO deleteProxyByIdsWithResult(List<Long> proxyIds) throws Exception;

    /**
     * 更新代理状态
     *
     * 启用或弃用代理配置
     * 状态变更会立即影响代理的使用
     *
     * @param proxyId 代理ID
     * @param status 新状态（0正常 | 1弃用）
     * @return 是否更新成功
     * @throws Exception 当代理不存在或无权限时抛出异常
     */
    boolean updateProxyStatus(Long proxyId, String status) throws Exception;

    /**
     * 测试代理连接
     *
     * 测试指定代理配置的连接可用性
     * 用于代理配置的验证和监控
     *
     * @param proxyId 代理ID
     * @return 测试结果，包含连接状态和响应时间等信息
     * @throws Exception 当代理不存在或无权限时抛出异常
     */
    Map<String, Object> testProxyConnection(Long proxyId) throws Exception;

    /**
     * 测试代理连接（临时配置）
     *
     * 测试临时代理配置的连接可用性
     * 用于添加代理前的验证
     *
     * @param proxy 代理配置对象（无需包含ID）
     * @return 测试结果，包含连接状态和响应时间等信息
     * @throws Exception 当参数验证失败时抛出异常
     */
    Map<String, Object> testProxyConnection(Proxy proxy) throws Exception;

    /**
     * 检查代理地址是否唯一
     *
     * 用于添加或修改代理时的唯一性验证
     *
     * @param host 代理主机地址
     * @param port 代理端口
     * @param userId 用户ID
     * @param proxyId 排除的代理ID（用于更新验证）
     * @return true如果唯一，false如果已存在
     * @throws Exception 当验证失败时抛出异常
     */
    boolean checkProxyAddressUnique(String host, String port, Long userId, Long proxyId) throws Exception;

    /**
     * 获取可用的代理配置列表
     *
     * 查询状态为正常的代理配置
     * 用于业务系统选择可用的代理
     *
     * @param proxyType 代理类型（可选，为null则查询所有类型）
     * @return 可用的代理配置列表
     * @throws Exception 当查询失败时抛出异常
     */
    List<Proxy> getAvailableProxies(Integer proxyType) throws Exception;

    /**
     * 随机获取一个可用的代理配置
     *
     * 从可用的代理配置中随机选择一个
     * 用于负载均衡和代理轮换
     *
     * @param proxyType 代理类型（可选，为null则查询所有类型）
     * @return 可用的代理配置，如果没有可用代理则返回null
     * @throws Exception 当查询失败时抛出异常
     */
    Proxy getRandomAvailableProxy(Integer proxyType) throws Exception;

    /**
     * 批量测试代理连接
     *
     * 批量测试指定代理列表的连接可用性
     * 用于代理池的健康检查
     *
     * @param proxyIds 代理ID列表
     * @return 测试结果列表，每个结果包含代理ID和连接状态
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    List<Map<String, Object>> batchTestProxyConnections(List<Long> proxyIds) throws Exception;

    /**
     * 获取代理统计信息
     *
     * 获取代理相关的统计数据
     * 用于管理界面的统计展示
     *
     * @return 统计信息Map
     * @throws Exception 当查询失败时抛出异常
     */
    Map<String, Object> getProxyStatistics() throws Exception;

    /**
     * 获取用户代理统计信息
     *
     * 获取指定用户的代理统计数据
     *
     * @param userId 用户ID
     * @return 统计信息Map
     * @throws Exception 当查询失败时抛出异常
     */
    Map<String, Object> getUserProxyStatistics(Long userId) throws Exception;

    /**
     * 检查用户是否有代理权限
     *
     * 验证当前登录用户是否有权限访问指定代理配置
     * 用于数据权限控制
     *
     * @param proxyId 代理ID
     * @return true如果有权限，false否则
     * @throws Exception 当验证失败时抛出异常
     */
    boolean hasProxyPermission(Long proxyId) throws Exception;

    /**
     * 导入代理配置
     *
     * 批量导入代理配置
     * 支持Excel等格式的批量导入
     *
     * @param proxies 代理配置列表
     * @param updateSupport 是否支持更新已存在的代理
     * @return 导入结果，包含成功和失败信息
     * @throws Exception 当导入过程中发生错误时抛出异常
     */
    Map<String, Object> importProxies(List<Proxy> proxies, boolean updateSupport) throws Exception;

    /**
     * 导出代理配置
     *
     * 导出代理配置为指定格式
     * 支持Excel等格式的数据导出
     *
     * @param proxies 代理配置列表
     * @return 导出文件的字节数组
     * @throws Exception 当导出过程中发生错误时抛出异常
     */
    byte[] exportProxies(List<Proxy> proxies) throws Exception;

    // ==================== 高级业务方法 ====================

    /**
     * 检查代理池健康状态
     *
     * 检查用户所有代理配置的健康状态
     * 自动更新不可用代理的状态
     *
     * @return 健康检查结果，包含可用和不可用的代理数量
     * @throws Exception 当检查过程中发生错误时抛出异常
     */
    Map<String, Object> checkProxyPoolHealth() throws Exception;

    /**
     * 清理无效代理配置
     *
     * 清理长时间不可用的代理配置
     * 可配置清理条件和保留期限
     *
     * @param days 保留天数，超过此天数未使用的代理将被清理
     * @return 清理结果，包含清理的数量和详情
     * @throws Exception 当清理过程中发生错误时抛出异常
     */
    Map<String, Object> cleanupInvalidProxies(Integer days) throws Exception;

    /**
     * 复制代理配置
     *
     * 复制现有代理配置创建新的代理
     * 用于快速创建相似的代理配置
     *
     * @param proxyId 源代理ID
     * @param newHost 新主机地址
     * @param newPort 新端口
     * @return 新创建的代理配置
     * @throws Exception 当复制失败时抛出异常
     */
    Proxy copyProxyConfig(Long proxyId, String newHost, String newPort) throws Exception;

    /**
     * 获取代理使用建议
     *
     * 根据代理的使用情况和性能数据，提供使用建议
     *
     * @return 使用建议列表
     * @throws Exception 当分析过程中发生错误时抛出异常
     */
    List<String> getProxyUsageRecommendations() throws Exception;

    /**
     * 验证代理配置
     *
     * 验证代理配置的完整性和有效性
     * 包括格式检查、端口范围检查等
     *
     * @param proxy 代理配置对象
     * @return 验证结果，包含是否有效和错误信息
     * @throws Exception 当验证过程中发生错误时抛出异常
     */
    Map<String, Object> validateProxyConfig(Proxy proxy) throws Exception;

    /**
     * 加密代理密码
     *
     * 对代理密码进行加密处理
     * 用于数据库存储前的加密
     *
     * @param password 明文密码
     * @return 加密后的密码
     * @throws Exception 当加密失败时抛出异常
     */
    String encryptProxyPassword(String password) throws Exception;

    /**
     * 解密代理密码
     *
     * 对加密的代理密码进行解密
     * 用于使用时的解密
     *
     * @param encryptedPassword 加密密码
     * @return 明文密码
     * @throws Exception 当解密失败时抛出异常
     */
    String decryptProxyPassword(String encryptedPassword) throws Exception;
}