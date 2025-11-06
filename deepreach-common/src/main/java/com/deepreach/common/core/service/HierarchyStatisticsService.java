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

    /**
     * 统计指定代理的直属子节点情况。
     */
    Map<String, Object> getAgentChildrenStatistics(Long agentUserId);

    /**
     * 统计总代伞下的代理与商户贡献。
     *
     * @param generalAgentId 总代理用户ID
     * @return 统计数据
     */
    Map<String, Object> getGeneralAgentContributionStatistics(Long generalAgentId);

    /**
     * 统计一级代理伞下的代理与商户贡献。
     *
     * @param level1AgentId 一级代理用户ID
     * @return 统计数据
     */
    Map<String, Object> getLevel1AgentContributionStatistics(Long level1AgentId);

    /**
     * 统计二级代理伞下的商户贡献。
     *
     * @param level2AgentId 二级代理用户ID
     * @return 统计数据
     */
    Map<String, Object> getLevel2AgentContributionStatistics(Long level2AgentId);

    /**
     * 获取买家账户运营概览。
     *
     * @param buyerUserId 买家主账号ID
     * @return 包含余额、员工和实例统计的数据
     */
    Map<String, Object> getBuyerOperationalStatistics(Long buyerUserId);

    /**
     * 获取代理自身的佣金概览。
     *
     * @param agentUserId 代理用户ID
     * @return 佣金汇总（总收入、已结算、待结算）
     */
    Map<String, Object> getAgentCommissionOverview(Long agentUserId);
}
