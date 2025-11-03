package com.deepreach.common.web.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 操作消息提醒
 *
 * 统一的API响应格式，用于规范化接口返回数据：
 * 1. 成功响应：包含数据、消息和状态码
 * 2. 失败响应：包含错误消息和状态码
 * 3. 统一的状态码定义
 * 4. 支持泛型的数据返回
 * 5. 序列化友好
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private int code;

    /** 返回消息 */
    private String msg;

    /** 数据 payload */
    private T data;

    /** 时间戳 */
    private long timestamp;

    /**
     * 初始化一个新创建的 Result 对象，使其表示一个空消息。
     */
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 初始化一个新创建的 Result 对象
     *
     * @param code 状态码
     * @param msg 返回消息
     */
    public Result(int code, String msg) {
        this.code = code;
        this.msg = msg;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 初始化一个新创建的 Result 对象
     *
     * @param code 状态码
     * @param msg 返回消息
     * @param data 数据对象
     */
    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 返回成功消息
     *
     * @return 成功消息
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }

    /**
     * 返回成功数据
     *
     * @param data 数据对象
     * @return 成功消息
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 返回成功消息
     *
     * @param msg 返回消息
     * @return 成功消息
     */
    public static <T> Result<T> success(String msg) {
        return new Result<>(200, msg);
    }

    /**
     * 返回成功消息
     *
     * @param msg 返回消息
     * @param data 数据对象
     * @return 成功消息
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回消息
     * @return 警告消息
     */
    public static <T> Result<T> warn(String msg) {
        return new Result<>(301, msg);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回消息
     * @param data 数据对象
     * @return 警告消息
     */
    public static <T> Result<T> warn(String msg, T data) {
        return new Result<>(301, msg, data);
    }

    /**
     * 返回错误消息
     *
     * @param msg 返回消息
     * @return 警告消息
     */
    public static <T> Result<T> error() {
        return new Result<>(500, "操作失败");
    }

    /**
     * 返回错误消息
     *
     * @param msg 返回消息
     * @return 警告消息
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg);
    }

    /**
     * 返回错误消息
     *
     * @param code 状态码
     * @param msg 返回消息
     * @return 警告消息
     */
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg);
    }

    /**
     * 返回错误消息
     *
     * @param code 状态码
     * @param msg 返回消息
     * @param data 数据对象
     * @return 警告消息
     */
    public static <T> Result<T> error(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    /**
     * 是否为成功消息
     *
     * @return 结果
     */
    public boolean isSuccess() {
        return this.code == 200;
    }

    /**
     * 是否为错误消息
     *
     * @return 结果
     */
    public boolean isError() {
        return this.code != 200;
    }

    /**
     * 方便链式调用
     *
     * @param data 数据对象
     * @return Result实例对象
     */
    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }

    /**
     * 方便链式调用
     *
     * @param code 状态码
     * @return Result实例对象
     */
    public Result<T> setCode(int code) {
        this.code = code;
        return this;
    }

    /**
     * 方便链式调用
     *
     * @param msg 返回消息
     * @return Result实例对象
     */
    public Result<T> setMsg(String msg) {
        this.msg = msg;
        return this;
    }
}