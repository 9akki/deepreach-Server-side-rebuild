package com.deepreach.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置类
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.storage.base-path:}")
    private String configuredBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取文件存储的实际路径（与FileStorageServiceImpl逻辑保持一致）
        String uploadPath = resolveUploadPath(configuredBasePath).toAbsolutePath().toString();
        log.info("静态资源映射配置：/uploads/** -> file:{}", uploadPath);

        // 配置静态资源映射：将 /uploads/** 映射到实际文件存储路径
        // 注意：addResourceLocations需要以/结尾
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .setCachePeriod(3600); // 缓存1小时

        // 可以根据需要添加其他静态资源映射
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    private java.nio.file.Path resolveUploadPath(String configuredBasePath) {
        String basePath;
        if (configuredBasePath != null && !configuredBasePath.trim().isEmpty()) {
            basePath = configuredBasePath.trim();
        } else {
            String userHome = System.getProperty("user.home");
            if (userHome == null || userHome.trim().isEmpty()) {
                userHome = System.getProperty("user.dir");
            }
            basePath = Paths.get(userHome, "uploads").toString();
        }
        return Paths.get(basePath).toAbsolutePath().normalize();
    }
}