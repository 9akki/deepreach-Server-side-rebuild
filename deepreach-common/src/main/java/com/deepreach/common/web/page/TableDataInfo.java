package com.deepreach.common.web.page;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 表格分页数据对象
 *
 * 分页查询的统一返回格式，包含：
 * 1. 分页数据列表
 * 2. 分页信息（总记录数、页码、页大小等）
 * 3. 查询条件
 * 4. 扩展字段支持
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Data
public class TableDataInfo<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int pageNum;

    /** 每页大小 */
    private int pageSize;

    /** 总页数 */
    private int pages;

    /** 数据列表 */
    private List<T> rows;

    /** 查询参数 */
    private java.util.Map<String, Object> params;

    /** 消息提示 */
    private String msg;

    /** 状态码 */
    private int code;

    /** 时间戳 */
    private long timestamp;

    /**
     * 空构造
     */
    public TableDataInfo() {
        this.timestamp = System.currentTimeMillis();
        this.code = 200;
        this.msg = "查询成功";
    }

    /**
     * 分页构造
     *
     * @param list      数据列表
     * @param total     总记录数
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     */
    public TableDataInfo(List<T> list, long total, int pageNum, int pageSize) {
        this.rows = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        if (pageSize <= 0) {
            this.pages = total > 0 ? 1 : 0;
        } else {
            this.pages = (int) Math.ceil((double) total / pageSize);
        }
        this.timestamp = System.currentTimeMillis();
        this.code = 200;
        this.msg = "查询成功";
    }

    /**
     * 简化构造（默认页码1，页大小10）
     *
     * @param list  数据列表
     * @param total 总记录数
     */
    public TableDataInfo(List<T> list, long total) {
        this(list, total, 1, 10);
    }

    /**
     * 创建成功结果
     *
     * @param list 数据列表
     * @param total 总记录数
     * @return 分页数据对象
     */
    public static <T> TableDataInfo<T> success(List<T> list, long total) {
        return new TableDataInfo<>(list, total);
    }

    /**
     * 创建成功结果
     *
     * @param list      数据列表
     * @param total     总记录数
     * @param pageNum   当前页码
     * @param pageSize  每页大小
     * @return 分页数据对象
     */
    public static <T> TableDataInfo<T> success(List<T> list, long total, int pageNum, int pageSize) {
        return new TableDataInfo<>(list, total, pageNum, pageSize);
    }

    /**
     * 创建错误结果
     *
     * @param msg 错误消息
     * @return 分页数据对象
     */
    public static <T> TableDataInfo<T> error(String msg) {
        TableDataInfo<T> tableDataInfo = new TableDataInfo<>();
        tableDataInfo.setCode(500);
        tableDataInfo.setMsg(msg);
        tableDataInfo.setRows(null);
        tableDataInfo.setTotal(0);
        return tableDataInfo;
    }

    /**
     * 是否为空数据
     *
     * @return true如果没有数据，false如果有数据
     */
    public boolean isEmpty() {
        return rows == null || rows.isEmpty();
    }

    /**
     * 是否有数据
     *
     * @return true如果有数据，false如果没有数据
     */
    public boolean hasData() {
        return !isEmpty();
    }

    /**
     * 是否为第一页
     *
     * @return true如果是第一页，false否则
     */
    public boolean isFirstPage() {
        return pageNum <= 1;
    }

    /**
     * 是否为最后一页
     *
     * @return true如果是最后一页，false否则
     */
    public boolean isLastPage() {
        return pageNum >= pages;
    }

    /**
     * 是否有上一页
     *
     * @return true如果有上一页，false否则
     */
    public boolean hasPreviousPage() {
        return pageNum > 1;
    }

    /**
     * 是否有下一页
     *
     * @return true如果有下一页，false否则
     */
    public boolean hasNextPage() {
        return pageNum < pages;
    }

    /**
     * 获取查询参数
     *
     * @return 查询参数Map，如果为null则返回空Map
     */
    public java.util.Map<String, Object> getParams() {
        if (params == null) {
            params = new java.util.HashMap<>();
        }
        return params;
    }

    /**
     * 设置查询参数
     *
     * @param params 查询参数Map
     */
    public void setParams(java.util.Map<String, Object> params) {
        this.params = params;
    }

    /**
     * 添加查询参数
     *
     * @param key   参数名
     * @param value 参数值
     */
    public void addParam(String key, Object value) {
        if (this.params == null) {
            this.params = new java.util.HashMap<>();
        }
        this.params.put(key, value);
    }

    /**
     * 获取查询参数值
     *
     * @param key 参数名
     * @return 参数值，如果不存在则返回null
     */
    public Object getParam(String key) {
        if (this.params == null) {
            return null;
        }
        return this.params.get(key);
    }

    /**
     * 计算起始位置
     *
     * @return 起始位置（从0开始）
     */
    public int getStartRow() {
        return (pageNum - 1) * pageSize;
    }

    /**
     * 计算结束位置
     *
     * @return 结束位置
     */
    public int getEndRow() {
        return Math.min(pageNum * pageSize, (int) total);
    }

    /**
     * 获取当前页的数据量
     *
     * @return 当前页数据量
     */
    public int getCurrentPageSize() {
        return rows != null ? rows.size() : 0;
    }
}
