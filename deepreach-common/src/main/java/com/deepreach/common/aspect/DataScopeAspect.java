package com.deepreach.common.aspect;

import com.deepreach.common.annotation.DataScope;
import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.service.SysDeptService;
import com.deepreach.common.security.DataScopeCalculator;
import com.deepreach.common.security.DeptUtils;
import com.deepreach.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限切面
 *
 * 处理带有 @DataScope 注解的方法，自动根据当前用户的数据权限范围过滤数据：
 * 1. 获取当前用户的数据权限范围
 * 2. 计算可访问的部门ID列表
 * 3. 构建数据权限SQL条件
 * 4. 将SQL条件设置到方法参数中
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Aspect
@Component
public class DataScopeAspect {

    private static final Logger log = LoggerFactory.getLogger(DataScopeAspect.class);

    @Autowired
    private SysDeptService deptService;

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

            // 计算用户的数据权限范围（使用DataScopeCalculator）
            String dataScopeValue = dataScopeCalculator.calculateDataScope(loginUser);
            Long userDeptId = loginUser.getDeptId();

            // 如果不是全部数据权限，需要获取可访问的部门ID列表
            List<Long> userChildDeptIds = new ArrayList<>();
            if (!"1".equals(dataScopeValue)) {
                userChildDeptIds = deptService.getAccessibleDeptIds(loginUser.getUserId());
            }

            log.debug("用户数据权限处理: 用户={}, 部门ID={}, 权限范围={}",
                loginUser.getUsername(), userDeptId, dataScopeValue);

            // 构建数据权限SQL条件（使用DataScopeCalculator）
            String sqlCondition = dataScopeCalculator.buildDataScopeSql(
                loginUser, dataScopeValue, dataScope.tableAlias(), dataScope.userFieldName());

            // 将SQL条件存入上下文
            context.put(DATA_SCOPE_SQL, sqlCondition);

            // 尝试将SQL条件设置到方法参数中
            setDataScopeToParams(joinPoint, sqlCondition);

            log.debug("数据权限SQL条件构建完成: {}", sqlCondition);
        } catch (Exception e) {
            log.error("数据权限处理异常", e);
        } finally {
            // 清理线程本地变量
            dataScopeContext.remove();
        }
    }

    /**
     * 构建数据权限SQL条件
     *
     * @param dataScope 数据权限注解
     * @param loginUser 登录用户信息
     * @param userDeptId 用户部门ID
     * @param dataScopeValue 数据权限范围值
     * @param userChildDeptIds 用户部门及子部门ID列表
     * @return SQL条件字符串
     */
    private String buildDataScopeSql(DataScope dataScope, LoginUser loginUser,
                                    Long userDeptId, String dataScopeValue, List<Long> userChildDeptIds) {
        // 如果数据权限范围为空或无效，返回空条件
        if (dataScopeValue == null || !DeptUtils.isValidDataScope(dataScopeValue)) {
            log.warn("用户数据权限范围无效: {}", dataScopeValue);
            return "";
        }

        // 全部数据权限
        if (DeptUtils.DataScope.ALL.equals(dataScopeValue)) {
            return "";
        }

        // 准备部门ID列表
        List<Long> accessibleDeptIds = new ArrayList<>();
        
        // 本部门数据权限
        if (DeptUtils.DataScope.DEPT.equals(dataScopeValue)) {
            if (userDeptId != null) {
                accessibleDeptIds.add(userDeptId);
            }
        }
        // 本部门及以下数据权限
        else if (DeptUtils.DataScope.DEPT_AND_CHILD.equals(dataScopeValue)) {
            if (userChildDeptIds != null && !userChildDeptIds.isEmpty()) {
                accessibleDeptIds.addAll(userChildDeptIds);
            }
        }
        // 自定义数据权限
        else if (DeptUtils.DataScope.CUSTOM.equals(dataScopeValue)) {
            if (userChildDeptIds != null && !userChildDeptIds.isEmpty()) {
                accessibleDeptIds.addAll(userChildDeptIds);
            }
        }

        // 如果没有可访问的部门，返回无权限条件
        if (accessibleDeptIds.isEmpty()) {
            return "1=0";
        }

        // 构建部门权限SQL条件
        String deptSql = DeptUtils.buildDeptPermissionSql(
            dataScope.tableAlias(), 
            dataScope.deptFieldName(), 
            accessibleDeptIds
        );

        // 如果启用个人数据权限，添加用户ID条件
        if (dataScope.enableUserPermission()) {
            String userSql = buildUserPermissionSql(dataScope, loginUser.getUserId());
            if (!userSql.isEmpty()) {
                if (!deptSql.isEmpty()) {
                    return "(" + deptSql + " OR " + userSql + ")";
                } else {
                    return userSql;
                }
            }
        }

        return deptSql;
    }

    /**
     * 构建个人数据权限SQL条件
     *
     * @param dataScope 数据权限注解
     * @param userId 用户ID
     * @return 用户权限SQL条件
     */
    private String buildUserPermissionSql(DataScope dataScope, Long userId) {
        if (userId == null) {
            return "";
        }

        String alias = dataScope.tableAlias();
        if (alias == null || alias.trim().isEmpty()) {
            alias = "";
        } else {
            alias = alias + ".";
        }

        return alias + dataScope.userFieldName() + " = " + userId;
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