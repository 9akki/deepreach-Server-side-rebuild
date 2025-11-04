package com.deepreach.web.initializer;

import com.deepreach.common.core.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 项目启动时初始化用户层级树并写入Redis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserHierarchyCacheInitializer implements CommandLineRunner {

    private final SysUserService userService;

    @Override
    public void run(String... args) {
        log.info("开始构建用户层级树缓存...");
        try {
            userService.rebuildUserHierarchyCache();
            log.info("用户层级树缓存构建完成");
        } catch (Exception e) {
            log.error("初始化用户层级树缓存失败", e);
        }
    }
}
