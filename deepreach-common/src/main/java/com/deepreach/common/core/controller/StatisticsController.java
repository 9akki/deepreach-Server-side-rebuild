package com.deepreach.common.core.controller;

import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.service.HierarchyStatisticsService;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.web.domain.Result;
import com.deepreach.common.annotation.Log;
import com.deepreach.common.enums.BusinessType;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.common.security.enums.UserIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据统计Controller
 *
 * 提供各种数据统计功能，包括：
 * 1. 用户管理部门统计
 * 2. 部门类型统计
 * 3. 用户数量统计
 * 4. 代理层级统计
 * 5. 业绩相关统计
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private HierarchyStatisticsService statisticsService;

    @Autowired
    private SysUserService userService;

    /**
     * 获取当前用户管理部门的统计信息
     *
     * 统计当前用户作为leader的部门树中各种类型部门的数量
     * 包括：系统部门、代理部门、商家总账号、员工
     *
     * @return 统计信息
     */
    @GetMapping("/managed-depts")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getManagedDeptsStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> statistics = statisticsService.getManagedDeptsStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取管理部门统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前用户管理部门的用户统计
     *
     * 统计当前用户作为leader的部门树中所有用户的数量
     * 按用户类型和状态分组统计
     *
     * @return 用户统计信息
     */
    @GetMapping("/managed-users")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getManagedUsersStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> statistics = userService.getManagedUsersStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取管理用户统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取代理层级统计信息
     *
     * 统计当前用户管理的代理层级分布
     * 包括一级代理、二级代理、三级代理的数量
     *
     * @return 代理层级统计信息
     */
    @GetMapping("/agent-levels")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getAgentLevelsStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> statistics = statisticsService.getManagedAgentLevelsStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取代理层级统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 一级代理伞下统计。
     */
    @GetMapping("/agent/level1/subtree")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getLevel1AgentSubtreeStatistics() {
        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                return Result.error("用户未登录");
            }
            if (!loginUser.hasIdentity(UserIdentity.AGENT_LEVEL_1)) {
                return Result.error("仅 level1 代理可以访问该统计");
            }

            Map<String, Object> statistics = statisticsService.getGeneralAgentContributionStatistics(loginUser.getUserId());
            if (statistics.containsKey("error")) {
                return Result.error(statistics.get("error").toString());
            }
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取 level1 代理伞下统计失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 二级代理伞下统计。
     */
    @GetMapping("/agent/level2/subtree")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getLevel2AgentSubtreeStatistics() {
        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                return Result.error("用户未登录");
            }
            if (!loginUser.hasIdentity(UserIdentity.AGENT_LEVEL_2)) {
                return Result.error("仅 level2 代理可以访问该统计");
            }

            Map<String, Object> statistics = statisticsService.getLevel1AgentContributionStatistics(loginUser.getUserId());
            if (statistics.containsKey("error")) {
                return Result.error(statistics.get("error").toString());
            }
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取 level2 代理伞下统计失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 三级代理伞下统计。
     */
    @GetMapping("/agent/level3/subtree")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getLevel3AgentSubtreeStatistics() {
        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                return Result.error("用户未登录");
            }
            if (!loginUser.hasIdentity(UserIdentity.AGENT_LEVEL_3)) {
                return Result.error("仅 level3 代理可以访问该统计");
            }

            Map<String, Object> statistics = statisticsService.getLevel2AgentContributionStatistics(loginUser.getUserId());
            if (statistics.containsKey("error")) {
                return Result.error(statistics.get("error").toString());
            }
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取 level3 代理伞下统计失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 代理佣金概览（基于 token）。
     */
    @GetMapping("/agent/commission-overview")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getAgentCommissionOverview() {
        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                return Result.error("用户未登录");
            }
            if (!loginUser.hasAnyIdentity(UserIdentity.AGENT_LEVEL_1, UserIdentity.AGENT_LEVEL_2, UserIdentity.AGENT_LEVEL_3)) {
                return Result.error("仅代理用户可以访问该统计");
            }

            Map<String, Object> overview = statisticsService.getAgentCommissionOverview(loginUser.getUserId());
            if (overview.containsKey("error")) {
                return Result.error(overview.get("error").toString());
            }
            return Result.success(overview);
        } catch (Exception e) {
            log.error("获取代理佣金概览失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 管理员代理业绩统计
     */
    @GetMapping("/adminAgentPerformanceStatistics")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getAdminAgentPerformanceStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null || !loginUser.isAdminIdentity()) {
                return Result.error("仅管理员可以查看该统计");
            }

            Map<String, Object> statistics = statisticsService.getAdminAgentPerformanceStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取管理员代理业绩统计失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 管理员商家业绩统计
     */
    @GetMapping("/adminMerchantsPerformanceStatistics")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getAdminMerchantsPerformanceStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null || !loginUser.isAdminIdentity()) {
                return Result.error("仅管理员可以查看该统计");
            }

            Map<String, Object> statistics = statisticsService.getAdminMerchantsPerformanceStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取管理员商家业绩统计失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 管理员商家资产统计
     */
    @GetMapping("/adminMerchantsAssetStatistics")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getAdminMerchantsAssetStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null || !loginUser.isAdminIdentity()) {
                return Result.error("仅管理员可以查看该统计");
            }

            Map<String, Object> statistics = statisticsService.getAdminMerchantsAssetStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取管理员商家资产统计失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 买家账户运营概览
     */
    @GetMapping("/buyer/{buyerId}/overview")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getBuyerOverview(@PathVariable("buyerId") Long buyerId) {
        try {
            if (buyerId == null || buyerId <= 0) {
                return Result.error("买家ID无效");
            }
            Map<String, Object> overview = statisticsService.getBuyerOperationalStatistics(buyerId);
            if (overview.containsKey("error")) {
                return Result.error(overview.get("error").toString());
            }
            return Result.success(overview);
        } catch (Exception e) {
            log.error("获取买家运营概览失败：buyerId={}", buyerId, e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    @GetMapping("/agent/general/children")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getGeneralAgentChildren(@RequestParam Long userId) {
        return getAgentChildrenStatisticsWithIdentity(userId, com.deepreach.common.security.enums.UserIdentity.AGENT_LEVEL_1);
    }

    @GetMapping("/agent/level1/children")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getLevel1AgentChildren(@RequestParam Long userId) {
        return getAgentChildrenStatisticsWithIdentity(userId, com.deepreach.common.security.enums.UserIdentity.AGENT_LEVEL_2);
    }

    @GetMapping("/agent/level2/children")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getLevel2AgentChildren(@RequestParam Long userId) {
        return getAgentChildrenStatisticsWithIdentity(userId, com.deepreach.common.security.enums.UserIdentity.AGENT_LEVEL_3);
    }

    private Result getAgentChildrenStatisticsWithIdentity(Long userId, com.deepreach.common.security.enums.UserIdentity expected) {
        try {
            if (userId == null || userId <= 0) {
                return Result.error("用户ID无效");
            }
            Map<String, Object> statistics = statisticsService.getAgentChildrenStatistics(userId);
            if (statistics.containsKey("error")) {
                return Result.error(statistics.get("error").toString());
            }
            Object identity = statistics.get("identity");
            if (identity == null || !expected.getRoleKey().equals(identity)) {
                return Result.error("用户身份不匹配，预期：" + expected.getRoleKey());
            }
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取代理直属子用户统计失败：userId={}, expected={}", userId, expected, e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取买家账户统计信息
     *
     * 统计当前用户管理的商家总账号和子账户数量
     * 包括每个商家总账号下的子账户数量
     *
     * @return 买家账户统计信息
     */
    @GetMapping("/buyer-accounts")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getBuyerAccountsStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> statistics = statisticsService.getManagedBuyerAccountsStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取买家账户统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取综合统计仪表板数据
     *
     * 提供一个综合的统计仪表板，包含各种关键指标
     * 适合用于首页展示或管理概览
     *
     * @return 综合统计信息
     */
    @GetMapping("/dashboard")
    @Log(title = "统计管理", businessType = BusinessType.OTHER)
    public Result getDashboardStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> dashboard = statisticsService.getDashboardStatistics(currentUserId);
            return Result.success(dashboard);
        } catch (Exception e) {
            log.error("获取仪表板统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取当前用户ID
     *
     * @return 当前用户ID，获取失败返回null
     */
    private Long getCurrentUserId() {
        try {
            return com.deepreach.common.security.SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            return null;
        }
    }
}
