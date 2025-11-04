package com.deepreach.common.core.service;

import java.util.List;
import java.util.Set;

/**
 * 用户层级服务，基于用户树缓存提供父子关系查询能力。
 */
public interface UserHierarchyService {

    /**
     * 获取指定用户的父用户ID（若为根节点返回null）。
     */
    Long findParentId(Long userId);

    /**
     * 获取指定用户的所有直接子用户ID。
     */
    List<Long> findDirectChildren(Long userId);

    /**
     * 获取指定用户的所有子孙用户ID（不包含自身）。
     */
    Set<Long> findDescendantIds(Long userId);

    /**
     * 判断 ancestorId 是否为 targetId 的祖先（不含自身）。
     */
    boolean isAncestor(Long ancestorId, Long targetId);

    /**
     * 获取用户树的根节点集合。
     */
    Set<Long> findRootIds();
}
