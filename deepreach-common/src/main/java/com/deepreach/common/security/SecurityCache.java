package com.deepreach.common.security;

import com.deepreach.common.core.domain.model.LoginUser;

import java.util.Collection;

/**
 * 安全缓存接口
 *
 * 设计理念：
 * 1. 可插拔 - 支持多种实现方式（内存、Redis等）
 * 2. 统一接口 - 所有实现使用相同的API
 * 3. 配置驱动 - 通过配置切换实现方式
 * 4. 性能优化 - 支持批量操作
 *
 * @author DeepReach Team
 * @version 1.0
 */
public interface SecurityCache {

    /**
     * 存储用户信息
     *
     * @param token      用户令牌
     * @param user       用户信息
     * @param expireTime 过期时间（秒）
     */
    void storeUser(String token, LoginUser user, long expireTime);

    /**
     * 获取用户信息
     *
     * @param token 用户令牌
     * @return 用户信息，不存在时返回null
     */
    LoginUser getUser(String token);

    /**
     * 移除用户信息
     *
     * @param token 用户令牌
     */
    void removeUser(String token);

    /**
     * 检查用户是否存在
     *
     * @param token 用户令牌
     * @return 是否存在
     */
    boolean exists(String token);

    /**
     * 刷新用户过期时间
     *
     * @param token      用户令牌
     * @param expireTime 新的过期时间（秒）
     */
    void refreshExpireTime(String token, long expireTime);

    /**
     * 批量存储用户信息
     *
     * @param tokenUsers 令牌和用户的映射
     * @param expireTime 过期时间（秒）
     */
    void storeUsers(java.util.Map<String, LoginUser> tokenUsers, long expireTime);

    /**
     * 批量移除用户信息
     *
     * @param tokens 令牌集合
     */
    void removeUsers(Collection<String> tokens);

    /**
     * 清理所有过期用户
     */
    void cleanExpiredUsers();

    /**
     * 获取当前缓存中的用户数量
     *
     * @return 用户数量
     */
    long getUserCount();

    /**
     * 获取所有活跃的令牌
     *
     * @return 令牌集合
     */
    Collection<String> getActiveTokens();

    /**
     * 检查缓存是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 获取缓存类型
     *
     * @return 缓存类型（memory、redis等）
     */
    String getCacheType();

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    CacheStats getStats();
}