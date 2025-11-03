package com.deepreach.common.annotation;

import com.deepreach.common.enums.BusinessType;
import com.deepreach.common.enums.OperatorType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * 用于标记需要记录操作日志的方法，自动记录：
 * 1. 操作标题和业务类型
 * 2. 操作者和操作时间
 * 3. 请求参数和返回结果
 * 4. 操作状态和错误信息
 * 5. 操作者类型和部门信息
 *
 * 使用示例：
 * <pre>
 * &#64;Log(title = "用户管理", businessType = BusinessType.INSERT)
 * public Result addUser(@RequestBody SysUser user) {
 *     // 业务逻辑
 * }
 * </pre>
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 模块标题
     *
     * 操作模块的名称，用于日志分类和检索
     * 如："用户管理"、"角色管理"、"系统设置"等
     *
     * @return 模块标题
     */
    String title() default "";

    /**
     * 业务操作类型
     *
     * 定义操作的业务类型，用于统计和分析
     *
     * @return 业务类型
     * @see BusinessType
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 操作人类别
     *
     * 定义操作者的类型，区分不同来源的操作
     *
     * @return 操作者类型
     * @see OperatorType
     */
    OperatorType operatorType() default OperatorType.MANAGE;

    /**
     * 是否保存请求的参数
     *
     * 是否记录请求的参数信息到日志中
     * 对于敏感信息可以设置为false
     *
     * @return true保存，false不保存
     */
    boolean saveRequestData() default true;

    /**
     * 是否保存响应的参数
     *
     * 是否记录响应的结果信息到日志中
     * 对于大量数据可以设置为false
     *
     * @return true保存，false不保存
     */
    boolean saveResponseData() default true;

    /**
     * 操作描述
     *
     * 操作的详细描述信息
     * 可以包含操作的具体内容和目的
     *
     * @return 操作描述
     */
    String description() default "";
}