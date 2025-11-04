package com.deepreach.common.utils;

import com.deepreach.common.core.domain.dto.UserHierarchyNodeDTO;
import com.deepreach.common.core.domain.dto.UserHierarchyTreeDTO;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具类, 用于根据用户与父用户的关系列表构建用户层级树.
 */
public final class UserHierarchyTreeBuilder {

    public static final String USER_TREE_CACHE_KEY = "user:hierarchy:tree";

    private UserHierarchyTreeBuilder() {
        // utility class
    }

    /**
     * 根据用户父子关系列表构建层级树结构.
     *
     * @param relations 用户与父用户的关系数据
     * @return 构建好的层级树结构
     */
    public static UserHierarchyTreeDTO build(List<UserHierarchyNodeDTO> relations) {
        if (relations == null || relations.isEmpty()) {
            return emptyTree();
        }

        Map<Long, Long> parentMapping = new HashMap<>();
        Map<Long, List<Long>> childrenMapping = new HashMap<>();
        Set<Long> rootSet = new HashSet<>();

        for (UserHierarchyNodeDTO relation : relations) {
            if (relation == null || relation.getUserId() == null) {
                continue;
            }
            Long userId = relation.getUserId();
            Long parentUserId = relation.getParentUserId();

            parentMapping.put(userId, parentUserId);
            childrenMapping.computeIfAbsent(userId, unused -> new ArrayList<>());

            if (parentUserId == null || parentUserId <= 0) {
                rootSet.add(userId);
                continue;
            }

            childrenMapping.computeIfAbsent(parentUserId, unused -> new ArrayList<>())
                .add(userId);
        }

        // Handle users whose parent is missing in the dataset, treat them as root nodes
        for (Map.Entry<Long, Long> entry : parentMapping.entrySet()) {
            Long userId = entry.getKey();
            Long parentId = entry.getValue();
            if (parentId != null && !parentMapping.containsKey(parentId)) {
                rootSet.add(userId);
            }
        }

        Map<Long, List<Long>> normalizedChildren = childrenMapping.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()),
                (left, right) -> left,
                HashMap::new
            ));

        List<Long> rootIds = rootSet.stream()
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        UserHierarchyTreeDTO tree = new UserHierarchyTreeDTO();
        tree.setParentMapping(new HashMap<>(parentMapping));
        tree.setChildrenMapping(normalizedChildren);
        tree.setRootUserIds(rootIds);
        return tree;
    }

    private static UserHierarchyTreeDTO emptyTree() {
        UserHierarchyTreeDTO tree = new UserHierarchyTreeDTO();
        tree.setParentMapping(Collections.emptyMap());
        tree.setChildrenMapping(Collections.emptyMap());
        tree.setRootUserIds(Collections.emptyList());
        return tree;
    }
}

