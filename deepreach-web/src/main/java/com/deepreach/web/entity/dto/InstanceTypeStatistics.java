package com.deepreach.web.entity.dto;

import lombok.Data;

/**
 * 实例类型统计DTO
 *
 * 用于存储按类型分组的实例统计信息
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Data
public class InstanceTypeStatistics {

    /**
     * 实例类型（0营销 1拓客）
     */
    private String instanceType;

    /**
     * 该类型的实例数量
     */
    private Long count;

    /**
     * 类型显示名称
     */
    public String getTypeDisplay() {
        if ("0".equals(instanceType)) {
            return "营销";
        } else if ("1".equals(instanceType)) {
            return "拓客";
        } else {
            return "未知类型";
        }
    }
}