package com.deepreach.common.core.service.impl;

import com.deepreach.common.core.domain.dto.UserHierarchyNodeDTO;
import com.deepreach.common.core.domain.dto.UserHierarchyTreeDTO;
import com.deepreach.common.core.mapper.SysUserMapper;
import com.deepreach.common.core.service.UserHierarchyService;
import com.deepreach.common.utils.UserHierarchyTreeBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 基于 Redis 缓存的用户层级服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserHierarchyServiceImpl implements UserHierarchyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SysUserMapper userMapper;

    @Override
    public Long findParentId(Long userId) {
        if (userId == null) {
            return null;
        }
        return getTree().getParentMapping().get(userId);
    }

    @Override
    public List<Long> findDirectChildren(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        Map<Long, List<Long>> childrenMap = getTree().getChildrenMapping();
        return childrenMap.getOrDefault(userId, Collections.emptyList());
    }

    @Override
    public Set<Long> findDescendantIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Map<Long, List<Long>> childrenMap = getTree().getChildrenMapping();
        Set<Long> descendants = new LinkedHashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(userId);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            List<Long> children = childrenMap.getOrDefault(current, Collections.emptyList());
            for (Long child : children) {
                if (child != null && descendants.add(child)) {
                    stack.push(child);
                }
            }
        }
        descendants.remove(userId);
        return descendants;
    }

    @Override
    public boolean isAncestor(Long ancestorId, Long targetId) {
        if (ancestorId == null || targetId == null || Objects.equals(ancestorId, targetId)) {
            return false;
        }
        Long parent = findParentId(targetId);
        while (parent != null && !Objects.equals(parent, ancestorId)) {
            parent = findParentId(parent);
        }
        return Objects.equals(parent, ancestorId);
    }

    @Override
    public Set<Long> findRootIds() {
        return new LinkedHashSet<>(getTree().getRootUserIds());
    }

    // ==================== 内部方法 ====================

    private UserHierarchyTreeDTO getTree() {
        Object cached = redisTemplate.opsForValue().get(UserHierarchyTreeBuilder.USER_TREE_CACHE_KEY);
        if (cached instanceof UserHierarchyTreeDTO) {
            return (UserHierarchyTreeDTO) cached;
        }
        log.warn("用户层级缓存未命中，准备从数据库重建");
        rebuildCache();
        Object fresh = redisTemplate.opsForValue().get(UserHierarchyTreeBuilder.USER_TREE_CACHE_KEY);
        if (fresh instanceof UserHierarchyTreeDTO) {
            return (UserHierarchyTreeDTO) fresh;
        }
        log.error("无法获取用户层级缓存，返回空树");
        return UserHierarchyTreeBuilder.build(Collections.emptyList());
    }

    private void rebuildCache() {
        try {
            List<UserHierarchyNodeDTO> relations = userMapper.selectAllUserHierarchyRelations();
            UserHierarchyTreeDTO tree = UserHierarchyTreeBuilder.build(relations);
            redisTemplate.opsForValue().set(UserHierarchyTreeBuilder.USER_TREE_CACHE_KEY, tree);
        } catch (Exception e) {
            log.error("重建用户层级缓存失败", e);
        }
    }
}
