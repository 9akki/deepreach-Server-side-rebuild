package com.deepreach.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存统计信息
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStats {

    /**
     * 当前用户数量
     */
    private long userCount;

    /**
     * 总命中次数
     */
    private long hitCount;

    /**
     * 总未命中次数
     */
    private long missCount;

    /**
     * 总操作次数
     */
    private long operationCount;

    /**
     * 缓存类型
     */
    private String cacheType;

    /**
     * 是否可用
     */
    private boolean available;

    /**
     * 上次清理时间
     */
    private long lastCleanTime;

    /**
     * 获取命中率
     *
     * @return 命中率（0-100）
     */
    public double getHitRate() {
        long total = hitCount + missCount;
        return total == 0 ? 0.0 : (double) hitCount / total * 100;
    }

    /**
     * 获取命中率字符串
     *
     * @return 命中率字符串（如 "85.5%"）
     */
    public String getHitRateString() {
        return String.format("%.2f%%", getHitRate());
    }
}