package com.deepreach.common.core.service.impl;

import com.deepreach.common.core.domain.dto.UserHierarchyNodeDTO;
import com.deepreach.common.core.domain.dto.UserHierarchyGroupDTO;
import com.deepreach.common.core.domain.dto.UserHierarchyTreeDTO;
import com.deepreach.common.core.domain.dto.UserListRequest;
import com.deepreach.common.core.domain.entity.SysOperLog;
import com.deepreach.common.core.domain.entity.SysRole;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.mapper.SysUserMapper;
import com.deepreach.common.core.mapper.SysRoleMapper;
import com.deepreach.common.core.service.UserHierarchyService;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.utils.UserHierarchyTreeBuilder;
import com.deepreach.common.security.UserRoleUtils;
import com.deepreach.common.security.enums.UserIdentity;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统用户Service实现类
 *
 * 基于部门类型的简化用户Service实现，包含：
 * 1. 用户基本信息管理业务逻辑
 * 2. 用户认证和授权业务处理
 * 3. 用户角色和权限管理
 * 4. 基于组织架构的数据权限控制实现
 * 5. 安全相关业务处理
 *
 * 设计理念：
 * - 部门决定用户类型：用户类型由部门类型自动决定，无需手动设置
 * - 简化业务逻辑：移除复杂的业务字段操作，专注于组织架构管理
 * - 权限控制优化：基于部门类型和层级进行细粒度的权限控制
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Slf4j
@Service
public class SysUserServiceImpl implements SysUserService {

    private static final Set<UserIdentity> AGENT_IDENTITIES =
        EnumSet.of(UserIdentity.AGENT_LEVEL_1, UserIdentity.AGENT_LEVEL_2, UserIdentity.AGENT_LEVEL_3);

    private static final class UserCreationContext {
        private final LoginUser creator;
        private final SysUser parentUser;
        private final UserIdentity targetIdentity;
        private final Set<UserIdentity> creatorIdentities;
        private final Set<UserIdentity> parentIdentities;

        private UserCreationContext(LoginUser creator,
                                    SysUser parentUser,
                                    UserIdentity targetIdentity,
                                    Set<UserIdentity> creatorIdentities,
                                    Set<UserIdentity> parentIdentities) {
            this.creator = creator;
            this.parentUser = parentUser;
            this.targetIdentity = targetIdentity;
            this.creatorIdentities = creatorIdentities;
            this.parentIdentities = parentIdentities;
        }
    }

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private UserHierarchyService hierarchyService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== 基础查询方法 ====================

    /**
     * 根据用户ID查询用户
     */
    @Override
    public SysUser selectUserById(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("查询用户失败：用户ID无效 - {}", userId);
            return null;
        }

        try {
            SysUser user = userMapper.selectUserById(userId);
            if (user != null) {
                log.debug("查询用户成功：用户ID={}, 用户名={}", userId, user.getUsername());
            } else {
                log.debug("查询用户失败：用户不存在 - {}", userId);
            }
            return user;
        } catch (Exception e) {
            log.error("查询用户异常：用户ID={}", userId, e);
            throw new RuntimeException("查询用户信息失败", e);
        }
    }

    /**
     * 根据用户名查询用户
     */
    @Override
    public SysUser selectUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("查询用户失败：用户名为空");
            return null;
        }

        try {
            SysUser user = userMapper.selectUserByUsername(username.trim());
            if (user != null) {
                log.debug("查询用户成功：用户名={}, 用户ID={}", username, user.getUserId());
            } else {
                log.debug("查询用户失败：用户名不存在 - {}", username);
            }
            return user;
        } catch (Exception e) {
            log.error("查询用户异常：用户名={}", username, e);
            throw new RuntimeException("查询用户信息失败", e);
        }
    }

    /**
     * 根据条件查询用户列表
     */
    @Override
    public List<SysUser> selectUserList(SysUser user) {
        return searchUsers(new UserListRequest());
    }

    @Override
    public List<SysUser> searchUsers(UserListRequest request) {
        UserListRequest effective = request != null ? request : new UserListRequest();
        SysUser filter = buildFilterFromRequest(effective);
        boolean fetchDirectChildren = effective.getUserId() != null && effective.getUserId() > 0;

        List<SysUser> candidates = fetchDirectChildren
            ? userMapper.selectUserList(filter)
            : selectUsersWithinHierarchy(effective.getRootUserId(), effective.getIdentity(), filter);

        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Iterator<SysUser> iterator = candidates.iterator();
        while (iterator.hasNext()) {
            SysUser user = iterator.next();
            if (!userMatchesIdentity(user, effective.getIdentity())) {
                iterator.remove();
            }
        }

        Set<String> requiredRoles = normalizeRoleKeys(effective.getRoleKeys());
        if (requiredRoles.isEmpty()) {
            candidates.forEach(this::ensureRoleKeysLoaded);
            enrichParentRoles(candidates);
            return candidates;
        }

        Iterator<SysUser> roleIterator = candidates.iterator();
        while (roleIterator.hasNext()) {
            SysUser user = roleIterator.next();
            Set<String> userRoles = ensureRoleKeysLoaded(user);
            if (userRoles == null || userRoles.isEmpty()) {
                roleIterator.remove();
                continue;
            }
            boolean matched = userRoles.stream()
                .filter(Objects::nonNull)
                .map(role -> role.toLowerCase(Locale.ROOT))
                .anyMatch(requiredRoles::contains);
            if (!matched) {
                roleIterator.remove();
            }
        }
        enrichParentRoles(candidates);
        return candidates;
    }

    private void enrichParentRoles(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Set<Long> parentIds = users.stream()
            .map(SysUser::getParentUserId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (parentIds.isEmpty()) {
            return;
        }
        Map<Long, Set<String>> parentRoleMap = fetchRoleKeysByUserIds(parentIds);
        users.forEach(user -> {
            Long parentId = user.getParentUserId();
            if (parentId == null || parentId <= 0) {
                user.setParentRoles(Collections.emptySet());
            } else {
                Set<String> parentRoleKeys = parentRoleMap.getOrDefault(parentId, Collections.emptySet());
                user.setParentRoles(parentRoleKeys);
            }
        });
    }

    /**
     * 在层级范围内查询用户列表
     */
    @Override
    public List<SysUser> selectUsersWithinHierarchy(Long rootUserId, SysUser filter) {
        return selectUsersWithinHierarchy(rootUserId, null, filter);
    }

    @Override
    public List<SysUser> selectUsersWithinHierarchy(Long rootUserId, String identity, SysUser filter) {
        SysUser criteria = filter != null ? filter : new SysUser();
        applyDataPermissionFilter(criteria);

        Set<Long> scope = new LinkedHashSet<>();
        LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        boolean isAdmin = currentUser != null && currentUser.isAdminIdentity();

        if (rootUserId != null && rootUserId > 0) {
            scope.add(rootUserId);
            scope.addAll(hierarchyService.findDescendantIds(rootUserId));
        } else if (currentUser != null) {
            scope.add(currentUser.getUserId());
            scope.addAll(hierarchyService.findDescendantIds(currentUser.getUserId()));
        }

        boolean applyScope = !scope.isEmpty();
        if (applyScope) {
            criteria.addParam("userIds", scope);
        }

        List<SysUser> users = userMapper.selectUserList(criteria);
        List<SysUser> result = new ArrayList<>();
        for (SysUser user : users) {
            if (applyScope && !scope.contains(user.getUserId())) {
                continue;
            }
            if (!userMatchesIdentity(user, identity)) {
                continue;
            }
            result.add(user);
        }
        return result;
    }

    @Override
    public List<UserHierarchyNodeDTO> listAllUserHierarchyRelations() {
        try {
            List<UserHierarchyNodeDTO> relations = fetchAllUserHierarchyRelations();
            log.debug("查询所有用户父子关系成功，记录数={}", relations.size());
            return relations;
        } catch (Exception e) {
            log.error("查询所有用户父子关系失败", e);
            throw new RuntimeException("查询用户层级关系失败", e);
        }
    }

    @Override
    public void rebuildUserHierarchyCache() {
        try {
            List<UserHierarchyNodeDTO> relations = fetchAllUserHierarchyRelations();
            UserHierarchyTreeDTO tree = UserHierarchyTreeBuilder.build(relations);
            redisTemplate.opsForValue().set(UserHierarchyTreeBuilder.USER_TREE_CACHE_KEY, tree);
            log.info("用户层级树缓存刷新成功：记录总数={}, 根节点数量={}",
                    relations.size(), tree.getRootUserIds().size());
        } catch (Exception e) {
            log.error("刷新用户层级树缓存失败", e);
            throw new RuntimeException("刷新用户层级树缓存失败", e);
        }
    }

    private List<UserHierarchyNodeDTO> fetchAllUserHierarchyRelations() {
        return userMapper.selectAllUserHierarchyRelations();
    }

    private void refreshUserHierarchyCacheSilently() {
        try {
            rebuildUserHierarchyCache();
        } catch (Exception e) {
            log.error("刷新用户层级树缓存失败，将在下次用户操作时重试", e);
        }
    }

    @Override
    public List<UserHierarchyGroupDTO> listUsersByLeaderDirectDepts(Long leaderUserId) {
        if (leaderUserId == null || leaderUserId <= 0) {
            log.warn("查询直属用户失败：负责人ID无效 - {}", leaderUserId);
            return Collections.emptyList();
        }

        if (hierarchyService == null) {
            log.warn("用户层级服务未初始化，无法查询直属用户");
            return Collections.emptyList();
        }

        List<Long> directChildren = hierarchyService.findDirectChildren(leaderUserId);
        if (directChildren == null || directChildren.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<Long> userIds = directChildren.stream()
            .filter(Objects::nonNull)
            .filter(id -> id > 0)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysUser> directUsers = userMapper.selectUsersByIds(userIds);
        if (directUsers == null || directUsers.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, SysUser> userIndex = directUsers.stream()
            .filter(Objects::nonNull)
            .filter(user -> user.getUserId() != null)
            .collect(Collectors.toMap(SysUser::getUserId, user -> user, (left, right) -> left, LinkedHashMap::new));

        Map<Long, Set<String>> roleKeysByUser = resolveRoleKeysForUsers(userIds);

        Map<String, UserHierarchyGroupDTO> groups = new LinkedHashMap<>();
        for (Long userId : userIds) {
            SysUser user = userIndex.get(userId);
            if (user == null) {
                continue;
            }
            Set<String> roleKeys = roleKeysByUser.getOrDefault(userId, Collections.emptySet());
            UserIdentity identity = resolvePrimaryIdentity(roleKeys);
            String groupKey = identity != null ? identity.getRoleKey() : "unassigned";

            UserHierarchyGroupDTO group = groups.computeIfAbsent(groupKey, key -> {
                UserHierarchyGroupDTO dto = new UserHierarchyGroupDTO();
                dto.setIdentityKey(key);
                dto.setIdentityLabel(resolveIdentityLabel(identity, key));
                return dto;
            });
            group.getUsers().add(buildHierarchyUserSummary(user, roleKeys, identity));
        }

        return new ArrayList<>(groups.values());
    }

    private Map<Long, Set<String>> resolveRoleKeysForUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> idSet = userIds instanceof Set ? (Set<Long>) userIds : new LinkedHashSet<>(userIds);
        List<Map<String, Object>> rows = userMapper.selectUserRoleMappings(idSet);
        Map<Long, Set<String>> roleKeysByUser = new LinkedHashMap<>();
        if (rows == null || rows.isEmpty()) {
            return roleKeysByUser;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Long userId = parseLong(row.get("userId"));
            Object roleKeyObj = row.get("roleKey");
            if (userId == null || !(roleKeyObj instanceof String)) {
                continue;
            }
            String roleKey = ((String) roleKeyObj).trim();
            if (roleKey.isEmpty()) {
                continue;
            }
            roleKeysByUser
                .computeIfAbsent(userId, key -> new LinkedHashSet<>())
                .add(roleKey);
        }
        return roleKeysByUser;
    }

    private Map<Long, Set<String>> fetchRoleKeysByUserIds(Set<Long> userIds) {
        return resolveRoleKeysForUsers(userIds);
    }

    private UserHierarchyGroupDTO.UserSummary buildHierarchyUserSummary(SysUser user,
                                                                       Collection<String> roleKeys,
                                                                       UserIdentity identity) {
        UserHierarchyGroupDTO.UserSummary summary = new UserHierarchyGroupDTO.UserSummary();
        summary.setUserId(user.getUserId());
        summary.setUsername(user.getUsername());
        summary.setNickname(user.getNickname());
        summary.setRealName(user.getRealName());
        summary.setPhone(user.getPhone());
        summary.setEmail(user.getEmail());
        summary.setStatus(user.getStatus());
        summary.setParentUserId(user.getParentUserId());
        Set<String> roles = roleKeys == null ? Collections.emptySet() : new LinkedHashSet<>(roleKeys);
        summary.setRoleKeys(roles);
        summary.setPrimaryIdentity(identity != null ? identity.getRoleKey() : "unassigned");
        return summary;
    }

    private UserIdentity resolvePrimaryIdentity(Collection<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return null;
        }
        List<UserIdentity> priority = Arrays.asList(
            UserIdentity.ADMIN,
            UserIdentity.AGENT_LEVEL_1,
            UserIdentity.AGENT_LEVEL_2,
            UserIdentity.AGENT_LEVEL_3,
            UserIdentity.BUYER_MAIN,
            UserIdentity.BUYER_SUB
        );
        for (UserIdentity identity : priority) {
            if (UserRoleUtils.hasIdentity(roleKeys, identity)) {
                return identity;
            }
        }
        return null;
    }

    private String resolveIdentityLabel(UserIdentity identity, String fallbackKey) {
        if (identity == null) {
            return "未分配身份";
        }
        switch (identity) {
            case ADMIN:
                return "超级管理员";
            case AGENT_LEVEL_1:
                return "总代";
            case AGENT_LEVEL_2:
                return "一级代理";
            case AGENT_LEVEL_3:
                return "二级代理";
            case BUYER_MAIN:
                return "买家总账号";
            case BUYER_SUB:
                return "买家子账号";
            default:
                return fallbackKey != null ? fallbackKey : "未知身份";
        }
    }

    private Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean hasUserHierarchyPermission(Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0) {
            return false;
        }
        LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        if (currentUser == null) {
            return false;
        }
        if (currentUser.isAdminIdentity()) {
            return true;
        }
        if (Objects.equals(currentUser.getUserId(), targetUserId)) {
            return true;
        }
        return hierarchyService.isAncestor(currentUser.getUserId(), targetUserId);
    }

    // ==================== CUD操作方法 ====================

    /**
     * 创建新用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser insertUser(SysUser user) throws Exception {
        normalizeOptionalFields(user);
        // 参数验证
        validateUserForInsert(user);

        // 验证用户创建权限（基于部门类型的权限控制）
        validateUserCreatePermission(user);

        try {
            // 设置默认值
            setDefaultValues(user);

            // 密码加密
            String encryptedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encryptedPassword);

            // 插入用户
            int result = userMapper.insertUser(user);
            if (result <= 0) {
                throw new RuntimeException("创建用户失败：数据库操作失败");
            }

            // MyBatis 在部分驱动上不会自动回填主键，手动兜底查询一次
            if (user.getUserId() == null) {
                SysUser persistedUser = userMapper.selectUserByUsername(user.getUsername());
                if (persistedUser != null) {
                    user.setUserId(persistedUser.getUserId());
                }
            }

            persistRequestedRoles(user);

            // 设置简化的角色和部门信息
            setSimplifiedUserInfo(user);

            log.info("创建用户成功：用户ID={}, 用户名={}, 创建者={}",
                    user.getUserId(), user.getUsername(), user.getCreateBy());
            refreshUserHierarchyCacheSilently();

            return user;
        } catch (Exception e) {
            log.error("创建用户异常：用户名={}", user.getUsername(), e);
            throw new RuntimeException("创建用户失败：" + e.getMessage(), e);
        }
    }

    /**
     * 更新用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUser user) throws Exception {
        // 参数验证
        validateUserForUpdate(user);

        // 检查数据权限
        if (!hasUserDataPermission(user.getUserId())) {
            throw new RuntimeException("无权限修改该用户信息");
        }

        try {
            // 清除敏感字段
            user.setPassword(null);
            user.setUsername(null); // 用户名通常不允许修改

            int result = userMapper.updateUser(user);
            if (result > 0) {
                log.info("更新用户成功：用户ID={}, 更新者={}", user.getUserId(), user.getUpdateBy());
                refreshUserHierarchyCacheSilently();
                return true;
            } else {
                log.warn("更新用户失败：用户不存在或无变更 - {}", user.getUserId());
                return false;
            }
        } catch (Exception e) {
            log.error("更新用户异常：用户ID={}", user.getUserId(), e);
            throw new RuntimeException("更新用户失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserById(Long userId) throws Exception {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户ID无效");
        }

        // 检查数据权限
        if (!hasUserDataPermission(userId)) {
            throw new RuntimeException("无权限删除该用户");
        }

        // 检查用户是否存在
        SysUser user = selectUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查是否为超级管理员
        if (user.isAdmin()) {
            throw new RuntimeException("不能删除超级管理员");
        }

        try {
            // 删除用户角色关联
            userMapper.deleteUserRoles(userId);

            // 删除用户
            int result = userMapper.deleteUserById(userId);
            if (result > 0) {
                log.info("删除用户成功：用户ID={}, 用户名={}", userId, user.getUsername());
                refreshUserHierarchyCacheSilently();
                return true;
            } else {
                log.warn("删除用户失败：用户ID={}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("删除用户异常：用户ID={}", userId, e);
            throw new RuntimeException("删除用户失败：" + e.getMessage(), e);
        }
    }

    /**
     * 批量删除用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserByIds(List<Long> userIds) throws Exception {
        if (userIds == null || userIds.isEmpty()) {
            throw new RuntimeException("用户ID列表为空");
        }

        // 过滤无效ID并检查权限
        List<Long> validIds = userIds.stream()
                .filter(id -> id != null && id > 0)
                .filter(this::hasUserDataPermission)
                .collect(Collectors.toList());

        if (validIds.isEmpty()) {
            throw new RuntimeException("没有可删除的有效用户");
        }

        try {
            // 批量删除用户角色关联
            for (Long userId : validIds) {
                userMapper.deleteUserRoles(userId);
            }

            // 批量删除用户
            int result = userMapper.deleteUserByIds(validIds);
            if (result > 0) {
                log.info("批量删除用户成功：删除数量={}, 用户IDs={}", result, validIds);
                refreshUserHierarchyCacheSilently();
                return true;
            } else {
                log.warn("批量删除用户失败：用户IDs={}", validIds);
                return false;
            }
        } catch (Exception e) {
            log.error("批量删除用户异常：用户IDs={}", validIds, e);
            throw new RuntimeException("批量删除用户失败：" + e.getMessage(), e);
        }
    }

    // ==================== 密码管理方法 ====================

    /**
     * 重置用户密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId, String newPassword) throws Exception {
        // 参数验证
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户ID无效");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("新密码不能为空");
        }

        // 检查数据权限
        if (!hasUserDataPermission(userId)) {
            throw new RuntimeException("无权限重置该用户密码");
        }

        // 密码强度验证
        validatePasswordStrength(newPassword);

        try {
            String encryptedPassword = passwordEncoder.encode(newPassword);
            int result = userMapper.updateUserPassword(userId, encryptedPassword);

            if (result > 0) {
                log.info("重置用户密码成功：用户ID={}, 操作者={}", userId, SecurityUtils.getCurrentUsername());
                return true;
            } else {
                log.warn("重置用户密码失败：用户不存在 - {}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("重置用户密码异常：用户ID={}", userId, e);
            throw new RuntimeException("重置密码失败：" + e.getMessage(), e);
        }
    }

    /**
     * 修改用户密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long userId, String oldPassword, String newPassword) throws Exception {
        // 参数验证
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户ID无效");
        }
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new RuntimeException("原密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("新密码不能为空");
        }

        // 检查是否为自己修改密码
        LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        if (currentUser == null || !userId.equals(currentUser.getUserId())) {
            throw new RuntimeException("只能修改自己的密码");
        }

        // 获取用户信息
        SysUser user = selectUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 验证新密码强度
        validatePasswordStrength(newPassword);

        // 检查新密码是否与原密码相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("新密码不能与原密码相同");
        }

        try {
            String encryptedPassword = passwordEncoder.encode(newPassword);
            int result = userMapper.updateUserPassword(userId, encryptedPassword);

            if (result > 0) {
                log.info("修改用户密码成功：用户ID={}", userId);
                return true;
            } else {
                log.warn("修改用户密码失败：用户ID={}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("修改用户密码异常：用户ID={}", userId, e);
            throw new RuntimeException("修改密码失败：" + e.getMessage(), e);
        }
    }

    // ==================== 角色权限管理方法 ====================

    /**
     * 分配用户角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignUserRoles(Long userId, List<Long> roleIds) throws Exception {
        // 参数验证
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户ID无效");
        }
        if (roleIds == null) {
            roleIds = new ArrayList<>();
        }

        // 检查数据权限
        if (!hasUserDataPermission(userId)) {
            throw new RuntimeException("无权限修改该用户角色");
        }

        try {
            // 清除原有角色
            userMapper.deleteUserRoles(userId);

            // 分配新角色
            if (!roleIds.isEmpty()) {
                int result = userMapper.assignUserRoles(userId, roleIds);
                if (result <= 0) {
                    throw new RuntimeException("分配角色失败");
                }
            }

            log.info("分配用户角色成功：用户ID={}, 角色数量={}", userId, roleIds.size());
            return true;
        } catch (Exception e) {
            log.error("分配用户角色异常：用户ID={}, 角色IDs={}", userId, roleIds, e);
            throw new RuntimeException("分配角色失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取用户角色ID列表
     */
    @Override
    public List<Long> getUserRoleIds(Long userId) {
        if (userId == null || userId <= 0) {
            return new ArrayList<>();
        }

        try {
            return userMapper.selectUserRoleIds(userId);
        } catch (Exception e) {
            log.error("获取用户角色ID异常：用户ID={}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取用户权限标识集合
     */
    @Override
    public Set<String> getUserPermissions(Long userId) {
        if (userId == null || userId <= 0) {
            return new HashSet<>();
        }

        try {
            return userMapper.selectPermissionsByUserId(userId);
        } catch (Exception e) {
            log.error("获取用户权限异常：用户ID={}", userId, e);
            return new HashSet<>();
        }
    }

    /**
     * 获取用户角色标识集合
     */
    @Override
    public Set<String> getUserRoles(Long userId) {
        if (userId == null || userId <= 0) {
            return new HashSet<>();
        }

        try {
            return userMapper.selectRoleKeysByUserId(userId);
        } catch (Exception e) {
            log.error("获取用户角色异常：用户ID={}", userId, e);
            return new HashSet<>();
        }
    }

    // ==================== 认证相关方法 ====================

    /**
     * 根据用户ID获取登录用户信息
     */
    @Override
    public LoginUser selectLoginUserById(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        try {
            // 查询用户信息
            SysUser user = userMapper.selectUserById(userId);
            if (user == null) {
                return null;
            }

            // 查询用户角色和权限
            Set<String> roles = getUserRoles(userId);
            Set<String> permissions = getUserPermissions(userId);

            // 构建LoginUser对象
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(user.getUserId());
            loginUser.setUsername(user.getUsername());
            loginUser.setDeptId(user.getDeptId());
            loginUser.setRoles(roles);
            loginUser.setPermissions(permissions);

            // 设置其他必要的字段
            loginUser.setNickname(user.getNickname());
            loginUser.setEmail(user.getEmail());
            loginUser.setPhone(user.getPhone());

            // 注意：LoginUser会自动计算isAdmin()，无需手动设置

            return loginUser;
        } catch (Exception e) {
            log.error("根据用户ID获取登录用户信息失败：用户ID={}", userId, e);
            return null;
        }
    }

    @Override
    public com.deepreach.common.core.domain.vo.UserVO getCompleteUserInfo(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        try {
            // 查询用户基础信息
            com.deepreach.common.core.domain.vo.UserVO userVO = userMapper.selectCompleteUserInfo(userId);
            if (userVO == null) {
                return null;
            }

            // 获取用户角色和权限
            Set<String> roles = getUserRoles(userId);
            Set<String> permissions = getUserPermissions(userId);

            // 设置角色和权限信息
            userVO.setRoles(roles);
            userVO.setPermissions(permissions);

            return userVO;
        } catch (Exception e) {
            log.error("获取用户完整信息失败，用户ID：{}", userId, e);
            return null;
        }
    }

    /**
     * 用户登录认证
     */
    @Override
    public LoginUser authenticate(String username, String password, String loginIp) throws Exception {
        // 参数验证
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        try {
            // 查询用户信息
            SysUser user = userMapper.selectUserWithRolesAndPermissions(username.trim());
            if (user == null) {
                throw new RuntimeException("用户名或密码错误");
            }

            // 检查用户状态
            if (!user.isNormal()) {
                throw new RuntimeException("用户账号已被停用");
            }

            // 验证密码
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new RuntimeException("用户名或密码错误");
            }

            // 获取用户角色和权限
            Set<String> roles = getUserRoles(user.getUserId());
            Set<String> permissions = getUserPermissions(user.getUserId());

            // 构建登录用户对象
            LoginUser loginUser = LoginUser.fromSysUser(user, roles, permissions);
            loginUser.setIpaddr(loginIp);
            loginUser.setLoginTime(LocalDateTime.now());

            // 设置基于部门类型的字段
            // LoginUser.fromSysUser方法中已经处理了部门类型的字段设置
            // 这里只需要设置父用户信息（如果有的话）
            if (user.getParentUserId() != null) {
                loginUser.setParentUserId(user.getParentUserId());
            }

            // 记录登录信息
            recordLoginInfo(user.getUserId(), loginIp, null);

            log.info("用户登录成功：用户ID={}, 用户名={}, 登录IP={}",
                    user.getUserId(), user.getUsername(), loginIp);

            return loginUser;
        } catch (RuntimeException e) {
            log.warn("用户登录失败：用户名={}, 登录IP={}, 失败原因={}",
                    username, loginIp, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("用户登录异常：用户名={}", username, e);
            throw new RuntimeException("登录失败，请稍后重试", e);
        }
    }

    /**
     * 用户注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser register(SysUser user) throws Exception {
        normalizeOptionalFields(user);
        // 参数验证
        validateUserForRegister(user);

        try {
            // 设置默认值
            setDefaultValuesForRegister(user);

            // 密码加密
            String encryptedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encryptedPassword);

            // 插入用户
            int result = userMapper.insertUser(user);
            if (result <= 0) {
                throw new RuntimeException("注册失败：数据库操作失败");
            }

            persistRequestedRoles(user);

            log.info("用户注册成功：用户ID={}, 用户名={}", user.getUserId(), user.getUsername());
            refreshUserHierarchyCacheSilently();
            return user;
        } catch (Exception e) {
            log.error("用户注册异常：用户名={}", user.getUsername(), e);
            throw new RuntimeException("注册失败：" + e.getMessage(), e);
        }
    }

    // ==================== 唯一性检查方法 ====================

    /**
     * 检查用户名是否唯一
     */
    @Override
    public boolean checkUsernameUnique(String username, Long userId) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        try {
            int count = userMapper.checkUsernameUnique(username.trim(), userId);
            return count == 0;
        } catch (Exception e) {
            log.error("检查用户名唯一性异常：用户名={}", username, e);
            return false;
        }
    }

    /**
     * 检查邮箱是否唯一
     */
    @Override
    public boolean checkEmailUnique(String email, Long userId) {
        if (email == null || email.trim().isEmpty()) {
            return true; // 邮箱为空时认为唯一
        }

        try {
            int count = userMapper.checkEmailUnique(email.trim(), userId);
            return count == 0;
        } catch (Exception e) {
            log.error("检查邮箱唯一性异常：邮箱={}", email, e);
            return false;
        }
    }

    /**
     * 检查手机号是否唯一
     */
    @Override
    public boolean checkPhoneUnique(String phone, Long userId) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // 手机号为空时认为唯一
        }

        try {
            int count = userMapper.checkPhoneUnique(phone.trim(), userId);
            return count == 0;
        } catch (Exception e) {
            log.error("检查手机号唯一性异常：手机号={}", phone, e);
            return false;
        }
    }

    // ==================== 其他业务方法 ====================

    /**
     * 记录用户登录信息
     *
     * 记录用户的最后登录信息，并手动记录登录操作日志
     * 由于数据库权限限制，不再使用触发器，改为代码层面记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginInfo(Long userId, String loginIp, String loginLocation) {
        if (userId == null || userId <= 0) {
            return;
        }

        try {
            // 获取用户信息
            SysUser user = selectUserById(userId);
            if (user == null) {
                log.warn("记录登录信息失败：用户不存在 - {}", userId);
                return;
            }

            // 更新用户最后登录信息
            userMapper.updateUserLoginInfo(userId, loginIp, LocalDateTime.now());

            // 手动记录登录操作日志
            recordLoginLog(user, loginIp, loginLocation);

            log.debug("记录用户登录信息成功：用户ID={}, 用户名={}, 登录IP={}",
                     userId, user.getUsername(), loginIp);
        } catch (Exception e) {
            log.error("记录用户登录信息异常：用户ID={}", userId, e);
        }
    }

    /**
     * 记录用户登录操作日志
     *
     * 手动记录用户的登录操作到操作日志表
     * 替代原来的数据库触发器功能
     *
     * @param user 登录用户信息
     * @param loginIp 登录IP地址
     * @param loginLocation 登录地点
     */
    private void recordLoginLog(SysUser user, String loginIp, String loginLocation) {
        try {
            // 使用SysOperLog的静态方法创建登录日志
            Integer operatorType = user.isBackendUser() ? 1 : 2;
            SysOperLog operLog = SysOperLog.createLoginLog(user.getUsername(), loginIp, operatorType);

            // 设置额外信息
            operLog.setDeptName(user.getDept() != null ? user.getDept().getDeptName() : "");
            operLog.setOperLocation(loginLocation != null ? loginLocation : "");
            operLog.setOperUrl("/auth/login"); // 登录接口URL

            // 这里应该调用操作日志Service记录日志
            // operLogService.insertOperlog(operLog);

            // 暂时使用应用日志记录，后续可以集成专门的操作日志服务
            log.info("用户登录日志 - 用户ID: {}, 用户名: {}, 部门: {}, 登录IP: {}, 登录地点: {}, 登录时间: {}",
                    user.getUserId(), user.getUsername(),
                    operLog.getDeptName(), loginIp, operLog.getOperLocation(), LocalDateTime.now());

        } catch (Exception e) {
            // 记录登录日志失败不应该影响登录流程
            log.error("记录登录日志异常：用户ID={}, 用户名={}",
                     user.getUserId(), user.getUsername(), e);
        }
    }

    /**
     * 更新用户状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserStatus(Long userId, String status) throws Exception {
        // 参数验证
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户ID无效");
        }
        if (status == null || (!"0".equals(status) && !"1".equals(status))) {
            throw new RuntimeException("状态值无效");
        }

        // 检查数据权限
        if (!hasUserDataPermission(userId)) {
            throw new RuntimeException("无权限修改该用户状态");
        }

        // 检查是否为超级管理员
        if (userId == 1L) {
            throw new RuntimeException("不能修改超级管理员状态");
        }

        try {
            int result = userMapper.updateUserStatus(userId, status);
            if (result > 0) {
                log.info("更新用户状态成功：用户ID={}, 状态={}, 操作者={}",
                        userId, status, SecurityUtils.getCurrentUsername());
                refreshUserHierarchyCacheSilently();
                return true;
            } else {
                log.warn("更新用户状态失败：用户不存在 - {}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("更新用户状态异常：用户ID={}, 状态={}", userId, status, e);
            throw new RuntimeException("更新用户状态失败：" + e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证用户插入参数
     *
     * 基于部门类型的用户验证，确保用户必须归属于某个部门
     */
    private void validateUserForInsert(SysUser user) throws Exception {
        if (user == null) {
            throw new RuntimeException("用户信息不能为空");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        // 用户名格式验证
        String username = user.getUsername().trim();
        if (username.length() < 3 || username.length() > 20) {
            throw new RuntimeException("用户名长度必须在3-20个字符之间");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("用户名只能包含字母、数字和下划线");
        }

        // 唯一性检查
        if (!checkUsernameUnique(username, null)) {
            throw new RuntimeException("用户名已存在");
        }
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            if (!checkEmailUnique(user.getEmail(), null)) {
                throw new RuntimeException("邮箱已被使用");
            }
        }
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            if (!checkPhoneUnique(user.getPhone(), null)) {
                throw new RuntimeException("手机号已被使用");
            }
        }

        // 密码强度验证
        validatePasswordStrength(user.getPassword());
    }

    /**
     * 验证用户更新参数
     */
    private void validateUserForUpdate(SysUser user) throws Exception {
        if (user == null || user.getUserId() == null || user.getUserId() <= 0) {
            throw new RuntimeException("用户ID无效");
        }

        // 检查用户是否存在
        SysUser existingUser = selectUserById(user.getUserId());
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 邮箱唯一性检查
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            if (!checkEmailUnique(user.getEmail(), user.getUserId())) {
                throw new RuntimeException("邮箱已被使用");
            }
        }

        // 手机号唯一性检查
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            if (!checkPhoneUnique(user.getPhone(), user.getUserId())) {
                throw new RuntimeException("手机号已被使用");
            }
        }
    }

    /**
     * 验证用户注册参数
     */
    private void validateUserForRegister(SysUser user) throws Exception {
        if (user == null) {
            throw new RuntimeException("用户信息不能为空");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("邮箱不能为空");
        }

        // 使用插入验证逻辑
        validateUserForInsert(user);
    }

    /**
     * 验证密码强度
     */
    private void validatePasswordStrength(String password) throws Exception {
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        String trimmedPassword = password.trim();
        if (trimmedPassword.length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }
        if (trimmedPassword.length() > 20) {
            throw new RuntimeException("密码长度不能超过20位");
        }

        // 可以根据需要添加更复杂的密码强度验证规则
        // 例如：必须包含大小写字母、数字、特殊字符等
    }

    /**
     * 设置用户默认值
     */
    private void setDefaultValues(SysUser user) {
        if (user.getStatus() == null) {
            user.setStatus("0"); // 默认正常状态
        }

        if (user.getUserType() == null) {
            user.setUserType(user.getParentUserId() != null ? 2 : 1);
        }

        if (user.getGender() == null) {
            user.setGender("2"); // 默认未知性别
        }
        if (user.getCreateTime() == null) {
            user.setCreateTime(LocalDateTime.now());
        }

        // 当未指定父用户时，默认视为顶级账号；无自动父子关联逻辑
    }

    private void normalizeOptionalFields(SysUser user) {
        if (user == null) {
            return;
        }
        if (user.getEmail() != null) {
            String email = user.getEmail().trim();
            user.setEmail(email.isEmpty() ? null : email);
        }
        if (user.getPhone() != null) {
            String phone = user.getPhone().trim();
            user.setPhone(phone.isEmpty() ? null : phone);
        }
        if (user.getNickname() != null) {
            String nickname = user.getNickname().trim();
            user.setNickname(nickname.isEmpty() ? null : nickname);
        }
        if (user.getRealName() != null) {
            String realName = user.getRealName().trim();
            user.setRealName(realName.isEmpty() ? null : realName);
        }
        if (user.getInvitationCode() != null) {
            String code = user.getInvitationCode().trim();
            user.setInvitationCode(code.isEmpty() ? null : code);
        }
    }

    /**
     * 验证买家子账户的父用户
     */
    /**
     * 验证用户创建权限（基于部门类型的权限控制）
     *
     * @param user 要创建的用户
     * @throws Exception 如果没有权限则抛出异常
     */
    public void validateUserCreatePermission(SysUser user) throws Exception {
        UserCreationContext context = buildCreationContext(user);
        validateCreationPermissions(user, context);
    }

    private UserCreationContext buildCreationContext(SysUser user) {
        LoginUser creator = SecurityUtils.getCurrentLoginUser();
        if (creator == null) {
            throw new RuntimeException("用户未登录");
        }

        Set<String> requestedRoleKeys = normalizeRoleKeySet(user.getRoles());
        if (requestedRoleKeys.isEmpty()) {
            throw new RuntimeException("创建用户必须指定角色身份");
        }
        user.setRoles(requestedRoleKeys);

        Set<UserIdentity> targetIdentities = new HashSet<>(UserRoleUtils.resolveIdentities(requestedRoleKeys));
        if (targetIdentities.isEmpty()) {
            throw new RuntimeException("无法解析用户身份，请检查角色配置");
        }
        if (targetIdentities.size() > 1) {
            throw new RuntimeException("暂不支持同时创建多个身份的用户");
        }
        UserIdentity targetIdentity = targetIdentities.iterator().next();
        if (UserIdentity.ADMIN.equals(targetIdentity)) {
            throw new RuntimeException("禁止创建管理员账号");
        }

        Long parentId = user.getParentUserId();
        if (parentId == null || parentId <= 0) {
            parentId = creator.getUserId();
            user.setParentUserId(parentId);
        }

        SysUser parentUser = selectUserById(parentId);
        if (parentUser == null) {
            throw new RuntimeException("父用户不存在");
        }

        Set<String> parentRoleKeys = userMapper.selectRoleKeysByUserId(parentUser.getUserId());
        if (parentRoleKeys == null || parentRoleKeys.isEmpty()) {
            throw new RuntimeException("父用户未配置身份，无法创建下级用户");
        }
        parentUser.setRoles(parentRoleKeys);

        Set<UserIdentity> creatorIdentities = new HashSet<>(UserRoleUtils.resolveIdentities(
            Optional.ofNullable(creator.getRoles()).orElse(Collections.emptySet())));
        Set<UserIdentity> parentIdentities = new HashSet<>(UserRoleUtils.resolveIdentities(parentRoleKeys));

        return new UserCreationContext(creator, parentUser, targetIdentity, creatorIdentities, parentIdentities);
    }

    private void validateCreationPermissions(SysUser newUser, UserCreationContext context) {
        Set<UserIdentity> allowedByParent = allowedChildIdentities(context.parentIdentities);
        if (!allowedByParent.contains(context.targetIdentity)) {
            throw new RuntimeException("父用户身份不允许创建该类型的子用户");
        }

        boolean creatorIsAdmin = context.creatorIdentities.contains(UserIdentity.ADMIN);

        if (!creatorIsAdmin) {
            Long creatorId = context.creator.getUserId();
            Long parentId = context.parentUser.getUserId();

            if (!Objects.equals(creatorId, parentId)
                && !hierarchyService.isAncestor(creatorId, parentId)) {
                throw new RuntimeException("只能在自己管理的用户树下创建用户");
            }

            if (!Objects.equals(creatorId, parentId)
                && context.parentIdentities.contains(UserIdentity.BUYER_MAIN)) {
                throw new RuntimeException("商户的子用户仅能由管理员代为创建");
            }
        }

        // 防止创建超级管理员账号
        if (newUser.getUserId() != null && Objects.equals(newUser.getUserId(), 1L)) {
            throw new RuntimeException("不能创建超级管理员账号");
        }
    }

    private Set<UserIdentity> allowedChildIdentities(Set<UserIdentity> parentIdentities) {
        if (parentIdentities == null || parentIdentities.isEmpty()) {
            return Collections.emptySet();
        }
        EnumSet<UserIdentity> result = EnumSet.allOf(UserIdentity.class);
        result.remove(UserIdentity.ADMIN);
        return result;
    }

    private boolean isAgentIdentity(Set<UserIdentity> identities) {
        if (identities == null || identities.isEmpty()) {
            return false;
        }
        for (UserIdentity identity : identities) {
            if (AGENT_IDENTITIES.contains(identity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查代理用户是否可以创建下级代理
     *
     * @param user 当前用户
     * @return true如果可以创建，false否则
     */
    public boolean canCreateSubAgent(com.deepreach.common.core.domain.model.LoginUser user) {
        if (user == null || !user.hasRole("AGENT")) {
            return false;
        }

        Integer currentLevel = user.getDept() != null ? user.getDept().getLevel() : null;
        return currentLevel != null && currentLevel < 3; // 1-2级代理可以创建下级代理
    }

    /**
     * 检查代理用户是否可以创建买家总账户
     *
     * @param user 当前用户
     * @return true如果可以创建，false否则
     */
    public boolean canCreateBuyerAccount(com.deepreach.common.core.domain.model.LoginUser user) {
        if (user == null) {
            return false;
        }

        // 系统管理员角色可以创建买家总账户
        if (user.hasRole("ADMIN") || user.hasRole("SYSTEM_ADMIN") ||
            user.hasRole("TECH_ADMIN") || user.hasRole("OPS_ADMIN")) {
            return true;
        }

        // 所有代理角色都可以创建买家总账户（不论层级）
        if (user.hasRole("AGENT")) {
            return true;
        }

        return false;
    }

    /**
     * 检查买家总账户用户是否可以创建子账户
     *
     * @param user 当前用户
     * @return true如果可以创建，false否则
     */
    public boolean canCreateSubAccount(com.deepreach.common.core.domain.model.LoginUser user) {
        if (user == null || !user.hasRole("BUYER_MAIN")) {
            return false;
        }

        // 买家总账户都可以创建子账户
        return true;
    }

    /**
     * 设置注册用户默认值
     */
    private void setDefaultValuesForRegister(SysUser user) {
        setDefaultValues(user);
        user.setUserType(2); // 注册用户默认为客户端用户
    }

    /**
     * 应用数据权限过滤
     */
    private void applyDataPermissionFilter(SysUser user) {
        // TODO: 后续基于用户树的数据权限过滤
    }

    private SysUser buildFilterFromRequest(UserListRequest request) {
        SysUser filter = new SysUser();
        if (request == null) {
            return filter;
        }

        Long parentId = request.getUserId();
        if (parentId != null && parentId > 0) {
            filter.setParentUserId(parentId);
        }

        String username = StringUtils.trimToNull(request.getUsername());
        if (username != null) {
            filter.setUsername(username);
        }
        String nickname = StringUtils.trimToNull(request.getNickname());
        if (nickname != null) {
            filter.setNickname(nickname);
        }
        String phone = StringUtils.trimToNull(request.getPhone());
        if (phone != null) {
            filter.setPhone(phone);
        }
        String email = StringUtils.trimToNull(request.getEmail());
        if (email != null) {
            filter.setEmail(email);
        }
        String status = StringUtils.trimToNull(request.getStatus());
        if (status != null) {
            filter.setStatus(status);
        }
        if (request.getUserType() != null) {
            filter.setUserType(request.getUserType());
        }

        String beginTime = StringUtils.trimToNull(request.getBeginTime());
        if (beginTime != null) {
            filter.addParam("beginTime", beginTime);
        }
        String endTime = StringUtils.trimToNull(request.getEndTime());
        if (endTime != null) {
            filter.addParam("endTime", endTime);
        }
        String keyword = StringUtils.trimToNull(request.getKeyword());
        if (keyword != null) {
            filter.addParam("keyword", keyword);
        }

        if (request.getRoleKeys() != null && !request.getRoleKeys().isEmpty()) {
            filter.addParam("roleKeys", new LinkedHashSet<>(request.getRoleKeys()));
        }

        return filter;
    }

    private Set<String> normalizeRoleKeys(List<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return Collections.emptySet();
        }
        return roleKeys.stream()
            .filter(Objects::nonNull)
            .map(key -> StringUtils.trimToNull(key))
            .filter(Objects::nonNull)
            .map(key -> key.toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizeRoleKeySet(Collection<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return roleKeys.stream()
            .map(StringUtils::trimToNull)
            .filter(Objects::nonNull)
            .map(key -> key.toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean userMatchesIdentity(SysUser user, String identity) {
        String normalized = StringUtils.trimToNull(identity);
        if (normalized == null) {
            return true;
        }

        normalized = normalized.toLowerCase(Locale.ROOT);
        Set<String> roles = ensureRoleKeysLoaded(user);

        switch (normalized) {
            case "1":
            case "admin":
            case "system":
            case "system_admin":
                return UserRoleUtils.hasIdentity(roles, UserIdentity.ADMIN);
            case "2":
            case "agent":
            case "agent_level":
                return UserRoleUtils.hasAnyIdentity(roles,
                    UserIdentity.AGENT_LEVEL_1,
                    UserIdentity.AGENT_LEVEL_2,
                    UserIdentity.AGENT_LEVEL_3);
            case "agent_level_1":
                return UserRoleUtils.hasIdentity(roles, UserIdentity.AGENT_LEVEL_1);
            case "agent_level_2":
                return UserRoleUtils.hasIdentity(roles, UserIdentity.AGENT_LEVEL_2);
            case "agent_level_3":
                return UserRoleUtils.hasIdentity(roles, UserIdentity.AGENT_LEVEL_3);
            case "3":
            case "buyer":
            case "buyer_main":
                return UserRoleUtils.hasIdentity(roles, UserIdentity.BUYER_MAIN);
            case "4":
            case "buyer_sub":
            case "sub_buyer":
                return UserRoleUtils.hasIdentity(roles, UserIdentity.BUYER_SUB);
            default:
                return true;
        }
    }

    private Set<String> ensureRoleKeysLoaded(SysUser user) {
        Set<String> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = userMapper.selectRoleKeysByUserId(user.getUserId());
            if (roles == null) {
                roles = Collections.emptySet();
            }
            user.setRoles(roles);
        }
        return roles;
    }

    /**
     * 获取查询条件描述
     */
    private String getQueryCondition(SysUser user) {
        if (user == null) {
            return "无条件";
        }

        StringBuilder sb = new StringBuilder();
        if (user.getUsername() != null) {
            sb.append("用户名:").append(user.getUsername()).append(",");
        }
        if (user.getNickname() != null) {
            sb.append("昵称:").append(user.getNickname()).append(",");
        }
        if (user.getStatus() != null) {
            sb.append("状态:").append(user.getStatus()).append(",");
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "无条件";
    }

    /**
     * 检查用户数据权限
     */
    @Override
    public boolean hasUserDataPermission(Long targetUserId) {
        LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        if (currentUser == null) {
            return false;
        }
        if (currentUser.isAdminIdentity()) {
            return true;
        }
        if (Objects.equals(targetUserId, currentUser.getUserId())) {
            return true;
        }
        return hierarchyService.isAncestor(currentUser.getUserId(), targetUserId);
    }

    // ==================== 其他业务方法实现 ====================

    @Override
    public java.util.Map<String, Object> getUserStatistics(Long userId) {
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        // TODO: 实现用户统计逻辑
        return statistics;
    }

    @Override
    public List<Long> getAccessibleUserIds() {
        // TODO: 实现可访问用户ID获取逻辑
        return new ArrayList<>();
    }

    @Override
    public boolean updateUserAvatar(Long userId, String avatarUrl) throws Exception {
        // TODO: 实现头像更新逻辑
        return false;
    }

    @Override
    public boolean updateUserInfo(Long userId, SysUser user) throws Exception {
        // TODO: 实现用户信息更新逻辑
        return false;
    }

    @Override
    public java.util.Map<String, Object> importUsers(List<SysUser> users, boolean updateSupport) throws Exception {
        // TODO: 实现用户导入逻辑
        return new java.util.HashMap<>();
    }

    @Override
    public byte[] exportUsers(List<SysUser> users) throws Exception {
        // TODO: 实现用户导出逻辑
        return new byte[0];
    }

    @Override
    public boolean isUserOnline(Long userId) {
        // TODO: 实现用户在线状态检查逻辑
        return false;
    }

    @Override
    public boolean forceUserOffline(Long userId) throws Exception {
        // TODO: 实现强制用户下线逻辑
        return false;
    }

    // ==================== 账户体系业务方法实现 ====================

    
    
    /**
     * 根据父用户ID查询子账号列表
     */
    @Override
    public List<SysUser> selectSubAccountsByParentUserId(Long parentUserId) {
        if (parentUserId == null || parentUserId <= 0) {
            log.warn("查询子账号失败：父用户ID无效 - {}", parentUserId);
            return new ArrayList<>();
        }

        try {
            List<SysUser> userList = userMapper.selectSubAccountsByParentUserId(parentUserId);
            log.debug("查询子账号成功：父用户ID={}, 结果数量={}", parentUserId, userList.size());
            return userList;
        } catch (Exception e) {
            log.error("查询子账号异常：父用户ID={}", parentUserId, e);
            throw new RuntimeException("查询子账号失败", e);
        }
    }

    /**
     * 查询下级用户列表（根据父用户ID）
     */
    @Override
    public List<SysUser> selectSubUsersByParentId(Long parentId) {
        if (parentId == null || parentId <= 0) {
            log.warn("查询下级用户失败：父用户ID无效 - {}", parentId);
            return new ArrayList<>();
        }

        try {
            List<SysUser> userList = userMapper.selectSubUsersByParentId(parentId);
            log.debug("查询下级用户成功：父用户ID={}, 结果数量={}", parentId, userList.size());
            return userList;
        } catch (Exception e) {
            log.error("查询下级用户异常：父用户ID={}", parentId, e);
            throw new RuntimeException("查询下级用户失败", e);
        }
    }

    /**
     * 查询买家总账号及其子账号
     */
    @Override
    public List<SysUser> selectBuyerAccountTree(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("查询买家账号树失败：用户ID无效 - {}", userId);
            return new ArrayList<>();
        }

        try {
            List<SysUser> userList = userMapper.selectBuyerAccountTree(userId);
            log.debug("查询买家账号树成功：用户ID={}, 结果数量={}", userId, userList.size());
            return userList;
        } catch (Exception e) {
            log.error("查询买家账号树异常：用户ID={}", userId, e);
            throw new RuntimeException("查询买家账号树失败", e);
        }
    }

    
    
    
    
    
    
    
    
    
    /**
     * 检查用户是否可以创建子账号
     */
    @Override
    public boolean checkCanCreateSubAccount(Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }

        try {
            SysUser user = selectUserWithDept(userId);
            if (user == null) {
                return false;
            }
            return user.isBuyerMainIdentity();
        } catch (Exception e) {
            log.error("检查用户是否可以创建子账号异常：用户ID={}", userId, e);
            return false;
        }
    }

    /**
     * 获取当前登录用户的SysUser对象
     *
     * 由于LoginUser中没有getUser()方法，通过userId查询数据库获取完整的SysUser信息
     *
     * @return 当前登录用户的SysUser对象，如果用户不存在则返回null
     */
    private SysUser getCurrentUser() {
        LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            return null;
        }

        return selectUserWithDept(loginUser.getUserId());
    }

    @Override
    public SysUser selectUserWithDept(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        try {
            SysUser user = userMapper.selectUserWithDept(userId);
            if (user != null) {
                ensureRoleKeysLoaded(user);
            }
            return user;
        } catch (Exception e) {
            log.error("查询用户完整信息异常：用户ID={}", userId, e);
            return null;
        }
    }

    private void setSimplifiedUserInfo(SysUser user) {
        try {
            // 查询用户的角色信息
            Set<SysRole> roles = userMapper.selectRolesByUserId(user.getUserId());
            if (roles != null && !roles.isEmpty()) {
                // 直接设置角色标识列表
                Set<String> roleIdentifiers = roles.stream()
                    .map(SysRole::getRoleKey)
                    .collect(Collectors.toSet());
                user.setRoles(roleIdentifiers);
            }
        } catch (Exception e) {
            log.warn("设置简化用户信息失败：用户ID={}", user.getUserId(), e);
        }
    }

    private void persistRequestedRoles(SysUser user) {
        if (user == null || user.getUserId() == null) {
            return;
        }

        Set<String> requestedRoleKeys = normalizeRoleKeySet(user.getRoles());
        user.setRoles(requestedRoleKeys);

        // 清理旧角色，避免重复绑定
        userMapper.deleteUserRoles(user.getUserId());

        if (requestedRoleKeys.isEmpty()) {
            return;
        }

        List<Long> roleIds = resolveRoleIdsFromKeys(requestedRoleKeys);
        if (roleIds.isEmpty()) {
            throw new RuntimeException("未找到可用的角色配置，请检查角色标识是否正确");
        }

        int inserted = userMapper.assignUserRoles(user.getUserId(), roleIds);
        if (inserted <= 0) {
            throw new RuntimeException("分配角色失败：数据库未生效");
        }
    }

    private List<Long> resolveRoleIdsFromKeys(Collection<String> roleKeys) {
        List<Long> roleIds = new ArrayList<>();
        for (String roleKey : roleKeys) {
            SysRole role = findRoleByKey(roleKey);
            if (role == null) {
                throw new RuntimeException("角色不存在：" + roleKey);
            }
            if (!"0".equals(role.getStatus()) || !"0".equals(role.getDelFlag())) {
                throw new RuntimeException("角色不可用：" + roleKey);
            }
            roleIds.add(role.getRoleId());
        }
        return roleIds;
    }

    private SysRole findRoleByKey(String roleKey) {
        String normalized = StringUtils.trimToNull(roleKey);
        if (normalized == null) {
            return null;
        }
        SysRole role = roleMapper.selectRoleByKey(normalized);
        if (role == null) {
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (!lower.equals(normalized)) {
                role = roleMapper.selectRoleByKey(lower);
            }
        }
        if (role == null) {
            String upper = normalized.toUpperCase(Locale.ROOT);
            if (!upper.equals(normalized)) {
                role = roleMapper.selectRoleByKey(upper);
            }
        }
        return role;
    }

    // ==================== 统计方法实现 ====================

    @Override
    public Map<String, Object> getManagedUsersStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            if (userId == null || userId <= 0) {
                return statistics;
            }

            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            if (currentUser == null) {
                throw new SecurityException("用户未登录");
            }

            if (!Objects.equals(currentUser.getUserId(), userId)
                && !currentUser.isAdminIdentity()
                && !hierarchyService.isAncestor(currentUser.getUserId(), userId)) {
                throw new SecurityException("无权访问该统计信息");
            }

            Set<Long> managedUserIds = collectManagedUserIds(userId);
            if (managedUserIds.isEmpty()) {
                initializeUserStatistics(statistics, 0L, 0L, 0L, 0L);
                statistics.put("totalUsers", 0L);
                statistics.put("managedUserIds", Collections.emptySet());
                statistics.put("identityBreakdown", Collections.emptyMap());
                statistics.put("agentLevelBreakdown", Collections.emptyMap());
                statistics.put("unknownUserCount", 0L);
                return statistics;
            }

            long activeUserTotal = Optional.ofNullable(userMapper.countActiveUsersByIds(managedUserIds)).orElse(0L);
            if (activeUserTotal == 0L) {
                initializeUserStatistics(statistics, 0L, 0L, 0L, 0L);
                statistics.put("totalUsers", 0L);
                statistics.put("managedUserIds", Collections.unmodifiableSet(new LinkedHashSet<>(managedUserIds)));
                statistics.put("identityBreakdown", Collections.emptyMap());
                statistics.put("agentLevelBreakdown", Collections.emptyMap());
                statistics.put("unknownUserCount", 0L);
                return statistics;
            }

            Map<UserIdentity, Long> identityCounts = aggregateIdentityCounts(managedUserIds);

            long adminCount = identityCounts.getOrDefault(UserIdentity.ADMIN, 0L);
            long agentLevel1Count = identityCounts.getOrDefault(UserIdentity.AGENT_LEVEL_1, 0L);
            long agentLevel2Count = identityCounts.getOrDefault(UserIdentity.AGENT_LEVEL_2, 0L);
            long agentLevel3Count = identityCounts.getOrDefault(UserIdentity.AGENT_LEVEL_3, 0L);
            long buyerMainCount = identityCounts.getOrDefault(UserIdentity.BUYER_MAIN, 0L);
            long buyerSubCount = identityCounts.getOrDefault(UserIdentity.BUYER_SUB, 0L);

            long agentTotal = agentLevel1Count + agentLevel2Count + agentLevel3Count;
            long knownCount = adminCount + agentTotal + buyerMainCount + buyerSubCount;
            long unknownCount = Math.max(0L, activeUserTotal - knownCount);

            initializeUserStatistics(statistics, adminCount, agentTotal, buyerMainCount, buyerSubCount);
            statistics.put("totalUsers", activeUserTotal);
            statistics.put("managedUserIds", Collections.unmodifiableSet(new LinkedHashSet<>(managedUserIds)));
            statistics.put("identityBreakdown", buildIdentityBreakdown(identityCounts, unknownCount));
            statistics.put("agentLevelBreakdown", buildAgentLevelBreakdown(identityCounts));
            statistics.put("unknownUserCount", unknownCount);

            log.info("用户 {} 管理的用户统计完成: total={}, admin={}, agentTotal={}, buyerMain={}, buyerSub={}, unknown={}",
                    userId, activeUserTotal, adminCount, agentTotal, buyerMainCount, buyerSubCount, unknownCount);

        } catch (Exception e) {
            log.error("获取管理用户统计信息失败：userId={}", userId, e);
            initializeUserStatistics(statistics, 0L, 0L, 0L, 0L);
            statistics.put("totalUsers", 0L);
            statistics.put("managedUserIds", Collections.emptySet());
            statistics.put("identityBreakdown", Collections.emptyMap());
            statistics.put("agentLevelBreakdown", Collections.emptyMap());
            statistics.put("unknownUserCount", 0L);
        }

        return statistics;
    }

    // ==================== 私有辅助方法 ====================

    private Set<Long> collectManagedUserIds(Long rootUserId) {
        if (rootUserId == null || rootUserId <= 0) {
            return Collections.emptySet();
        }
        LinkedHashSet<Long> managedUserIds = new LinkedHashSet<>();
        managedUserIds.add(rootUserId);
        managedUserIds.addAll(hierarchyService.findDescendantIds(rootUserId));
        return managedUserIds;
    }

    private Map<UserIdentity, Long> aggregateIdentityCounts(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Map<String, Object>> rawCounts = userMapper.countUsersByRoleKeys(userIds);
        Map<UserIdentity, Long> identityCounts = new EnumMap<>(UserIdentity.class);

        for (Map<String, Object> row : rawCounts) {
            Object roleKeyObj = row.get("roleKey");
            Object countObj = row.get("userCount");
            if (!(roleKeyObj instanceof String) || !(countObj instanceof Number)) {
                continue;
            }

            String roleKey = (String) roleKeyObj;
            Number countNumber = (Number) countObj;
            UserIdentity.fromRoleKey(roleKey)
                .ifPresent(identity ->
                    identityCounts.merge(identity, countNumber.longValue(), Long::sum)
                );
        }

        return identityCounts;
    }

    private Map<String, Long> buildIdentityBreakdown(Map<UserIdentity, Long> counts, long unknownCount) {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        breakdown.put(UserIdentity.ADMIN.getRoleKey(), counts.getOrDefault(UserIdentity.ADMIN, 0L));
        breakdown.put(UserIdentity.AGENT_LEVEL_1.getRoleKey(), counts.getOrDefault(UserIdentity.AGENT_LEVEL_1, 0L));
        breakdown.put(UserIdentity.AGENT_LEVEL_2.getRoleKey(), counts.getOrDefault(UserIdentity.AGENT_LEVEL_2, 0L));
        breakdown.put(UserIdentity.AGENT_LEVEL_3.getRoleKey(), counts.getOrDefault(UserIdentity.AGENT_LEVEL_3, 0L));
        breakdown.put(UserIdentity.BUYER_MAIN.getRoleKey(), counts.getOrDefault(UserIdentity.BUYER_MAIN, 0L));
        breakdown.put(UserIdentity.BUYER_SUB.getRoleKey(), counts.getOrDefault(UserIdentity.BUYER_SUB, 0L));
        if (unknownCount > 0) {
            breakdown.put("unassigned", unknownCount);
        }
        return breakdown;
    }

    private Map<String, Long> buildAgentLevelBreakdown(Map<UserIdentity, Long> counts) {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        breakdown.put(UserIdentity.AGENT_LEVEL_1.getRoleKey(), counts.getOrDefault(UserIdentity.AGENT_LEVEL_1, 0L));
        breakdown.put(UserIdentity.AGENT_LEVEL_2.getRoleKey(), counts.getOrDefault(UserIdentity.AGENT_LEVEL_2, 0L));
        breakdown.put(UserIdentity.AGENT_LEVEL_3.getRoleKey(), counts.getOrDefault(UserIdentity.AGENT_LEVEL_3, 0L));
        return breakdown;
    }

    /**
     * 初始化用户统计信息
     */
    private void initializeUserStatistics(Map<String, Object> statistics,
                                        Long systemUsers, Long agentUsers, Long buyerMainUsers, Long buyerSubUsers) {
        statistics.put("systemUserCount", systemUsers);
        statistics.put("agentUserCount", agentUsers);
        statistics.put("buyerMainUserCount", buyerMainUsers);
        statistics.put("buyerSubUserCount", buyerSubUsers);
    }

}
