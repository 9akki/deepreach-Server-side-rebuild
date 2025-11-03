package com.deepreach.web.entity.dto;

import lombok.Data;

/**
 * 平台使用统计DTO
 *
 * 用于存储按平台分组的实例使用统计信息
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Data
public class PlatformUsageStatistics {

    /**
     * 平台ID
     */
    private Integer platformId;

    /**
     * 使用该平台的实例数量
     */
    private Long count;

    /**
     * 平台显示名称
     */
    public String getPlatformDisplay() {
        return "平台" + platformId;
    }
}