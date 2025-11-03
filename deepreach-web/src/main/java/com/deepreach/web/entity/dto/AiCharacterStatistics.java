package com.deepreach.web.entity.dto;

import lombok.Data;

/**
 * AI人设数量统计信息
 *
 * 聚合指定用户范围内的人设数量分布。
 */
@Data
public class AiCharacterStatistics {

    /**
     * 总人设数量
     */
    private Long totalCount;

    /**
     * 系统人设数量
     */
    private Long systemCount;

    /**
     * 用户自建人设数量
     */
    private Long userCreatedCount;

    /**
     * 情感类人设数量
     */
    private Long emotionCount;

    /**
     * 业务类人设数量
     */
    private Long businessCount;

    /**
     * 返回Long值，空时为0。
     */
    public long safeValue(Long value) {
        return value != null ? value : 0L;
    }
}
