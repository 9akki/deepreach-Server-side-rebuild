package com.deepreach.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security 安全配置类
 *
 * 提供安全相关的 Bean 配置：
 * 1. 密码编码器配置
 * 2. 安全策略配置
 * 3. 认证管理器配置
 * 4. 权限控制配置
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-23
 */
@Configuration
public class SecurityConfig {

    /**
     * 密码编码器 Bean
     *
     * 使用 BCrypt 算法进行密码加密：
     * 1. BCrypt 是基于 Blowfish 加密算法的单向哈希函数
     * 2. 每次加密都会生成不同的 salt，增强安全性
     * 3. 内置强度因子，可调整计算复杂度
     * 4. 是目前推荐的最佳实践
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 密码编码器 Bean（自定义强度）
     *
     * 提供更高安全强度的密码编码器
     * strength 参数范围：4-31，默认为 10
     * 数值越高，安全性越高，但性能开销也越大
     *
     * @return 高强度的 BCryptPasswordEncoder 实例
     */
    @Bean("strongPasswordEncoder")
    public PasswordEncoder strongPasswordEncoder() {
        // 使用强度 12，提供更高的安全性
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 密码编码器 Bean（快速编码）
     *
     * 提供较低强度但更快的密码编码器
     * 适用于需要高性能的场景，如批量用户导入
     *
     * @return 快速的 BCryptPasswordEncoder 实例
     */
    @Bean("fastPasswordEncoder")
    public PasswordEncoder fastPasswordEncoder() {
        // 使用强度 8，提供更好的性能
        return new BCryptPasswordEncoder(8);
    }
}