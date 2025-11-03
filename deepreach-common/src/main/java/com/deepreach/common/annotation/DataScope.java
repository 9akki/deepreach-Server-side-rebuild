package com.deepreach.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 *
 * 用于标记需要进行数据权限控制的方法，自动根据当前用户的数据权限范围过滤数据：
 * 1. 全部数据权限 - 可以查看所有数据
 * 2. 自定义数据权限 - 可以查看指定部门的数据
 * 3. 本部门数据权限 - 只能查看本部门的数据
 * 4. 本部门及以下数据权限 - 可以查看本部门及子部门的数据
 *
 * 使用示例：
 * <pre>
 * &#64;DataScope(tableAlias = "u", deptFieldName = "dept_id")
 * public List<SysUser> selectUserList(SysUser user) {
 *     // 业务逻辑
 * }
 * </pre>
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 表别名
     *
     * SQL查询中的表别名，用于构建完整的字段名
     * 如："u"、"d"等，如果为空则不使用别名
     *
     * @return 表别名
     */
    String tableAlias() default "";

    /**
     * 部门字段名
     *
     * 表中部门ID字段的名称，默认为"dept_id"
     * 用于构建数据权限SQL条件
     *
     * @return 部门字段名
     */
    String deptFieldName() default "dept_id";

    /**
     * 用户字段名
     *
     * 表中用户ID字段的名称，默认为"user_id"
     * 用于构建个人数据权限SQL条件
     *
     * @return 用户字段名
     */
    String userFieldName() default "user_id";

    /**
     * 是否启用个人数据权限
     *
     * 是否启用个人数据权限控制
     * 如果启用，则只能查看自己的数据
     *
     * @return true启用，false不启用
     */
    boolean enableUserPermission() default false;

    /**
     * 权限描述
     *
     * 数据权限的描述信息，用于日志记录和调试
     *
     * @return 权限描述
     */
    String value() default "";
}