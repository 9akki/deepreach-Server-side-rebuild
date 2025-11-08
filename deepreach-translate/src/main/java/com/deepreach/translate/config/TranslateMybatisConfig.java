package com.deepreach.translate.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.deepreach.translate.mapper")
public class TranslateMybatisConfig {
}
