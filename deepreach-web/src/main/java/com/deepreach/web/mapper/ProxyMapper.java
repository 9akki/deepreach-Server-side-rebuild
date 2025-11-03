package com.deepreach.web.mapper;

import com.deepreach.web.entity.Proxy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代理池Mapper接口
 *
 * 负责代理配置相关的数据库操作，包括：
 * 1. 代理配置基本CRUD操作
 * 2. 代理配置查询和过滤
 * 3. 用户代理权限管理
 * 4. 代理状态管理
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Mapper
public interface ProxyMapper {

    /**
     * 根据代理ID查询代理配置
     *
     * 查询代理的完整配置信息
     * 包括用户关联和基本配置信息
     *
     * @param proxyId 代理ID
     * @return 代理实体对象，如果不存在则返回null
     */
    Proxy selectProxyById(@Param("proxyId") Long proxyId);

    /**
     * 根据用户ID查询代理配置列表
     *
     * 查询指定用户的所有代理配置
     * 用于用户代理管理界面
     *
     * @param userId 用户ID
     * @return 代理配置列表
     */
    List<Proxy> selectProxiesByUserId(@Param("userId") Long userId);

    /**
     * 查询代理配置列表（分页）
     *
     * 支持多条件查询，包括代理类型、状态、用户等
     * 自动应用用户权限过滤，确保用户只能查看自己的代理配置
     *
     * @param proxy 查询条件对象，包含各种查询参数
     * @return 代理配置列表，按创建时间倒序排列
     */
    List<Proxy> selectProxyList(Proxy proxy);

    /**
     * 根据代理类型查询代理配置列表
     *
     * @param proxyType 代理类型（0 HTTP | 1 SOCKS5）
     * @param userId 用户ID（用于权限控制）
     * @return 指定类型的代理配置列表
     */
    List<Proxy> selectProxiesByType(@Param("proxyType") Integer proxyType, @Param("userId") Long userId);

    /**
     * 根据状态查询代理配置列表
     *
     * @param status 状态（0正常 | 1弃用）
     * @param userId 用户ID（用于权限控制）
     * @return 指定状态的代理配置列表
     */
    List<Proxy> selectProxiesByStatus(@Param("status") String status, @Param("userId") Long userId);

    /**
     * 统计代理配置总数
     *
     * 用于后台统计功能，统计符合条件的代理数量
     * 支持多条件统计，自动应用用户权限过滤
     *
     * @param proxy 查询条件对象
     * @return 代理配置总数
     */
    int countProxies(Proxy proxy);

    /**
     * 统计指定用户的代理配置数量
     *
     * @param userId 用户ID
     * @return 代理配置数量
     */
    int countProxiesByUserId(@Param("userId") Long userId);

    /**
     * 统计指定代理类型的代理配置数量
     *
     * @param proxyType 代理类型
     * @param userId 用户ID（用于权限控制）
     * @return 代理配置数量
     */
    int countProxiesByType(@Param("proxyType") Integer proxyType, @Param("userId") Long userId);

    /**
     * 插入新的代理配置
     *
     * 创建新的代理配置记录
     * 密码需要在Service层进行加密处理
     *
     * @param proxy 代理配置对象，包含完整的配置信息
     * @return 成功插入的记录数，通常为1
     */
    int insertProxy(Proxy proxy);

    /**
     * 更新代理配置
     *
     * 更新代理的基本信息，包括地址、端口、认证等
     * 密码修改时需要在Service层进行加密处理
     *
     * @param proxy 代理配置对象，包含要更新的信息
     * @return 成功更新的记录数
     */
    int updateProxy(Proxy proxy);

    /**
     * 更新代理状态
     *
     * 用于启用/弃用代理配置
     * 状态变更会立即影响代理的使用
     *
     * @param proxyId 代理ID
     * @param status 新状态（0正常 | 1弃用）
     * @return 成功更新的记录数
     */
    int updateProxyStatus(@Param("proxyId") Long proxyId, @Param("status") String status);

    /**
     * 删除代理配置
     *
     * 根据代理ID删除代理配置记录
     * 操作不可逆，请谨慎使用
     *
     * @param proxyId 代理ID
     * @return 成功删除的记录数
     */
    int deleteProxyById(@Param("proxyId") Long proxyId);

    /**
     * 批量删除代理配置
     *
     * 根据代理ID列表批量删除代理配置
     * 用于批量管理功能，提高操作效率
     *
     * @param proxyIds 代理ID列表
     * @return 成功删除的记录数
     */
    int deleteProxyByIds(@Param("proxyIds") List<Long> proxyIds);

    /**
     * 检查代理地址和端口是否唯一
     *
     * 用于添加代理配置时的唯一性验证
     * 同一用户不能添加相同的代理地址和端口组合
     *
     * @param host 代理主机地址
     * @param port 代理端口
     * @param userId 用户ID
     * @param proxyId 排除的代理ID，用于更新验证
     * @return 存在相同配置的记录数，0表示唯一
     */
    int checkProxyAddressUnique(@Param("host") String host,
                               @Param("port") String port,
                               @Param("userId") Long userId,
                               @Param("proxyId") Long proxyId);

    /**
     * 检查用户是否拥有指定代理配置
     *
     * 用于权限验证，确保用户只能操作自己的代理配置
     *
     * @param proxyId 代理ID
     * @param userId 用户ID
     * @return true如果用户拥有该代理配置，false否则
     */
    boolean checkProxyOwnership(@Param("proxyId") Long proxyId, @Param("userId") Long userId);

    /**
     * 查询可用的代理配置列表
     *
     * 查询状态为正常的代理配置
     * 用于业务系统选择可用的代理
     *
     * @param userId 用户ID（用于权限控制）
     * @param proxyType 代理类型（可选，为null则查询所有类型）
     * @return 可用的代理配置列表
     */
    List<Proxy> selectAvailableProxies(@Param("userId") Long userId, @Param("proxyType") Integer proxyType);

    /**
     * 随机获取一个可用的代理配置
     *
     * 从可用的代理配置中随机选择一个
     * 用于负载均衡和代理轮换
     *
     * @param userId 用户ID（用于权限控制）
     * @param proxyType 代理类型（可选，为null则查询所有类型）
     * @return 可用的代理配置，如果没有可用代理则返回null
     */
    Proxy selectRandomAvailableProxy(@Param("userId") Long userId, @Param("proxyType") Integer proxyType);

    /**
     * 测试代理连接
     *
     * 尝试连接指定的代理服务器，测试其可用性
     * 用于代理配置的连通性验证
     *
     * @param proxyId 代理ID
     * @return true如果代理可用，false否则
     */
    boolean testProxyConnection(@Param("proxyId") Long proxyId);

    // ==================== 高级查询方法 ====================

    /**
     * 根据主机地址模糊查询代理配置
     *
     * @param host 主机地址（支持模糊匹配）
     * @param userId 用户ID（用于权限控制）
     * @return 匹配的代理配置列表
     */
    List<Proxy> selectProxiesByHostLike(@Param("host") String host, @Param("userId") Long userId);

    /**
     * 根据端口范围查询代理配置
     *
     * @param startPort 起始端口
     * @param endPort 结束端口
     * @param userId 用户ID（用于权限控制）
     * @return 在指定端口范围内的代理配置列表
     */
    List<Proxy> selectProxiesByPortRange(@Param("startPort") Integer startPort,
                                        @Param("endPort") Integer endPort,
                                        @Param("userId") Long userId);

    /**
     * 查询需要认证的代理配置列表
     *
     * @param userId 用户ID（用于权限控制）
     * @return 需要认证的代理配置列表
     */
    List<Proxy> selectProxiesWithAuthentication(@Param("userId") Long userId);

    /**
     * 查询无需认证的代理配置列表
     *
     * @param userId 用户ID（用于权限控制）
     * @return 无需认证的代理配置列表
     */
    List<Proxy> selectProxiesWithoutAuthentication(@Param("userId") Long userId);

    /**
     * 根据创建时间范围查询代理配置
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param userId 用户ID（用于权限控制）
     * @return 在指定时间范围内创建的代理配置列表
     */
    List<Proxy> selectProxiesByCreateTimeRange(@Param("startTime") java.time.LocalDateTime startTime,
                                             @Param("endTime") java.time.LocalDateTime endTime,
                                             @Param("userId") Long userId);

    /**
     * 查询最近使用的代理配置
     *
     * @param limit 限制数量
     * @param userId 用户ID（用于权限控制）
     * @return 最近使用的代理配置列表
     */
    List<Proxy> selectRecentlyUsedProxies(@Param("limit") Integer limit, @Param("userId") Long userId);
}