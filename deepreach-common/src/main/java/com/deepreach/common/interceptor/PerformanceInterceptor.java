package com.deepreach.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 性能监控拦截器
 *
 * 监控请求的处理时间，用于性能分析和优化：
 * 1. 记录请求开始时间
 * 2. 计算请求处理时间
 * 3. 记录慢请求
 * 4. 统计API调用频率
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Slf4j
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    /**
     * 请求开始时间属性名
     */
    private static final String START_TIME_ATTRIBUTE = "startTime";

    /**
     * 慢请求阈值（毫秒）
     */
    private static final long SLOW_REQUEST_THRESHOLD = 3000;

    /**
     * 前置处理
     *
     * 在请求处理前记录开始时间
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @param handler 处理器
     * @return true继续处理，false中断处理
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);

        // 记录请求信息
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = getClientIpAddress(request);
        
        log.debug("开始处理请求: {} {} from {}", method, requestUri, remoteAddr);

        return true;
    }

    /**
     * 后置处理
     *
     * 在请求处理完成后计算处理时间并记录
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @param handler 处理器
     * @param ex 异常对象（如果有）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                             Object handler, Exception ex) {
        try {
            // 获取请求开始时间
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            if (startTime == null) {
                return;
            }

            // 计算处理时间
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // 获取请求信息
            String requestUri = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();
            String remoteAddr = getClientIpAddress(request);

            // 记录请求处理信息
            log.info("请求处理完成: {} {} from {} - 状态: {} - 耗时: {}ms", 
                method, requestUri, remoteAddr, status, executionTime);

            // 检查是否为慢请求
            if (executionTime > SLOW_REQUEST_THRESHOLD) {
                log.warn("检测到慢请求: {} {} from {} - 耗时: {}ms - 超过阈值: {}ms", 
                    method, requestUri, remoteAddr, executionTime, SLOW_REQUEST_THRESHOLD);
            }

            // 记录异常信息
            if (ex != null) {
                log.error("请求处理异常: {} {} from {} - 异常: {}", 
                    method, requestUri, remoteAddr, ex.getMessage(), ex);
            }

        } catch (Exception e) {
            log.error("性能监控拦截器处理异常", e);
        }
    }

    /**
     * 获取客户端真实IP地址
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
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
}