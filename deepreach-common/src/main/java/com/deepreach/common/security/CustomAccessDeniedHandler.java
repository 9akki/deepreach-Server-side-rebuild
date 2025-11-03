package com.deepreach.common.security;

import com.deepreach.common.core.domain.model.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.deepreach.common.web.domain.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 自定义访问拒绝处理器
 *
 * 处理权限不足的请求，返回明确的错误信息：
 * 1. 拦截权限不足的请求
 * 2. 返回 403 禁止访问状态码
 * 3. 分析并告知用户缺少什么权限
 * 4. 提供详细的错误信息
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理权限不足的请求
     *
     * 首先判断用户是否已登录：
     * 1. 如果未登录 → 返回401 "未授权访问，请先登录"
     * 2. 如果已登录但权限不足 → 返回403 明确告知缺少什么权限
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param accessDeniedException 访问拒绝异常
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {

        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();

        log.warn("访问被拒绝: {} {} - {}", requestMethod, requestUri, accessDeniedException.getMessage());

        // 检查用户是否已登录
        LoginUser loginUser = null;
        try {
            loginUser = SecurityUtils.getCurrentLoginUser();
        } catch (Exception e) {
            log.debug("获取当前用户信息失败: {}", e.getMessage());
        }

        Result<?> result;

        if (loginUser == null) {
            // 用户未登录 → 返回401
            log.warn("未登录用户尝试访问受保护资源: {} {}", requestMethod, requestUri);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            result = Result.error(401, "未授权访问，请先登录");
        } else {
            // 用户已登录但权限不足 → 返回403 并详细说明权限
            log.warn("已登录用户权限不足: {} {} - 用户: {}", requestMethod, requestUri, loginUser.getUsername());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            // 简化的权限不足提示
            String errorMessage = "权限不足，请联系管理员分配相应权限";
            result = Result.error(403, errorMessage);
        }

        // 将响应对象转换为 JSON 字符串并写入响应
        String jsonResponse = objectMapper.writeValueAsString(result);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}