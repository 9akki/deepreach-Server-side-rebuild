package com.deepreach.common.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 统一响应结果 (AjaxResult)
 *
 * @param <T> 数据类型
 * @author DeepReach Team
 */
@Schema(description = "统一响应结果")
public class AjaxResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码")
    private Integer code;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "时间戳")
    @JsonProperty("timestamp")
    private Long timestamp;

    public AjaxResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public AjaxResult(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public AjaxResult(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    /**
     * 成功响应
     */
    public static <T> AjaxResult<T> success() {
        return new AjaxResult<>(200, "操作成功");
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> AjaxResult<T> success(T data) {
        return new AjaxResult<>(200, "操作成功", data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> AjaxResult<T> success(String message, T data) {
        return new AjaxResult<>(200, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> AjaxResult<T> error() {
        return new AjaxResult<>(500, "操作失败");
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> AjaxResult<T> error(String message) {
        return new AjaxResult<>(500, message);
    }

    /**
     * 失败响应（自定义状态码和消息）
     */
    public static <T> AjaxResult<T> error(Integer code, String message) {
        return new AjaxResult<>(code, message);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }

    // Getter和Setter方法
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AjaxResult{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}