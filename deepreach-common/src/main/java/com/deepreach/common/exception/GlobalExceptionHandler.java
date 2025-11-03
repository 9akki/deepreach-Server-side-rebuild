package com.deepreach.common.exception;

import com.deepreach.common.web.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 全局异常处理器
 *
 * @author DeepReach Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     */
    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleServiceException(ServiceException e) {
        logger.error("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        logger.warn("参数校验失败：{}", e.getMessage());

        StringBuilder message = new StringBuilder("参数校验失败：");
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            message.append(fieldError.getField()).append(" ").append(fieldError.getDefaultMessage()).append("；");
        }

        return Result.error(400, message.toString());
    }

    /**
     * 处理参数校验异常（@ModelAttribute）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        logger.warn("参数绑定失败：{}", e.getMessage());

        StringBuilder message = new StringBuilder("参数绑定失败：");
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            message.append(fieldError.getField()).append(" ").append(fieldError.getDefaultMessage()).append("；");
        }

        return Result.error(400, message.toString());
    }

    /**
     * 处理参数校验异常（@Validated）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        logger.warn("参数校验失败：{}", e.getMessage());

        StringBuilder message = new StringBuilder("参数校验失败：");
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            message.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("；");
        }

        return Result.error(400, message.toString());
    }

    /**
     * 处理营销实例数量不足异常
     */
    @ExceptionHandler(InsufficientMarketingInstanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleInsufficientMarketingInstanceException(InsufficientMarketingInstanceException e) {
        logger.warn("营销实例数量不足：{}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("非法参数异常：{}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        logger.error("系统异常", e);
        return Result.error(500, "系统异常，请联系管理员");
    }
}