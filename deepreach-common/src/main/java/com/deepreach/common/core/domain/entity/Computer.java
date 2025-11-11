package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 云电脑实体类
 *
 * 对应数据库表：t_computer
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Computer extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 云电脑ID
     */
    private String computerId;

    /**
     * 套餐ID
     */
    private String bundleId;

    /**
     * 策略组ID
     */
    private String policyGroupId;

    /**
     * 办公站点ID
     */
    private String officeSiteId;

    /**
     * 云电脑访问域名
     */
    private String endpoint;

    /**
     * 云电脑所属区域
     */
    private String regionId;

}
