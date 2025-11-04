package com.deepreach.common.core.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据传输对象, 用于表示用户与其父用户之间的层级关系.
 */
@Data
public class UserHierarchyNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前用户ID.
     */
    private Long userId;

    /**
     * 父用户ID, 根用户时为null.
     */
    private Long parentUserId;
}
