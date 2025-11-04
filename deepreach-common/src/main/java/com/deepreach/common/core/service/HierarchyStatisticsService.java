package com.deepreach.common.core.service;

import java.util.Map;

/**
 * 基于用户树的统计服务，提供原部门统计接口的替代实现。
 */
public interface HierarchyStatisticsService {

    /**
     * 统计当前用户可管理范围内的身份数据。
     */
    Map<String, Object> getManagedDeptsStatistics(Long userId);

    /**
     * 统计代理层级及其业绩表现。
     */
    Map<String, Object> getManagedAgentLevelsStatistics(Long userId);

    /**
     * 统计买家主账号与子账号数据。
     */
    Map<String, Object> getManagedBuyerAccountsStatistics(Long userId);

    /**
     * 汇总仪表盘所需的核心统计指标。
     */
    Map<String, Object> getDashboardStatistics(Long userId);

    /**
     * 管理员查看代理业绩统计（总代/一级/二级）。
     */
    Map<String, Object> getAdminAgentPerformanceStatistics(Long userId);

    /**
     * 管理员查看商家业绩/实例统计。
     */
    Map<String, Object> getAdminMerchantsPerformanceStatistics(Long userId);

    /**
     * 管理员查看商家资产统计（总充值/总结算/净值）。
     */
    Map<String, Object> getAdminMerchantsAssetStatistics(Long userId);
}
