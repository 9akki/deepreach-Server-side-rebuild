package com.deepreach.common.aspect;

import com.deepreach.common.annotation.Log;
import com.deepreach.common.core.domain.entity.SysOperLog;
import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.service.SysDeptService;
import com.deepreach.common.core.service.SysOperLogService;
import com.deepreach.common.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面
 *
 * 处理带有 @Log 注解的方法，自动记录操作日志：
 * 1. 记录操作前信息（方法、参数、IP等）
 * 2. 记录操作后信息（结果、状态等）
 * 3. 记录异常信息
 * 4. 异步保存日志到数据库
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysDeptService deptService;

    @Autowired
    private SysOperLogService operLogService;

    /**
     * 线程本地变量，用于存储日志信息
     */
    private static final ThreadLocal<SysOperLog> logThreadLocal = new ThreadLocal<>();

    /**
     * 线程本地变量，用于存储操作开始时间（纳秒级）
     */
    private static final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    /**
     * 处理带有 @Log 注解的方法 - 前置通知
     *
     * 记录方法执行前的信息：
     * 1. 操作者信息
     * 2. 请求信息（URL、方法、参数）
     * 3. 操作时间
     *
     * @param joinPoint 连接点
     * @param logAnnotation 日志注解
     */
    @Before(value = "@annotation(logAnnotation)")
    public void doBefore(JoinPoint joinPoint, Log logAnnotation) {
        log.info("LogAspect doBefore 被触发: {} - {}", logAnnotation.title(), joinPoint.getSignature().getName());

        // 记录开始时间（纳秒级精度）
        startTimeThreadLocal.set(System.nanoTime());

        try {
            // 获取当前请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("RequestAttributes 为空，跳过日志记录");
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            // 创建操作日志对象
            SysOperLog operLog = new SysOperLog();
            operLog.setTitle(logAnnotation.title());
            operLog.setBusinessType(logAnnotation.businessType().getCode());
            operLog.setOperatorType(logAnnotation.operatorType().getCode());
            operLog.setMethod(getMethodFullName(joinPoint));
            operLog.setRequestMethod(request.getMethod());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperIp(getIpAddr(request));

            // 获取当前登录用户信息
            try {
                LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
                if (loginUser != null) {
                    operLog.setOperName(loginUser.getUsername());
                    // 通过部门服务获取部门名称
                    String deptName = "";
                    if (loginUser.getDeptId() != null) {
                        deptName = deptService.getDeptDisplayName(loginUser.getDeptId());
                    }
                    operLog.setDeptName(deptName);
                }
            } catch (Exception e) {
                log.debug("获取当前用户信息失败", e);
                operLog.setOperName("匿名用户");
                operLog.setDeptName("");
            }

            // 记录请求参数
            if (logAnnotation.saveRequestData()) {
                operLog.setOperParam(getRequestParams(joinPoint));
            }

            operLog.setOperTime(LocalDateTime.now());
            operLog.setStatus(0); // 默认成功状态

            // 将日志对象存入线程本地变量
            logThreadLocal.set(operLog);

            log.debug("操作日志前置处理完成: {}", operLog.getTitle());
        } catch (Exception e) {
            log.error("操作日志前置处理异常", e);
        }
    }

    /**
     * 处理带有 @Log 注解的方法 - 返回通知
     *
     * 记录方法执行成功后的信息：
     * 1. 执行结果
     * 2. 更新操作状态为成功
     *
     * @param joinPoint 连接点
     * @param logAnnotation 日志注解
     * @param result 方法返回值
     */
    @AfterReturning(pointcut = "@annotation(logAnnotation)", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Log logAnnotation, Object result) {
        try {
            SysOperLog operLog = logThreadLocal.get();
            if (operLog == null) {
                return;
            }

            // 计算操作消耗时间（纳秒级转毫秒）
            Long startTime = startTimeThreadLocal.get();
            if (startTime != null) {
                long durationNanos = System.nanoTime() - startTime;
                operLog.setCostTime(durationNanos / 1_000_000); // 纳秒转毫秒
            }

            // 记录返回结果
            if (logAnnotation.saveResponseData()) {
                operLog.setJsonResult(getResponseResult(result));
            }

            // 更新操作状态为成功
            operLog.setStatus(0);

            // 异步保存日志
            saveOperLog(operLog);

            log.debug("操作日志返回处理完成: {}", operLog.getTitle());
        } catch (Exception e) {
            log.error("操作日志返回处理异常", e);
        } finally {
            // 清理线程本地变量
            logThreadLocal.remove();
            startTimeThreadLocal.remove();
        }
    }

    /**
     * 处理带有 @Log 注解的方法 - 异常通知
     *
     * 记录方法执行异常时的信息：
     * 1. 异常信息
     * 2. 更新操作状态为失败
     *
     * @param joinPoint 连接点
     * @param logAnnotation 日志注解
     * @param exception 异常对象
     */
    @AfterThrowing(pointcut = "@annotation(logAnnotation)", throwing = "exception")
    public void doAfterThrowing(JoinPoint joinPoint, Log logAnnotation, Exception exception) {
        try {
            SysOperLog operLog = logThreadLocal.get();
            if (operLog == null) {
                return;
            }

            // 计算操作消耗时间（纳秒级转毫秒）
            Long startTime = startTimeThreadLocal.get();
            if (startTime != null) {
                long durationNanos = System.nanoTime() - startTime;
                operLog.setCostTime(durationNanos / 1_000_000); // 纳秒转毫秒
            }

            // 记录异常信息
            operLog.setStatus(1); // 失败状态
            operLog.setErrorMsg(exception.getMessage());

            // 异步保存日志
            saveOperLog(operLog);

            log.debug("操作日志异常处理完成: {}, 异常: {}", operLog.getTitle(), exception.getMessage());
        } catch (Exception e) {
            log.error("操作日志异常处理异常", e);
        } finally {
            // 清理线程本地变量
            logThreadLocal.remove();
            startTimeThreadLocal.remove();
        }
    }

    /**
     * 获取方法全名
     *
     * @param joinPoint 连接点
     * @return 方法全名
     */
    private String getMethodFullName(JoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
    }

    /**
     * 获取请求参数
     *
     * @param joinPoint 连接点
     * @return 请求参数JSON字符串
     */
    private String getRequestParams(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "";
            }

            // 过滤掉不需要序列化的参数类型
            Object[] filteredArgs = Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest) && !(arg instanceof HttpServletResponse))
                .toArray();

            if (filteredArgs.length == 0) {
                return "";
            }

            return objectMapper.writeValueAsString(filteredArgs);
        } catch (Exception e) {
            log.warn("序列化请求参数失败", e);
            return "";
        }
    }

    /**
     * 获取响应结果
     *
     * @param result 响应结果
     * @return 响应结果JSON字符串
     */
    private String getResponseResult(Object result) {
        try {
            if (result == null) {
                return "";
            }
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("序列化响应结果失败", e);
            return "";
        }
    }

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求对象
     * @return IP地址
     */
    private String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 对于多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    /**
     * 保存操作日志
     *
     * 调用日志服务异步保存到数据库
     *
     * @param operLog 操作日志对象
     */
    private void saveOperLog(SysOperLog operLog) {
        try {
            // 打印日志用于调试
            log.info("保存操作日志: {} - {} - {} - {}",
                operLog.getTitle(),
                operLog.getOperName(),
                operLog.getOperUrl(),
                operLog.getStatus() == 0 ? "成功" : "失败");

            // 检查日志对象是否完整
            log.info("操作日志对象详情: operId={}, title={}, operName={}, operUrl={}, status={}",
                operLog.getOperId(),
                operLog.getTitle(),
                operLog.getOperName(),
                operLog.getOperUrl(),
                operLog.getStatus());

            // 异步保存到数据库
            log.info("准备异步保存操作日志到数据库...");
            operLogService.insertOperLogAsync(operLog);
            log.info("操作日志异步保存任务已提交");
        } catch (Exception e) {
            log.error("保存操作日志失败", e);

            // 如果数据库保存失败，至少打印完整信息
            log.error("操作日志保存失败，详细信息: {}", operLog.toString());
        }
    }
}