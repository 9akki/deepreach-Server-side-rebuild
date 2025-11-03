package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 云电脑用户实体类
 *
 * 对应数据库表：t_cloud_user
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CloudUser extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 客户端用户名（与sys_user.username对应）
     */
    private String clientUsername;

    /**
     * 最终用户ID
     */
    private String endUserId;

    /**
     * 真实昵称
     */
    private String realNickName;

    /**
     * 密码
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 电话
     */
    private String phone;

    /**
     * 所有者类型
     */
    private String ownerType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 组织ID
     */
    private String orgid;

    /**
     * 是否新用户
     */
    private Integer isNew;

}