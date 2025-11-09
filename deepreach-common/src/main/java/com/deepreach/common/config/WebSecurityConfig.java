package com.deepreach.common.config;

import com.deepreach.common.security.JwtAuthenticationEntryPoint;
import com.deepreach.common.security.JwtAuthenticationFilter;
import com.deepreach.common.security.CustomAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Web 安全配置类
 *
 * 配置 Spring Security 的认证和授权策略：
 * 1. HTTP 请求安全配置
 * 2. JWT 认证过滤器配置
 * 3. CORS 跨域配置
 * 4. 静态资源配置
 * 5. 公开接口配置
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * 安全过滤器链配置
     *
     * 配置 HTTP 安全策略：
     * 1. 禁用 CSRF（使用 JWT 时不需要）
     * 2. 配置会话管理为无状态
     * 3. 配置请求授权规则
     * 4. 添加 JWT 认证过滤器
     * 5. 配置异常处理
     *
     * @param http HTTP 安全配置对象
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF，因为使用 JWT
            .csrf(AbstractHttpConfigurer::disable)

            // 配置会话管理为无状态
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开接口 - 不需要认证
                .requestMatchers(
                    "/auth/login",
                    "/auth/register",
                    "/auth/refresh",
                    "/auth/logout",
                    "/auth/user/info",
                    "/auth/token/validate",
                    "/auth/password/forgot",
                    "/dr/balance/deduct",
                    "/dr/balance/pricing-mode",
                    "/files/template/**",
                    "/dr/price/{priceId}",
                    "/uploads/**",  // 公开文件访问 - 不需要认证
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/druid/**",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()

                // 系统接口 - 需要认证，具体权限由@PreAuthorize注解控制
                .requestMatchers("/system/**").authenticated()

                // 用户接口 - 需要认证
                .requestMatchers("/user/**").authenticated()

                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )

            // 配置异常处理
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // 添加 JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // 配置 CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    /**
     * 认证管理器配置
     *
     * 提供 Spring Security 的认证管理器 Bean
     * 用于处理用户认证逻辑
     *
     * @param config 认证配置对象
     * @return 认证管理器
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS 配置源
     *
     * 配置跨域资源共享策略：
     * 1. 允许的源
     * 2. 允许的 HTTP 方法
     * 3. 允许的请求头
     * 4. 是否支持凭证
     *
     * @return CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许所有源（生产环境应该限制具体域名）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允许凭证
        configuration.setAllowCredentials(true);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // 预检请求的缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
