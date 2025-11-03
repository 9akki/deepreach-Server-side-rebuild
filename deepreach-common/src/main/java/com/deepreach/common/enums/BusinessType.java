package com.deepreach.common.enums;

/**
 * 业务操作类型枚举
 *
 * 定义系统中各种业务操作的类型，用于：
 * 1. 操作日志的分类和统计
 * 2. 权限控制的细粒度管理
 * 3. 业务流程的监控和审计
 * 4. 数据统计和分析报表
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
public enum BusinessType {
    /**
     * 其它
     */
    OTHER(0, "其它"),

    /**
     * 新增
     */
    INSERT(1, "新增"),

    /**
     * 修改
     */
    UPDATE(2, "修改"),

    /**
     * 删除
     */
    DELETE(3, "删除"),

    /**
     * 授权
     */
    GRANT(4, "授权"),

    /**
     * 导出
     */
    EXPORT(5, "导出"),

    /**
     * 导入
     */
    IMPORT(6, "导入"),

    /**
     * 强退
     */
    FORCE(7, "强退"),

    /**
     * 生成代码
     */
    GENCODE(8, "生成代码"),

    /**
     * 清空数据
     */
    CLEAN(9, "清空数据");

    private final int code;
    private final String description;

    BusinessType(int code, String description) {
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
     * @param code 业务类型代码
     * @return 业务类型枚举，如果不存在则返回OTHER
     */
    public static BusinessType getByCode(int code) {
        for (BusinessType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return OTHER;
    }

    /**
     * 根据描述获取枚举
     *
     * @param description 业务类型描述
     * @return 业务类型枚举，如果不存在则返回OTHER
     */
    public static BusinessType getByDescription(String description) {
        for (BusinessType type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return OTHER;
    }

    /**
     * 检查是否为新增操作
     *
     * @return true如果是新增操作，false否则
     */
    public boolean isInsert() {
        return this == INSERT;
    }

    /**
     * 检查是否为修改操作
     *
     * @return true如果是修改操作，false否则
     */
    public boolean isUpdate() {
        return this == UPDATE;
    }

    /**
     * 检查是否为删除操作
     *
     * @return true如果是删除操作，false否则
     */
    public boolean isDelete() {
        return this == DELETE;
    }

    /**
     * 检查是否为授权操作
     *
     * @return true如果是授权操作，false否则
     */
    public boolean isGrant() {
        return this == GRANT;
    }

    /**
     * 检查是否为导出操作
     *
     * @return true如果是导出操作，false否则
     */
    public boolean isExport() {
        return this == EXPORT;
    }

    /**
     * 检查是否为导入操作
     *
     * @return true如果是导入操作，false否则
     */
    public boolean isImport() {
        return this == IMPORT;
    }

    /**
     * 检查是否为强制操作
     *
     * @return true如果是强制操作，false否则
     */
    public boolean isForce() {
        return this == FORCE;
    }

    /**
     * 检查是否为生成代码操作
     *
     * @return true如果是生成代码操作，false否则
     */
    public boolean isGenCode() {
        return this == GENCODE;
    }

    /**
     * 检查是否为清空数据操作
     *
     * @return true如果是清空数据操作，false否则
     */
    public boolean isClean() {
        return this == CLEAN;
    }

    /**
     * 检查是否为数据修改操作（增删改）
     *
     * @return true如果是数据修改操作，false否则
     */
    public boolean isDataModification() {
        return isInsert() || isUpdate() || isDelete();
    }

    /**
     * 检查是否为数据传输操作
     *
     * @return true如果是数据传输操作，false否则
     */
    public boolean isDataTransfer() {
        return isExport() || isImport();
    }

    @Override
    public String toString() {
        return this.description;
    }
}