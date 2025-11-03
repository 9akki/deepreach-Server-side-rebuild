package com.deepreach.common.enums;

/**
 * 操作人类别枚举
 *
 * 定义系统中操作者的类型，用于：
 * 1. 区分不同来源的操作
 * 2. 操作日志的分类统计
 * 3. 安全审计的权限分析
 * 4. 用户行为的分析报表
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
public enum OperatorType {
    /**
     * 其它
     */
    OTHER(0, "其它"),

    /**
     * 后台用户
     */
    MANAGE(1, "后台用户"),

    /**
     * 手机端用户
     */
    MOBILE(2, "手机端用户");

    private final int code;
    private final String description;

    OperatorType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 操作者类型代码
     * @return 操作者类型枚举，如果不存在则返回OTHER
     */
    public static OperatorType getByCode(int code) {
        for (OperatorType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return OTHER;
    }

    /**
     * 根据描述获取枚举
     *
     * @param description 操作者类型描述
     * @return 操作者类型枚举，如果不存在则返回OTHER
     */
    public static OperatorType getByDescription(String description) {
        for (OperatorType type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return OTHER;
    }

    /**
     * 检查是否为后台用户
     *
     * @return true如果是后台用户，false否则
     */
    public boolean isManage() {
        return this == MANAGE;
    }

    /**
     * 检查是否为手机端用户
     *
     * @return true如果是手机端用户，false否则
     */
    public boolean isMobile() {
        return this == MOBILE;
    }

    /**
     * 检查是否为其它类型
     *
     * @return true如果是其它类型，false否则
     */
    public boolean isOther() {
        return this == OTHER;
    }

    @Override
    public String toString() {
        return this.description;
    }
}