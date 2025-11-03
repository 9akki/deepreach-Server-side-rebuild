package com.deepreach.common.security;

import com.deepreach.common.core.domain.model.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;

import java.util.Collection;

/**
 * 权限处理服务
 *
 * 基于部门类型的权限处理服务，用于Spring Security的@PreAuthorize注解权限验证：
 * 1. 提供统一的权限检查接口，支持超级管理员权限
 * 2. 基于部门类型的权限控制逻辑
 * 3. 支持角色和权限的细粒度验证
 * 4. 集成组织架构的权限管理
 *
 * 设计理念：
 * - 部门决定权限范围：不同部门类型有不同的默认权限
 * - 角色与部门类型绑定：权限验证考虑用户的部门背景
 * - 简化权限逻辑：基于明确的权限规则和组织架构
 *
 * 使用示例：
 * <pre>
 * &#64;PreAuthorize("@ss.hasPermi('system:user:list')")
 * public Result listUsers() {
 *     // 业务逻辑 - 只有系统部门和代理部门用户可访问
 * }
 *
 * &#64;PreAuthorize("@ss.hasRole('ADMIN')")
 * public Result deleteUser(Long userId) {
 *     // 业务逻辑 - 只有管理员角色可访问
 * }
 * </pre>
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Slf4j
@Service("ss")
public class PermissionService {

    /**
     * 所有权限标识
     */
    private static final String ALL_PERMISSION = "*:*:*";

    /**
     * 管理员角色标识
     */
    private static final String SUPER_ADMIN = "ADMIN";

    /**
     * 超级管理员用户ID
     */
    private static final Long SUPER_ADMIN_USER_ID = 1L;

    /**
     * 验证用户是否具备某权限
     * 超级管理员拥有所有权限
     *
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    public boolean hasPermi(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return false;
        }

        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                log.warn("权限检查失败：用户未登录 - 权限: {}", permission);
                return false;
            }

            // 超级管理员拥有所有权限
            if (isSuperAdmin(loginUser)) {
                log.info("超级管理员用户 {} 拥有权限: {}", loginUser.getUsername(), permission);
                return true;
            }

            // 普通用户权限检查
            if (CollectionUtils.isEmpty(loginUser.getPermissions())) {
                log.debug("用户 {} 没有配置权限", loginUser.getUsername());
                return false;
            }

            return hasPermissions(loginUser.getPermissions(), permission);
        } catch (Exception e) {
            log.warn("权限检查失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证用户是否含有指定权限（任意一个即可）
     *
     * @param permissions 权限数组
     * @return 用户是否具备任意权限
     */
    public boolean hasAnyPermi(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null || CollectionUtils.isEmpty(loginUser.getPermissions())) {
                return false;
            }

            // 超级管理员拥有所有权限
            if (SecurityUtils.isCurrentUserAdmin()) {
                return true;
            }

            Collection<String> userPermissions = loginUser.getPermissions();
            for (String permission : permissions) {
                if (hasPermissions(userPermissions, permission)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("权限检查失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证用户是否含有所有指定权限
     *
     * @param permissions 权限数组
     * @return 用户是否具备所有权限
     */
    public boolean hasAllPermi(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null || CollectionUtils.isEmpty(loginUser.getPermissions())) {
                return false;
            }

            // 超级管理员拥有所有权限
            if (SecurityUtils.isCurrentUserAdmin()) {
                return true;
            }

            Collection<String> userPermissions = loginUser.getPermissions();
            for (String permission : permissions) {
                if (!hasPermissions(userPermissions, permission)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("权限检查失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证用户是否具备某角色
     *
     * @param role 角色字符串
     * @return 用户是否具备某角色
     */
    public boolean hasRole(String role) {
        return hasAnyRoles(role);
    }

    /**
     * 验证用户是否含有指定角色（任意一个即可）
     *
     * @param roles 角色数组
     * @return 用户是否具备任意角色
     */
    public boolean hasAnyRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }

        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null || CollectionUtils.isEmpty(loginUser.getRoles())) {
                return false;
            }

            // 超级管理员拥有所有角色
            if (SecurityUtils.isCurrentUserAdmin()) {
                return true;
            }

            Collection<String> userRoles = loginUser.getRoles();
            for (String role : roles) {
                if (userRoles.contains(role) || userRoles.contains(SUPER_ADMIN) || userRoles.contains("admin")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("角色检查失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证用户是否含有所有指定角色
     *
     * @param roles 角色数组
     * @return 用户是否具备所有角色
     */
    public boolean hasAllRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }

        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null || CollectionUtils.isEmpty(loginUser.getRoles())) {
                return false;
            }

            // 超级管理员拥有所有角色
            if (SecurityUtils.isCurrentUserAdmin()) {
                return true;
            }

            Collection<String> userRoles = loginUser.getRoles();
            for (String role : roles) {
                if (!userRoles.contains(role) && !userRoles.contains(SUPER_ADMIN) && !userRoles.contains("admin")) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("角色检查失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 判断是否为超级管理员
     *
     * @return 是否为超级管理员
     */
    public boolean isAdmin() {
        return SecurityUtils.isCurrentUserAdmin();
    }

    /**
     * 判断用户是否为超级管理员
     * 支持多种判断方式
     *
     * @param loginUser 登录用户对象
     * @return 是否为超级管理员
     */
    private boolean isSuperAdmin(LoginUser loginUser) {
        if (loginUser == null) {
            return false;
        }

        // 方式1：通过用户ID判断（最直接）
        if (SUPER_ADMIN_USER_ID.equals(loginUser.getUserId())) {
            return true;
        }

        // 方式2：通过角色判断（更灵活，支持大小写）
        if (loginUser.getRoles() != null &&
            (loginUser.getRoles().contains(SUPER_ADMIN) || loginUser.getRoles().contains("admin"))) {
            return true;
        }

        // 方式3：通过SecurityUtils判断（兼容性）
        if (SecurityUtils.isCurrentUserAdmin()) {
            return true;
        }

        return false;
    }

    /**
     * 检查用户是否已认证
     *
     * @return 是否已认证
     */
    public boolean isAuthenticated() {
        return SecurityUtils.isAuthenticated();
    }

    /**
     * 检查用户是否启用
     *
     * @return 用户是否启用
     */
    public boolean isEnabled() {
        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            return loginUser != null && loginUser.isEnabled();
        } catch (Exception e) {
            log.warn("用户状态检查失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证用户是否具备某权限，如果不具备则抛出异常
     * 超级管理员拥有所有权限
     *
     * @param permission 权限字符串
     * @throws AccessDeniedException 如果用户不具备权限
     */
    public void checkPermi(String permission) throws AccessDeniedException {
        if (!hasPermi(permission)) {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            String username = loginUser != null ? loginUser.getUsername() : "未知用户";
            String errorMsg = String.format("用户 '%s' 没有权限 '%s'，请联系管理员分配相应权限。", username, permission);
            log.warn("权限检查失败: {}", errorMsg);
            throw new AccessDeniedException(errorMsg);
        }
    }

    /**
     * 验证用户是否具备某权限，如果不具备则抛出异常（带自定义消息）
     * 超级管理员拥有所有权限
     *
     * @param permission 权限字符串
     * @param message 自定义错误消息
     * @throws AccessDeniedException 如果用户不具备权限
     */
    public void checkPermi(String permission, String message) throws AccessDeniedException {
        if (!hasPermi(permission)) {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            String username = loginUser != null ? loginUser.getUsername() : "未知用户";
            String errorMsg = String.format("%s。缺少权限: %s", message, permission);
            log.warn("权限检查失败: {}", errorMsg);
            throw new AccessDeniedException(errorMsg);
        }
    }

    /**
     * 验证用户是否含有某个角色，如果不具备则抛出异常
     *
     * @param role 角色字符串
     * @throws AccessDeniedException 如果用户不具备角色
     */
    public void checkRole(String role) throws AccessDeniedException {
        if (!hasRole(role)) {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            String username = loginUser != null ? loginUser.getUsername() : "未知用户";
            String errorMsg = String.format("用户 '%s' 没有角色 '%s'，请联系管理员分配相应角色。", username, role);
            log.warn("角色检查失败: {}", errorMsg);
            throw new AccessDeniedException(errorMsg);
        }
    }

    /**
     * 判断是否包含权限
     * 支持通配符权限匹配
     *
     * @param permissions 权限列表
     * @param permission  权限字符串
     * @return 是否包含权限
     */
    private boolean hasPermissions(Collection<String> permissions, String permission) {
        if (CollectionUtils.isEmpty(permissions)) {
            return false;
        }

        return permissions.stream()
                .filter(p -> !p.trim().isEmpty())
                .anyMatch(p -> ALL_PERMISSION.equals(p) ||
                           PatternMatchUtils.simpleMatch(p, permission));
    }
}