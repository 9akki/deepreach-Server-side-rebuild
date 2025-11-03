package com.deepreach.web.entity.dto;

import lombok.Data;

/**
 * 人设使用统计DTO
 *
 * 用于存储按AI人设分组的实例使用统计信息
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Data
public class CharacterUsageStatistics {

    /**
     * AI人设ID
     */
    private Integer characterId;

    /**
     * 使用该人设的实例数量
     */
    private Long count;

    /**
     * 人设显示名称
     */
    public String getCharacterDisplay() {
        return "人设" + characterId;
    }
}