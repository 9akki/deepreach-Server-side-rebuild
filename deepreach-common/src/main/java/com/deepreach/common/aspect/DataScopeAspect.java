package com.deepreach.common.aspect;

import com.deepreach.common.annotation.DataScope;
import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.security.DataScopeCalculator;
import com.deepreach.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 数据权限切面
 *
 * 基于用户层级的数据权限切面。
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Autowired
    private DataScopeCalculator dataScopeCalculator;

    /**
     * 数据权限SQL条件缓存
     * 使用线程本地变量存储，避免多线程问题
     */
    private static final ThreadLocal<Map<String, Object>> dataScopeContext = new ThreadLocal<>();

    /**
     * 数据权限SQL参数键名
     */
    private static final String DATA_SCOPE_SQL = "dataScopeSql";

    /**
     * 处理带有 @DataScope 注解的方法 - 前置通知
     *
     * 在方法执行前处理数据权限：
     * 1. 获取当前用户信息
     * 2. 计算数据权限范围
     * 3. 构建SQL条件
     * 4. 设置到方法参数中
     *
     * @param joinPoint 连接点
     * @param dataScope 数据权限注解
     */
  @Before(value = "@annotation(dataScope)")
    public void doBefore(JoinPoint joinPoint, DataScope dataScope) {
        try {
            // 初始化上下文
            Map<String, Object> context = new ConcurrentHashMap<>();
            dataScopeContext.set(context);

            // 获取当前登录用户信息
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                log.warn("当前用户未登录，跳过数据权限处理");
                return;
            }

            if (dataScopeCalculator.hasFullAccess(loginUser)) {
                log.debug("用户 {} 拥有全部数据权限，跳过过滤", loginUser.getUsername());
                return;
            }

            Set<Long> accessibleUserIds = dataScopeCalculator.calculateAccessibleUserIds(loginUser);
            String sqlCondition = buildUserScopeSql(dataScope, accessibleUserIds);

            context.put(DATA_SCOPE_SQL, sqlCondition);
            setDataScopeToParams(joinPoint, sqlCondition);

            log.debug("数据权限SQL条件构建完成: {}", sqlCondition);
        } catch (Exception e) {
            log.error("数据权限处理异常", e);
        } finally {
            // 清理线程本地变量
            dataScopeContext.remove();
        }
    }

    private String buildUserScopeSql(DataScope dataScope, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return "1=0";
        }
        String alias = dataScope.tableAlias();
        String fieldName = (alias == null || alias.isBlank())
            ? dataScope.userFieldName()
            : alias + "." + dataScope.userFieldName();

        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (Long id : userIds) {
            joiner.add(String.valueOf(id));
        }

        return fieldName + " IN " + joiner;
    }

    /**
     * 将数据权限SQL条件设置到方法参数中
     *
     * @param joinPoint 连接点
     * @param sqlCondition SQL条件
     */
    private void setDataScopeToParams(JoinPoint joinPoint, String sqlCondition) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }

        // 查找参数中的Map类型或实体对象
        for (Object arg : args) {
            if (arg instanceof Map) {
                // 如果参数是Map类型，直接设置
                @SuppressWarnings("unchecked")
                Map<String, Object> paramMap = (Map<String, Object>) arg;
                paramMap.put("dataScope", sqlCondition);
                log.debug("数据权限SQL条件已设置到Map参数中: {}", sqlCondition);
                return;
            } else {
                // 如果参数是实体对象，尝试通过反射设置dataScope属性
                try {
                    arg.getClass().getMethod("setDataScope", String.class).invoke(arg, sqlCondition);
                    log.debug("数据权限SQL条件已设置到实体参数中: {}", sqlCondition);
                    return;
                } catch (Exception e) {
                    // 忽略反射异常，继续尝试下一个参数
                }
            }
        }

        log.warn("未找到合适的参数类型设置数据权限SQL条件");
    }

    /**
     * 获取当前线程的数据权限SQL条件
     *
     * @return 数据权限SQL条件，如果不存在则返回空字符串
     */
    public static String getCurrentDataScopeSql() {
        Map<String, Object> context = dataScopeContext.get();
        if (context == null) {
            return "";
        }
        return (String) context.getOrDefault(DATA_SCOPE_SQL, "");
    }

    /**
     * 清理当前线程的数据权限上下文
     */
    public static void clearDataScopeContext() {
        dataScopeContext.remove();
    }
}
