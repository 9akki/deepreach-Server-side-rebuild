package com.deepreach.common.security.impl;

import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.security.CacheStats;
import com.deepreach.common.security.SecurityCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存安全缓存实现
 *
 * 特点：
 * 1. 无外部依赖 - 启动即可使用
 * 2. 高性能 - 内存访问，速度快
 * 3. 线程安全 - 使用ConcurrentHashMap
 * 4. 自动清理 - 定期清理过期数据
 *
 * 适用场景：
 * - 开发环境
 * - 小规模应用
 * - 单机部署
 * - 第一阶段MVP
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "security.cache.type", havingValue = "memory", matchIfMissing = true)
public class MemorySecurityCache implements SecurityCache {

    /**
     * 用户缓存
     * Key: token, Value: CacheEntry
     */
    private final Map<String, CacheEntry> userCache = new ConcurrentHashMap<>();

    /**
     * 统计信息
     */
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong operationCount = new AtomicLong(0);
    private volatile long lastCleanTime = System.currentTimeMillis();

    /**
     * 缓存实体
     */
    private static class CacheEntry {
        private final LoginUser user;
        private final long expireTime;

        public CacheEntry(LoginUser user, long expireTime) {
            this.user = user;
            this.expireTime = expireTime;
        }

        public LoginUser getUser() {
            return user;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        public long getExpireTime() {
            return expireTime;
        }
    }

    @Override
    public void storeUser(String token, LoginUser user, long expireTime) {
        if (token == null || user == null) {
            log.warn("Token or user cannot be null");
            return;
        }

        long actualExpireTime = System.currentTimeMillis() + expireTime * 1000;
        userCache.put(token, new CacheEntry(user, actualExpireTime));
        operationCount.incrementAndGet();

        log.debug("Stored user {} with token {}, expire at {}",
                 user.getUsername(), token.substring(0, 8) + "...", actualExpireTime);
    }

    @Override
    public LoginUser getUser(String token) {
        if (token == null) {
            missCount.incrementAndGet();
            return null;
        }

        CacheEntry entry = userCache.get(token);
        if (entry == null) {
            missCount.incrementAndGet();
            return null;
        }

        if (entry.isExpired()) {
            userCache.remove(token);
            missCount.incrementAndGet();
            log.debug("Token {} expired, removed from cache", token.substring(0, 8) + "...");
            return null;
        }

        hitCount.incrementAndGet();
        operationCount.incrementAndGet();
        return entry.getUser();
    }

    @Override
    public void removeUser(String token) {
        if (token != null) {
            CacheEntry removed = userCache.remove(token);
            operationCount.incrementAndGet();
            if (removed != null) {
                log.debug("Removed user {} with token {}",
                         removed.getUser().getUsername(), token.substring(0, 8) + "...");
            }
        }
    }

    @Override
    public boolean exists(String token) {
        if (token == null) {
            return false;
        }

        CacheEntry entry = userCache.get(token);
        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            userCache.remove(token);
            return false;
        }

        return true;
    }

    @Override
    public void refreshExpireTime(String token, long expireTime) {
        if (token == null) {
            return;
        }

        CacheEntry entry = userCache.get(token);
        if (entry != null && !entry.isExpired()) {
            long newExpireTime = System.currentTimeMillis() + expireTime * 1000;
            userCache.put(token, new CacheEntry(entry.getUser(), newExpireTime));
            operationCount.incrementAndGet();
            log.debug("Refreshed expire time for token {}", token.substring(0, 8) + "...");
        }
    }

    @Override
    public void storeUsers(Map<String, LoginUser> tokenUsers, long expireTime) {
        if (tokenUsers != null && !tokenUsers.isEmpty()) {
            long actualExpireTime = System.currentTimeMillis() + expireTime * 1000;
            Map<String, CacheEntry> entries = new ConcurrentHashMap<>();

            for (Map.Entry<String, LoginUser> entry : tokenUsers.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    entries.put(entry.getKey(), new CacheEntry(entry.getValue(), actualExpireTime));
                }
            }

            userCache.putAll(entries);
            operationCount.addAndGet(entries.size());

            log.debug("Stored {} users in cache", entries.size());
        }
    }

    @Override
    public void removeUsers(Collection<String> tokens) {
        if (tokens != null && !tokens.isEmpty()) {
            int removedCount = 0;
            for (String token : tokens) {
                if (token != null && userCache.remove(token) != null) {
                    removedCount++;
                }
            }
            operationCount.addAndGet(removedCount);
            log.debug("Removed {} users from cache", removedCount);
        }
    }

    @Override
    public void cleanExpiredUsers() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        Set<Map.Entry<String, CacheEntry>> entries = userCache.entrySet();
        for (Map.Entry<String, CacheEntry> entry : entries) {
            if (entry.getValue().isExpired()) {
                userCache.remove(entry.getKey());
                removedCount++;
            }
        }

        lastCleanTime = currentTime;
        operationCount.incrementAndGet();

        if (removedCount > 0) {
            log.info("Cleaned {} expired users from cache", removedCount);
        }
    }

    @Override
    public long getUserCount() {
        // 清理过期用户后再计数
        cleanExpiredUsers();
        return userCache.size();
    }

    @Override
    public Collection<String> getActiveTokens() {
        cleanExpiredUsers();
        return java.util.Collections.unmodifiableSet(userCache.keySet());
    }

    @Override
    public boolean isAvailable() {
        return true; // 内存缓存总是可用的
    }

    @Override
    public String getCacheType() {
        return "memory";
    }

    @Override
    public CacheStats getStats() {
        cleanExpiredUsers(); // 确保统计数据准确

        return CacheStats.builder()
                .userCount(userCache.size())
                .hitCount(hitCount.get())
                .missCount(missCount.get())
                .operationCount(operationCount.get())
                .cacheType(getCacheType())
                .available(isAvailable())
                .lastCleanTime(lastCleanTime)
                .build();
    }

    /**
     * 获取缓存详细信息（用于调试）
     */
    public Map<String, Object> getDetailedInfo() {
        Map<String, Object> info = new ConcurrentHashMap<>();
        info.put("userCount", userCache.size());
        info.put("hitCount", hitCount.get());
        info.put("missCount", missCount.get());
        info.put("operationCount", operationCount.get());
        info.put("hitRate", getStats().getHitRateString());
        info.put("lastCleanTime", lastCleanTime);
        info.put("cacheType", getCacheType());
        info.put("available", isAvailable());

        // 获取用户列表（不包含敏感信息）
        Map<String, String> users = new ConcurrentHashMap<>();
        for (Map.Entry<String, CacheEntry> entry : userCache.entrySet()) {
            LoginUser user = entry.getValue().getUser();
            if (user != null) {
                users.put(entry.getKey().substring(0, 8) + "...",
                         user.getUsername() + " (" + entry.getValue().getExpireTime() + ")");
            }
        }
        info.put("activeUsers", users);

        return info;
    }

    /**
     * 手动清理缓存
     */
    public void clear() {
        int size = userCache.size();
        userCache.clear();
        operationCount.incrementAndGet();
        log.info("Cleared all {} users from cache", size);
    }

    /**
     * 获取缓存大小（包含过期数据）
     */
    public int getRawSize() {
        return userCache.size();
    }
}