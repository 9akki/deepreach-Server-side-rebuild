package com.deepreach.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 数据权限计算器
 *
 * 基于部门类型的数据权限计算器，负责计算用户的数据权限范围：
 * 1. 基于部门类型和层级计算数据权限
 * 2. 支持超级管理员特殊处理
 * 3. 支持组织架构的层级权限控制
 * 4. 优化数据权限查询性能
 *
 * 设计理念：
 * - 部门决定权限范围：不同部门类型有不同的数据权限
 * - 层级权限控制：支持部门层级的权限继承
 * - 简化权限逻辑：基于明确的权限规则
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Service
public class DataScopeCalculator {

    private static final Logger log = LoggerFactory.getLogger(DataScopeCalculator.class);

    /**
     * 超级管理员用户ID
     */
    private static final Long SUPER_ADMIN_USER_ID = 1L;

    /**
     * 超级管理员角色标识
     */
    private static final String SUPER_ADMIN_ROLE = "ADMIN";

    /**
     * 计算用户的数据权限范围
     * 根据用户的角色计算最大数据权限范围
     *
     * @param loginUser 当前登录用户
     * @return 数据权限范围代码
     */
    public String calculateDataScope(com.deepreach.common.core.domain.model.LoginUser loginUser) {
        if (loginUser == null) {
            log.warn("用户信息为空，默认本人数据权限");
            return "5";
        }

        // 超级管理员拥有全部数据权限
        if (isSuperAdmin(loginUser)) {
            log.info("超级管理员用户 {} 拥有全部数据权限", loginUser.getUsername());
            return "1";
        }

        // 根据用户的角色计算数据权限范围
        if (CollectionUtils.isEmpty(loginUser.getRoles())) {
            log.warn("用户 {} 没有分配角色，默认本人权限", loginUser.getUsername());
            return "5";
        }

        // 获取用户所有角色的数据权限范围，取最小值（最大权限）
        String maxDataScope = calculateMaxDataScopeFromRoles(loginUser.getRoles());

        log.debug("用户 {} 根据角色计算的数据权限范围: {}", loginUser.getUsername(), maxDataScope);
        return maxDataScope;
    }

    /**
     * 根据角色列表计算最大数据权限范围
     * 取最小数值代表最大权限（1 > 2 > 3 > 4 > 5）
     *
     * @param roles 用户角色标识集合
     * @return 最大数据权限范围
     */
    private String calculateMaxDataScopeFromRoles(Set<String> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return "5"; // 默认本人数据权限
        }

        // 基于新角色体系的数据权限优先级映射
        // 超级管理员：全部数据权限
        if (roles.contains("admin")) {
            return "1";
        }

        // 系统管理员角色：全部数据权限
        if (roles.contains("system_admin")
            || roles.contains("tech_admin")
            || roles.contains("ops_admin")) {
            return "1";
        }

        // 代理角色：本部门及以下数据权限
        if (roles.contains("agent")) {
            return "4";
        }

        // 买家总账号角色：本部门及以下数据权限
        if (roles.contains("buyer_main")) {
            return "4";
        }

        // 买家子账号角色：本人数据权限
        if (roles.contains("buyer_sub")) {
            return "5";
        }

        // 默认给予本人数据权限
        return "5";
    }

    /**
     * 构建数据权限SQL条件
     *
     * @param loginUser 当前登录用户
     * @param dataScope 数据权限范围
     * @param deptAlias 部门表别名
     * @param userAlias 用户表别名
     * @return SQL WHERE条件片段
     */
    public String buildDataScopeSql(com.deepreach.common.core.domain.model.LoginUser loginUser,
                                   String dataScope, String deptAlias, String userAlias) {
        if (StringUtils.isEmpty(dataScope)) {
            return "";
        }

        Long userId = loginUser.getUserId();
        Long deptId = loginUser.getDeptId();

        switch (dataScope) {
            case "1": // 全部数据权限
                return "";

            case "2": // 自定义数据权限
                return buildCustomDataScopeSql(userId, deptAlias, userAlias);

            case "3": // 部门数据权限
                return buildDeptDataScopeSql(deptId, deptAlias, userAlias);

            case "4": // 部门及子部门权限
                return buildDeptAndChildDataScopeSql(deptId, deptAlias, userAlias);

            case "5": // 本人数据权限
                return buildSelfDataScopeSql(userId, userAlias);

            default:
                log.warn("未知的数据权限范围: {}", dataScope);
                return "";
        }
    }

    /**
     * 构建全部数据权限SQL（无条件）
     */
    private String buildAllDataScopeSql() {
        return ""; // 不添加WHERE条件
    }

    /**
     * 构建自定义数据权限SQL
     */
    private String buildCustomDataScopeSql(Long userId, String deptAlias, String userAlias) {
        // 自定义权限可以根据业务需求扩展
        // 示例：可以访问本部门用户和下级部门用户的某些特定数据
        if (StringUtils.isEmpty(deptAlias) && StringUtils.isEmpty(userAlias)) {
            return String.format("%s.user_id = %d", userAlias != null ? userAlias : "u", userId);
        }

        return String.format("(%s.dept_id = %s.dept_id OR %s.user_id = %d)",
                          deptAlias, userAlias, userAlias, userId);
    }

    /**
     * 构建部门数据权限SQL
     */
    private String buildDeptDataScopeSql(Long deptId, String deptAlias, String userAlias) {
        if (StringUtils.isEmpty(deptAlias)) {
            return "";
        }

        return String.format("%s.dept_id = %d", deptAlias, deptId);
    }

    /**
     * 构建部门及子部门数据权限SQL
     */
    private String buildDeptAndChildDataScopeSql(Long deptId, String deptAlias, String userAlias) {
        if (StringUtils.isEmpty(deptAlias)) {
            return "";
        }

        try {
            // TODO: 这里需要调用部门服务查询子部门
            // 暂时只查询本部门数据
            log.warn("部门及子部门权限查询功能待实现，暂时使用本部门权限");
            return buildDeptDataScopeSql(deptId, deptAlias, userAlias);

        } catch (Exception e) {
            log.error("构建部门及子部门数据权限SQL失败", e);
            return buildDeptDataScopeSql(deptId, deptAlias, userAlias);
        }
    }

    /**
     * 构建本人数据权限SQL
     */
    private String buildSelfDataScopeSql(Long userId, String userAlias) {
        if (StringUtils.isEmpty(userAlias)) {
            return "";
        }

        return String.format("%s.user_id = %d", userAlias, userId);
    }

    /**
     * 判断是否为超级管理员
     *
     * @param loginUser 登录用户对象
     * @return 是否为超级管理员
     */
    private boolean isSuperAdmin(com.deepreach.common.core.domain.model.LoginUser loginUser) {
        if (loginUser == null) {
            return false;
        }

        // 方式1：通过用户ID判断（最直接）
        if (SUPER_ADMIN_USER_ID.equals(loginUser.getUserId())) {
            return true;
        }

        // 方式2：通过角色判断（更灵活）
        if (loginUser.getRoles() != null && loginUser.getRoles().contains(SUPER_ADMIN_ROLE)) {
            return true;
        }

        return false;
    }

    /**
     * 获取数据权限范围描述
     *
     * @param dataScope 数据权限范围代码
     * @return 数据权限范围描述
     */
    public String getDataScopeDescription(String dataScope) {
        switch (dataScope) {
            case "1":
                return "全部数据权限";
            case "2":
                return "自定义数据权限";
            case "3":
                return "部门数据权限";
            case "4":
                return "部门及子部门权限";
            case "5":
                return "本人数据权限";
            default:
                return "未知权限范围";
        }
    }

    /**
     * 获取所有数据权限范围
     *
     * @return 所有数据权限范围集合
     */
    public Set<String> getAllDataScopeTypes() {
        Set<String> dataScopes = new HashSet<>();
        dataScopes.add("1");
        dataScopes.add("2");
        dataScopes.add("3");
        dataScopes.add("4");
        dataScopes.add("5");
        return dataScopes;
    }
}