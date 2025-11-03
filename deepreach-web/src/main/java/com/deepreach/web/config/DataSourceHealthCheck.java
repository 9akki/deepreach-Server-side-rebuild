package com.deepreach.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据源健康检查
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
@Slf4j
@Component
public class DataSourceHealthCheck implements CommandLineRunner {

    @Autowired
    @Qualifier("primaryJdbcTemplate")
    private JdbcTemplate primaryJdbcTemplate;

    @Autowired
    @Qualifier("cloudComputerJdbcTemplate")
    private JdbcTemplate cloudComputerJdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始检查数据源配置...");

        // 检查主数据源
        try {
            String result = primaryJdbcTemplate.queryForObject("SELECT 'PRIMARY_OK'", String.class);
            log.info("主数据源连接成功: {}", result);
        } catch (Exception e) {
            log.error("主数据源连接失败", e);
        }

        // 检查云电脑数据源
        try {
            String result = cloudComputerJdbcTemplate.queryForObject("SELECT 'CLOUD_COMPUTER_OK'", String.class);
            log.info("云电脑数据源连接成功: {}", result);

            // 测试云电脑数据查询
            Integer count = cloudComputerJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_cloud_user WHERE client_username = 'admin'",
                Integer.class
            );
            log.info("云电脑数据库中admin用户记录数: {}", count);

        } catch (Exception e) {
            log.error("云电脑数据源连接失败", e);
        }

        log.info("数据源检查完成");
    }
}