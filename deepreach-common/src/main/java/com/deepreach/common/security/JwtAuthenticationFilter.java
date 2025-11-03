package com.deepreach.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * 处理每个请求的 JWT 认证：
 * 1. 从请求中提取 JWT Token
 * 2. 验证 Token 的有效性
 * 3. 解析 Token 获取用户信息
 * 4. 设置 Spring Security 上下文
 * 5. 支持用户权限加载
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Token 请求头名称
     */
    private static final String TOKEN_HEADER = "Authorization";

    /**
     * Token 前缀
     */
    private static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 过滤器的核心逻辑
     *
     * 对每个请求进行 JWT 认证处理：
     * 1. 提取 Token
     * 2. 验证 Token
     * 3. 加载用户详情
     * 4. 设置认证上下文
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        try {
            // 检查是否需要跳过认证
            if (shouldSkipAuthentication(request)) {
                log.debug("跳过认证：{}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // 从请求中提取 JWT Token
            String token = extractTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                log.debug("从请求中提取到Token: {}", token.substring(0, Math.min(20, token.length())) + "...");

                // 验证 Token 并获取用户名
                String username = jwtTokenUtil.getUsernameFromToken(token);
                log.debug("从Token中解析出用户名: {}", username);

                // 如果 Token 有效且当前没有认证信息
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    log.debug("开始加载用户详情: {}", username);

                    // 加载用户详情
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    log.debug("用户详情加载成功: {}", userDetails.getUsername());

                    // 验证 Token 是否有效
                    boolean isValid = jwtTokenUtil.validateToken(token, userDetails.getUsername());
                    log.debug("Token验证结果: {}", isValid);

                    if (isValid) {
                        // 创建认证令牌
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                            );

                        // 设置认证详情
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 设置认证上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.info("用户 {} 认证成功", username);
                    } else {
                        log.warn("用户 {} Token 验证失败", username);
                    }
                } else {
                    log.warn("Token解析的用户名为空或已有认证信息");
                }
            } else {
                log.warn("从请求中未能提取到有效Token");
            }
        } catch (Exception e) {
            log.error("JWT 认证过程中发生异常", e);
            // 清除认证上下文
            SecurityContextHolder.clearContext();
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 获取数据权限上下文
     * 供其他组件使用
     *
     * @return 数据权限SQL条件
     */
    public static String getDataScopeContext() {
        // 获取当前线程的数据权限SQL条件
        return com.deepreach.common.aspect.DataScopeAspect.getCurrentDataScopeSql();
    }

    /**
     * 从请求中提取 JWT Token
     *
     * 支持多种 Token 提取方式：
     * 1. Authorization 请求头（推荐）
     * 2. 查询参数（临时调试用）
     *
     * @param request HTTP 请求
     * @return JWT Token，如果没有找到则返回 null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 1. 从 Authorization 请求头中提取
        String bearerToken = request.getHeader(TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }

        // 2. 从查询参数中提取（仅用于开发调试）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            log.warn("通过查询参数传递 Token，存在安全风险，请使用请求头方式");
            return tokenParam;
        }

        return null;
    }

    /**
     * 判断是否需要跳过 JWT 认证的请求
     *
     * 某些公开接口不需要 JWT 认证：
     * 1. 登录接口
     * 2. 注册接口
     * 3. 静态资源
     * 4. 健康检查接口
     *
     * @param request HTTP 请求
     * @return true 如果需要跳过认证，false 否则
     */
    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        String requestURI = request.getRequestURI();

        // 定义不需要认证的路径
        String[] skipPaths = {
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/swagger-ui",
            "/v3/api-docs",
            "/druid",
            "/actuator"
        };

        for (String path : skipPaths) {
            if (requestURI.startsWith(path)) {
                return true;
            }
        }

        return false;
    }
}