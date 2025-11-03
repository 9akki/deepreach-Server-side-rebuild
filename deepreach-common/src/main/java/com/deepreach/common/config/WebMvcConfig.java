package com.deepreach.common.config;

import com.deepreach.common.interceptor.PerformanceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 *
 * 配置Spring MVC的相关设置：
 * 1. 拦截器注册和配置
 * 2. 跨域资源共享配置
 * 3. 静态资源处理
 * 4. 消息转换器配置
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 注册拦截器
     *
     * 配置自定义拦截器的拦截路径和排除路径：
     * 1. 性能监控拦截器
     * 2. 其他业务拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册性能监控拦截器
        registry.addInterceptor(new PerformanceInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/auth/login",
                    "/auth/register",
                    "/auth/logout",
                    "/auth/refresh",
                    "/auth/user/info",
                    "/auth/token/validate",
                    "/auth/password/forgot",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/druid/**",
                    "/actuator/**",
                    "/static/**",
                    "/public/**",
                    "/error",
                    "/favicon.ico"
                );

        // 可以在这里添加其他拦截器
        // registry.addInterceptor(new OtherInterceptor())
        //         .addPathPatterns("/**")
        //         .excludePathPatterns("/auth/**");
    }

    /**
     * 配置跨域资源共享
     *
     * 配置CORS策略，允许跨域请求：
     * 1. 允许的源
     * 2. 允许的HTTP方法
     * 3. 允许的请求头
     * 4. 是否支持凭证
     *
     * @param registry CORS注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization", "Content-Type")
                .maxAge(3600);
    }

    /**
     * 配置静态资源处理
     *
     * 配置静态资源的访问路径和实际位置：
     * 1. 类路径资源
     * 2. 文件系统资源
     * 3. WebJars资源
     *
     * @param registry 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源处理
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // WebJars资源处理
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        // 文件上传资源处理
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}