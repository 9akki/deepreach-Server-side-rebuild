package com.deepreach.common.core.controller;

import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.service.HierarchyStatisticsService;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.web.domain.Result;
import com.deepreach.common.annotation.Log;
import com.deepreach.common.enums.BusinessType;
import com.deepreach.common.security.SecurityUtils;
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
     * 包括：系统部门、代理部门、买家总账户、买家子账户
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
     * 获取买家账户统计信息
     *
     * 统计当前用户管理的买家总账户和子账户数量
     * 包括每个买家总账户下的子账户数量
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
