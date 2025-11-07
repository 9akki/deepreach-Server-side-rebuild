package com.deepreach.web.service;

import java.util.Map;

/**
 * 买家实例统计服务
 *
 * 提供针对商家总账号及其子账户实例的统计能力。
 */
public interface BuyerInstanceStatisticsService {

    /**
     * 根据商家总账号用户ID统计其所有员工下的实例信息。
     *
     * @param buyerMainUserId 商家总账号用户ID
     * @return 统计结果，包含子账户及其实例明细
     */
    Map<String, Object> getBuyerSubInstanceStatistics(Long buyerMainUserId);

    /**
     * 根据商家总账号用户名统计其所有员工下的实例信息。
     *
     * @param buyerMainUsername 商家总账号用户名
     * @return 统计结果，包含子账户及其实例明细
     */
    Map<String, Object> getBuyerSubInstanceStatistics(String buyerMainUsername);

    /**
     * 根据商家总账号用户ID获取综合统计信息。
     *
     * @param buyerMainUserId 商家总账号用户ID
     * @return 综合统计信息，包括子部门/用户数量、实例类型与平台统计、AI人设数量统计以及DR账户信息
     */
    Map<String, Object> getBuyerHierarchyOverview(Long buyerMainUserId);
}
