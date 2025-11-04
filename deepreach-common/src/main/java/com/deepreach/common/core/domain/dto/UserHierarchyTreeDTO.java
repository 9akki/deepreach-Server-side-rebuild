package com.deepreach.common.core.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用户层级树结构, 用于在Redis中缓存用户父子关系.
 */
@Data
public class UserHierarchyTreeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 根用户ID列表.
     */
    private List<Long> rootUserIds = Collections.emptyList();

    /**
     * 父用户ID -> 子用户ID列表映射.
     */
    private Map<Long, List<Long>> childrenMapping = Collections.emptyMap();

    /**
     * 用户ID -> 父用户ID映射, 根用户时父ID为null.
     */
    private Map<Long, Long> parentMapping = Collections.emptyMap();
}
