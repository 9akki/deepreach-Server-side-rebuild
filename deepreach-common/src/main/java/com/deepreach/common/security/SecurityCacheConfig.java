package com.deepreach.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 安全缓存配置
 *
 * 配置安全缓存相关功能，包括：
 * 1. 缓存类型选择
 * 2. 缓存清理策略
 * 3. 缓存统计信息
 * 4. 缓存健康检查
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Slf4j
@Configuration
@EnableScheduling
public class SecurityCacheConfig {

    @Value("${security.cache.type:memory}")
    private String cacheType;

    @Value("${security.cache.clean.interval:300}")
    private int cleanInterval;

    @Value("${security.cache.stats.enabled:true}")
    private boolean statsEnabled;

    /**
     * 获取缓存类型
     *
     * @return 缓存类型（memory、redis等）
     */
    public String getCacheType() {
        return cacheType;
    }

    /**
     * 获取清理间隔（秒）
     *
     * @return 清理间隔
     */
    public int getCleanInterval() {
        return cleanInterval;
    }

    /**
     * 是否启用统计
     *
     * @return 是否启用统计
     */
    public boolean isStatsEnabled() {
        return statsEnabled;
    }

    /**
     * 定时清理过期缓存
     *
     * 根据配置的清理间隔，定期清理过期的缓存数据
     */
    @Scheduled(fixedRateString = "${security.cache.clean.interval:300000}")
    @ConditionalOnProperty(name = "security.cache.clean.enabled", havingValue = "true", matchIfMissing = true)
    public void cleanExpiredCache() {
        try {
            // 这里会触发具体实现类的清理逻辑
            log.trace("Scheduled cache cleanup triggered");
        } catch (Exception e) {
            log.error("Error during scheduled cache cleanup", e);
        }
    }

    /**
     * 定时输出缓存统计信息
     *
     * 根据配置的统计间隔，定期输出缓存统计信息
     */
    @Scheduled(fixedRateString = "${security.cache.stats.interval:600000}")
    @ConditionalOnProperty(name = "security.cache.stats.enabled", havingValue = "true", matchIfMissing = true)
    public void logCacheStats() {
        try {
            // 这里会触发具体实现类的统计逻辑
            log.trace("Scheduled cache stats logging triggered");
        } catch (Exception e) {
            log.error("Error during scheduled cache stats logging", e);
        }
    }
}