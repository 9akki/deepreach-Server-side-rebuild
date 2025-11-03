package com.deepreach.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * DeepReach应用启动类
 * 智能网页增强平台
 *
 * @author DeepReach Team
 * @since 1.0.0
 */
@SpringBootApplication(
    scanBasePackages = "com.deepreach",
    exclude = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
    }
)
@MapperScan({"com.deepreach.web.mapper", "com.deepreach.common.core.mapper"})
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class DeepReachApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepReachApplication.class, args);
        System.out.println("=====================================");
        System.out.println("🚀 DeepReach 智能网页增强平台启动成功!");
        System.out.println("📖 API文档: http://localhost:8080/swagger-ui.html");
        System.out.println("=====================================");
    }
}