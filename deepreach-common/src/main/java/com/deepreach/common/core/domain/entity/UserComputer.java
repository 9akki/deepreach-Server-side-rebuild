package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 用户电脑关联实体类
 *
 * 对应数据库表：t_user_computer
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserComputer extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 最终用户ID
     */
    private String endUserId;

    /**
     * 云电脑ID
     */
    private String computerId;

}