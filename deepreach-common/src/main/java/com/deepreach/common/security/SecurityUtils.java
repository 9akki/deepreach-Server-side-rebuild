package com.deepreach.common.security;

import com.deepreach.common.core.domain.model.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collection;

/**
 * 安全服务工具类
 *
 * 设计理念：
 * 1. 架构无关 - 所有阶段都可以使用
 * 2. 静态方法 - 无需依赖注入，调用简单
 * 3. 异常安全 - 完善的错误处理
 * 4. 性能优化 - 避免重复查询
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
public class SecurityUtils {

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，未登录时返回null
     */
    public static Long getCurrentUserId() {
        try {
            LoginUser loginUser = getCurrentLoginUser();
            return loginUser != null ? loginUser.getUserId() : null;
        } catch (Exception e) {
            log.warn("Failed to get current user ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，未登录时返回null
     */
    public static String getCurrentUsername() {
        try {
            LoginUser loginUser = getCurrentLoginUser();
            return loginUser != null ? loginUser.getUsername() : null;
        } catch (Exception e) {
            log.warn("Failed to get current username: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前用户邮箱
     *
     * @return 邮箱，未登录或无邮箱时返回null
     */
    public static String getCurrentEmail() {
        try {
            LoginUser loginUser = getCurrentLoginUser();
            return loginUser != null ? loginUser.getEmail() : null;
        } catch (Exception e) {
            log.warn("Failed to get current email: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前用户昵称
     *
     * @return 昵称，未登录时返回null
     */
    public static String getCurrentNickname() {
        try {
            LoginUser loginUser = getCurrentLoginUser();
            return loginUser != null ? loginUser.getNickname() : null;
        } catch (Exception e) {
            log.warn("Failed to get current nickname: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前用户手机号
     *
     * @return 手机号，未登录或无手机号时返回null
     */
    public static String getCurrentPhone() {
        try {
            LoginUser loginUser = getCurrentLoginUser();
            return loginUser != null ? loginUser.getPhone() : null;
        } catch (Exception e) {
            log.warn("Failed to get current phone: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前用户完整信息
     *
     * @return LoginUser对象，未登录时返回null
     */
    public static LoginUser getCurrentLoginUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LoginUser) {
                return (LoginUser) authentication.getPrincipal();
            }
        } catch (Exception e) {
            log.error("Failed to get current login user", e);
        }
        return null;
    }

    /**
     * 获取当前用户角色列表
     *
     * @return 角色列表，未登录时返回空列表
     */
    public static Collection<String> getCurrentUserRoles() {
        try {
            LoginUser loginUser = getCurrentLoginUser();
            return loginUser != null ? loginUser.getRoles() : java.util.Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get current user roles: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 获取当前用户权限列表
     *
     * @return 权限列表，未登录时返回空列表
     */
    public static Collection<String> getCurrentUserPermissions() {
        try {
            LoginUser loginUser = getCurrentLoginUser();
            return loginUser != null ? loginUser.getPermissions() : java.util.Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get current user permissions: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 生成BCrypt密码
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    public static String encryptPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    /**
     * 验证密码
     *
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 验证结果
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 判断用户是否为管理员
     *
     * @param userId 用户ID
     * @return 是否为管理员
     */
    public static boolean isAdmin(Long userId) {
        return userId != null && 1L == userId;
    }

    /**
     * 判断当前用户是否为管理员
     *
     * @return 是否为管理员
     */
    public static boolean isCurrentUserAdmin() {
        return isAdmin(getCurrentUserId());
    }

    /**
     * 验证用户是否具备某权限
     *
     * @param permission 权限字符串
     * @return 是否具备权限
     */
    public static boolean hasPermi(String permission) {
        return hasAnyPermi(permission);
    }

    /**
     * 验证用户是否具备某权限（别名方法）
     *
     * @param permission 权限字符串
     * @return 是否具备权限
     */
    public static boolean hasPermission(String permission) {
        return hasPermi(permission);
    }

    /**
     * 验证用户是否含有指定权限（任意一个即可）
     *
     * @param permissions 权限数组
     * @return 是否具备任意权限
     */
    public static boolean hasAnyPermi(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        try {
            LoginUser loginUser = getCurrentLoginUser();
            if (loginUser == null || loginUser.getPermissions() == null) {
                return false;
            }

            Collection<String> userPermissions = loginUser.getPermissions();
            for (String permission : permissions) {
                if (userPermissions.contains(permission)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check permissions: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 验证用户是否含有所有指定权限
     *
     * @param permissions 权限数组
     * @return 是否具备所有权限
     */
    public static boolean hasAllPermi(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        try {
            LoginUser loginUser = getCurrentLoginUser();
            if (loginUser == null || loginUser.getPermissions() == null) {
                return false;
            }

            Collection<String> userPermissions = loginUser.getPermissions();
            for (String permission : permissions) {
                if (!userPermissions.contains(permission)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to check all permissions: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证用户是否具备某角色
     *
     * @param role 角色字符串
     * @return 是否具备角色
     */
    public static boolean hasRole(String role) {
        return hasAnyRoles(role);
    }

    /**
     * 验证用户是否含有指定角色（任意一个即可）
     *
     * @param roles 角色数组
     * @return 是否具备任意角色
     */
    public static boolean hasAnyRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }

        try {
            LoginUser loginUser = getCurrentLoginUser();
            if (loginUser == null || loginUser.getRoles() == null) {
                return false;
            }

            Collection<String> userRoles = loginUser.getRoles();
            for (String role : roles) {
                if (userRoles.contains(role)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check roles: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 验证用户是否含有所有指定角色
     *
     * @param roles 角色数组
     * @return 是否具备所有角色
     */
    public static boolean hasAllRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }

        try {
            LoginUser loginUser = getCurrentLoginUser();
            if (loginUser == null || loginUser.getRoles() == null) {
                return false;
            }

            Collection<String> userRoles = loginUser.getRoles();
            for (String role : roles) {
                if (!userRoles.contains(role)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to check all roles: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查用户是否已认证
     *
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.isAuthenticated()
                   && !(authentication.getPrincipal() instanceof String);
        } catch (Exception e) {
            log.warn("Failed to check authentication status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否为匿名用户
     *
     * @return 是否为匿名用户
     */
    public static boolean isAnonymous() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication == null
                   || authentication.getPrincipal() instanceof String
                   || "anonymousUser".equals(authentication.getName());
        } catch (Exception e) {
            log.warn("Failed to check anonymous status: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 检查当前用户是否为指定用户
     *
     * @param userId 用户ID
     * @return 是否为当前用户
     */
    public static boolean isCurrentUser(Long userId) {
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    /**
     * 安全执行操作（仅在用户已认证时）
     *
     * @param operation 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws SecurityException 用户未认证时抛出异常
     */
    public static <T> T executeIfAuthenticated(AuthenticatedOperation<T> operation) {
        if (isAuthenticated()) {
            try {
                LoginUser loginUser = getCurrentLoginUser();
                return operation.execute(loginUser);
            } catch (Exception e) {
                log.error("Error executing authenticated operation", e);
                throw new RuntimeException("操作执行失败: " + e.getMessage(), e);
            }
        } else {
            throw new SecurityException("用户未认证，无法执行操作");
        }
    }

    /**
     * 安全执行操作（返回Optional）
     *
     * @param operation 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果的Optional包装
     */
    public static <T> java.util.Optional<T> executeIfAuthenticatedSafe(AuthenticatedOperation<T> operation) {
        try {
            if (isAuthenticated()) {
                LoginUser loginUser = getCurrentLoginUser();
                return java.util.Optional.ofNullable(operation.execute(loginUser));
            }
        } catch (Exception e) {
            log.error("Error executing authenticated operation safely", e);
        }
        return java.util.Optional.empty();
    }

    /**
     * 检查用户是否启用
     *
     * @return 用户是否启用
     */
    public static boolean isCurrentUserEnabled() {
        try {
            LoginUser loginUser = getCurrentLoginUser();
            return loginUser != null && loginUser.isEnabled();
        } catch (Exception e) {
            log.warn("Failed to check user enabled status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前用户的显示名称
     * 优先显示昵称，其次显示用户名
     *
     * @return 显示名称
     */
    public static String getCurrentDisplayName() {
        try {
            LoginUser loginUser = getCurrentLoginUser();
            if (loginUser != null) {
                return loginUser.getDisplayName();
            }
        } catch (Exception e) {
            log.warn("Failed to get display name: {}", e.getMessage());
        }
        return "未知用户";
    }

    /**
     * 认证操作函数式接口
     */
    @FunctionalInterface
    public interface AuthenticatedOperation<T> {
        /**
         * 执行操作
         *
         * @param loginUser 当前登录用户
         * @return 操作结果
         */
        T execute(LoginUser loginUser);
    }
}