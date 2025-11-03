package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 阿里云ECD配置实体类
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-11-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AliyunEcdConfig extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;
}