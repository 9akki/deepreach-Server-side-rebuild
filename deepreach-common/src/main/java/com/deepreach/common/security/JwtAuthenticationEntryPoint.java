package com.deepreach.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.deepreach.common.web.domain.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证入口点
 *
 * 处理未认证的请求，返回统一的错误响应：
 * 1. 拦截未认证的请求
 * 2. 返回 401 未授权状态码
 * 3. 提供统一的错误响应格式
 * 4. 支持中英文错误信息
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理认证失败的请求
     *
     * 当用户未提供有效的认证信息时调用此方法
     * 返回 JSON 格式的错误响应
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param authException 认证异常
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.warn("未授权访问: {} {}", request.getMethod(), request.getRequestURI(), authException);

        // 设置响应状态码和内容类型
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 构建错误响应
        Result<?> result = Result.error(401, "未授权访问，请先登录");

        // 将响应对象转换为 JSON 字符串并写入响应
        String jsonResponse = objectMapper.writeValueAsString(result);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}