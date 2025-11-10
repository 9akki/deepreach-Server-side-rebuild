package com.deepreach.common.core.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity基类
 *
 * 所有实体类的基类，提供通用的字段和方法：
 * 1. 主键ID字段
 * 2. 审计字段（创建者、创建时间、更新者、更新时间）
 * 3. 备注字段
 * 4. 通用工具方法
 * 5. 序列化支持
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Data
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分页参数
     */
    private Integer pageNum;
    private Integer pageSize;
    private String orderByColumn;
    private String isAsc = "asc";
    private Boolean reasonable = Boolean.TRUE;

    /**
     * 搜索值
     */
    private String searchValue;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 请求参数
     * -- SETTER --
     *  设置请求参数
     *
     * @param params 请求参数Map

     */
    @Setter
    private Map<String, Object> params;

    /**
     * 获取请求参数
     *
     * @return 请求参数Map，如果为null则返回空Map
     */
    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }

    /**
     * 添加请求参数
     *
     * @param key 参数名
     * @param value 参数值
     */
    public void addParam(String key, Object value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }
        this.params.put(key, value);
    }

    /**
     * 获取请求参数值
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
     * 获取请求参数值（带默认值）
     *
     * @param key 参数名
     * @param defaultValue 默认值
     * @return 参数值，如果不存在则返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParam(String key, T defaultValue) {
        if (this.params == null) {
            return defaultValue;
        }
        Object value = this.params.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 检查是否有指定的请求参数
     *
     * @param key 参数名
     * @return true如果存在，false如果不存在
     */
    public boolean hasParam(String key) {
        if (this.params == null) {
            return false;
        }
        return this.params.containsKey(key);
    }

    /**
     * 移除指定的请求参数
     *
     * @param key 参数名
     * @return 被移除的参数值，如果不存在则返回null
     */
    public Object removeParam(String key) {
        if (this.params == null) {
            return null;
        }
        return this.params.remove(key);
    }

    /**
     * 清空所有请求参数
     */
    public void clearParams() {
        if (this.params != null) {
            this.params.clear();
        }
    }

    public Boolean getReasonable() {
        return reasonable;
    }

    public void setReasonable(Boolean reasonable) {
        this.reasonable = reasonable;
    }

    /**
     * 获取参数数量
     *
     * @return 参数数量
     */
    public int getParamSize() {
        return this.params != null ? this.params.size() : 0;
    }

    /**
     * 检查是否有参数
     *
     * @return true如果有参数，false如果没有参数
     */
    public boolean hasParams() {
        return this.params != null && !this.params.isEmpty();
    }
}
