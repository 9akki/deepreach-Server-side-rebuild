package com.deepreach.common.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 *
 * 用于标记需要进行权限校验的方法，支持：
 * 1. Spring Security表达式权限校验
 * 2. 角色权限验证
 * 3. 功能权限验证
 * 4. 自定义权限逻辑
 *
 * 使用示例：
 * <pre>
 * &#64;PreAuthorize("@ss.hasPermi('system:user:list')")
 * public Result listUsers() {
 *     // 业务逻辑
 * }
 *
 * &#64;PreAuthorize("@ss.hasRole('admin')")
 * public Result deleteUser(@PathVariable Long userId) {
 *     // 业务逻辑
 * }
 * </pre>
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreAuthorize {

    /**
     * Spring Security表达式
     *
     * 权限校验的表达式，支持：
     * 1. @ss.hasPermi('permission') - 检查权限
     * 2. @ss.hasRole('role') - 检查角色
     * 3. @ss.hasAnyRoles('role1,role2') - 检查任意角色
     * 4. @ss.hasAllRoles('role1,role2') - 检查所有角色
     * 5. authentication.name == 'admin' - 自定义表达式
     *
     * @return 权限校验表达式
     */
    String value();

    /**
     * 权限校验失败时的错误消息
     *
     * 自定义权限校验失败时的错误提示信息
     * 如果不设置，使用默认消息
     *
     * @return 错误消息
     */
    String message() default "无权限访问，请联系管理员";

    /**
     * 是否记录权限校验失败日志
     *
     * 是否在权限校验失败时记录日志，用于安全审计
     *
     * @return true记录日志，false不记录日志
     */
    String logFail() default "true";
}