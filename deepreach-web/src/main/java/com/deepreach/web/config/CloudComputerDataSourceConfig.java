package com.deepreach.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.sql.Driver;

/**
 * 云电脑数据源配置
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
@Configuration
public class CloudComputerDataSourceConfig {

    @Value("${spring.cloud-computer.datasource.url}")
    private String url;

    @Value("${spring.cloud-computer.datasource.username}")
    private String username;

    @Value("${spring.cloud-computer.datasource.password}")
    private String password;

    @Value("${spring.cloud-computer.datasource.driver-class-name}")
    private String driverClassName;

    /**
     * 云电脑数据源
     */
    @Bean(name = "cloudComputerDataSource")
    public DataSource cloudComputerDataSource() {
        try {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);

            // 加载驱动类
            @SuppressWarnings("unchecked")
            Class<? extends Driver> driverClass = (Class<? extends Driver>) Class.forName(driverClassName);
            dataSource.setDriverClass(driverClass);

            return dataSource;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("无法加载数据库驱动: " + driverClassName, e);
        } catch (Exception e) {
            throw new RuntimeException("创建云电脑数据源失败", e);
        }
    }

    /**
     * 云电脑JdbcTemplate
     */
    @Bean(name = "cloudComputerJdbcTemplate")
    public JdbcTemplate cloudComputerJdbcTemplate() {
        return new JdbcTemplate(cloudComputerDataSource());
    }
}