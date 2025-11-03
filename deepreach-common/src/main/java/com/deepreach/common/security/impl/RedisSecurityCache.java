package com.deepreach.common.security.impl;

import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.security.CacheStats;
import com.deepreach.common.security.SecurityCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis安全缓存实现
 *
 * 特点：
 * 1. 分布式支持 - 多服务器共享缓存
 * 2. 持久化 - 数据可持久化存储
 * 3. 高可用 - 支持主从和集群
 * 4. 自动过期 - Redis原生支持过期策略
 *
 * 适用场景：
 * - 生产环境
 * - 分布式部署
 * - 第二阶段升级后
 * - 大规模应用
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "security.cache.type", havingValue = "redis")
public class RedisSecurityCache implements SecurityCache {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 配置属性
     */
    private final String keyPrefix = "security:cache:";
    private final String statsKey = "security:stats";

    /**
     * 统计信息
     */
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong operationCount = new AtomicLong(0);
    private volatile long lastCleanTime = System.currentTimeMillis();

    @Override
    public void storeUser(String token, LoginUser user, long expireTime) {
        if (token == null || user == null) {
            log.warn("Token or user cannot be null");
            return;
        }

        try {
            String key = buildKey(token);
            redisTemplate.opsForValue().set(key, user, expireTime, TimeUnit.SECONDS);
            operationCount.incrementAndGet();
            updateStats();

            log.debug("Stored user {} with token {} in Redis, expire in {} seconds",
                     user.getUsername(), token.substring(0, 8) + "...", expireTime);
        } catch (Exception e) {
            log.error("Failed to store user in Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store user in cache", e);
        }
    }

    @Override
    public LoginUser getUser(String token) {
        if (token == null) {
            missCount.incrementAndGet();
            return null;
        }

        try {
            String key = buildKey(token);
            LoginUser user = (LoginUser) redisTemplate.opsForValue().get(key);
            operationCount.incrementAndGet();

            if (user != null) {
                hitCount.incrementAndGet();
                log.debug("Found user {} for token {} in Redis",
                         user.getUsername(), token.substring(0, 8) + "...");
            } else {
                missCount.incrementAndGet();
                log.debug("User not found for token {} in Redis", token.substring(0, 8) + "...");
            }

            updateStats();
            return user;
        } catch (Exception e) {
            log.error("Failed to get user from Redis: {}", e.getMessage(), e);
            missCount.incrementAndGet();
            return null;
        }
    }

    @Override
    public void removeUser(String token) {
        if (token == null) {
            return;
        }

        try {
            String key = buildKey(token);
            Boolean result = redisTemplate.delete(key);
            operationCount.incrementAndGet();

            if (Boolean.TRUE.equals(result)) {
                log.debug("Removed user with token {} from Redis", token.substring(0, 8) + "...");
            }
            updateStats();
        } catch (Exception e) {
            log.error("Failed to remove user from Redis: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String token) {
        if (token == null) {
            return false;
        }

        try {
            String key = buildKey(token);
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check if user exists in Redis: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void refreshExpireTime(String token, long expireTime) {
        if (token == null) {
            return;
        }

        try {
            String key = buildKey(token);
            Boolean result = redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
            operationCount.incrementAndGet();

            if (Boolean.TRUE.equals(result)) {
                log.debug("Refreshed expire time for token {} in Redis", token.substring(0, 8) + "...");
            }
            updateStats();
        } catch (Exception e) {
            log.error("Failed to refresh expire time in Redis: {}", e.getMessage(), e);
        }
    }

    @Override
    public void storeUsers(Map<String, LoginUser> tokenUsers, long expireTime) {
        if (tokenUsers == null || tokenUsers.isEmpty()) {
            return;
        }

        try {
            Map<String, LoginUser> entries = new java.util.HashMap<>();
            for (Map.Entry<String, LoginUser> entry : tokenUsers.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    entries.put(buildKey(entry.getKey()), entry.getValue());
                }
            }

            if (!entries.isEmpty()) {
                // 批量设置
                redisTemplate.opsForValue().multiSet(entries);

                // 批量设置过期时间
                for (String key : entries.keySet()) {
                    redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
                }

                operationCount.addAndGet(entries.size());
                log.debug("Stored {} users in Redis", entries.size());
                updateStats();
            }
        } catch (Exception e) {
            log.error("Failed to store users in Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store users in cache", e);
        }
    }

    @Override
    public void removeUsers(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        try {
            Set<String> keys = tokens.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(this::buildKey)
                    .collect(java.util.stream.Collectors.toSet());

            if (!keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                operationCount.incrementAndGet();
                log.debug("Removed {} users from Redis", deletedCount);
                updateStats();
            }
        } catch (Exception e) {
            log.error("Failed to remove users from Redis: {}", e.getMessage(), e);
        }
    }

    @Override
    public void cleanExpiredUsers() {
        // Redis自动处理过期键，这里只需要更新清理时间
        lastCleanTime = System.currentTimeMillis();
        log.debug("Redis automatically handles expired keys");
    }

    @Override
    public long getUserCount() {
        try {
            Set<String> keys = redisTemplate.keys(keyPrefix + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("Failed to get user count from Redis: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public Collection<String> getActiveTokens() {
        try {
            Set<String> keys = redisTemplate.keys(keyPrefix + "*");
            if (keys != null) {
                return keys.stream()
                        .map(key -> key.substring(keyPrefix.length()))
                        .collect(java.util.stream.Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to get active tokens from Redis: {}", e.getMessage(), e);
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isAvailable() {
        try {
            redisTemplate.opsForValue().get("health:check");
            return true;
        } catch (Exception e) {
            log.warn("Redis is not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getCacheType() {
        return "redis";
    }

    @Override
    public CacheStats getStats() {
        cleanExpiredUsers();
        long userCount = getUserCount();

        return CacheStats.builder()
                .userCount(userCount)
                .hitCount(hitCount.get())
                .missCount(missCount.get())
                .operationCount(operationCount.get())
                .cacheType(getCacheType())
                .available(isAvailable())
                .lastCleanTime(lastCleanTime)
                .build();
    }

    /**
     * 更新统计信息到Redis
     */
    private void updateStats() {
        try {
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("hitCount", hitCount.get());
            stats.put("missCount", missCount.get());
            stats.put("operationCount", operationCount.get());
            stats.put("lastCleanTime", lastCleanTime);
            stats.put("cacheType", getCacheType());
            stats.put("available", isAvailable());

            redisTemplate.opsForHash().putAll(statsKey, stats);
            redisTemplate.expire(statsKey, 1, TimeUnit.HOURS); // 统计信息1小时过期
        } catch (Exception e) {
            log.warn("Failed to update stats in Redis: {}", e.getMessage());
        }
    }

    /**
     * 从Redis获取统计信息
     */
    private Map<Object, Object> getStatsFromRedis() {
        try {
            return redisTemplate.opsForHash().entries(statsKey);
        } catch (Exception e) {
            log.warn("Failed to get stats from Redis: {}", e.getMessage());
            return java.util.Collections.emptyMap();
        }
    }

    /**
     * 构建Redis键
     */
    private String buildKey(String token) {
        return keyPrefix + token;
    }

    /**
     * 获取Redis连接信息
     */
    public Map<String, Object> getRedisInfo() {
        try {
            Map<String, Object> info = new java.util.HashMap<>();

            // 获取Redis信息
            Properties properties = redisTemplate.getConnectionFactory()
                    .getConnection().info();

            info.put("redis_version", properties.getProperty("redis_version"));
            info.put("redis_mode", properties.getProperty("redis_mode"));
            info.put("used_memory", properties.getProperty("used_memory_human"));
            info.put("connected_clients", properties.getProperty("connected_clients"));
            info.put("uptime_in_seconds", properties.getProperty("uptime_in_seconds"));

            return info;
        } catch (Exception e) {
            log.error("Failed to get Redis info: {}", e.getMessage(), e);
            return java.util.Collections.emptyMap();
        }
    }

    /**
     * 手动清理所有安全缓存数据
     */
    public void clear() {
        try {
            Set<String> keys = redisTemplate.keys(keyPrefix + "*");
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                log.info("Cleared {} security cache entries from Redis", deletedCount);
            }
        } catch (Exception e) {
            log.error("Failed to clear security cache from Redis: {}", e.getMessage(), e);
        }
    }
}