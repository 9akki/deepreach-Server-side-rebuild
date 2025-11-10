package com.deepreach.common.core.service.impl;

import com.deepreach.common.core.domain.entity.DrPriceConfig;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.mapper.AgentCommissionAccountStatMapper;
import com.deepreach.common.core.mapper.SysUserMapper;
import com.deepreach.common.core.service.DrPriceConfigService;
import com.deepreach.common.core.service.HierarchyStatisticsService;
import com.deepreach.common.core.service.UserHierarchyService;
import com.deepreach.common.security.UserRoleUtils;
import com.deepreach.common.security.enums.UserIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 用户树统计实现，替代原部门统计逻辑。
 */
@Slf4j
@Service
public class HierarchyStatisticsServiceImpl implements HierarchyStatisticsService {

    private static final BigDecimal LEVEL1_COMMISSION_RATE = new BigDecimal("0.30");
    private static final BigDecimal LEVEL2_COMMISSION_RATE = new BigDecimal("0.20");
    private static final BigDecimal LEVEL3_COMMISSION_RATE = new BigDecimal("0.10");
    private static final int MONEY_SCALE = 6;
    private static final Set<String> MARKETING_PLATFORM_TYPES =
        Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("marketing")));
    private static final Set<String> PROSPECTING_PLATFORM_TYPES =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList("customer-acquisition", "prospecting")));
    private static final List<String> MARKETING_DISPLAY_PLATFORMS =
        Collections.unmodifiableList(Arrays.asList("Telegram", "WhatsApp", "Facebook", "Line"));
    private static final List<String> PROSPECTING_DISPLAY_PLATFORMS =
        Collections.unmodifiableList(Arrays.asList("Instagram", "TikTok", "Facebook", "X"));

    private static final Map<UserIdentity, List<UserIdentity>> DIRECT_CHILDREN_BY_IDENTITY = Map.of(
        UserIdentity.AGENT_LEVEL_1, Arrays.asList(UserIdentity.AGENT_LEVEL_2, UserIdentity.AGENT_LEVEL_3, UserIdentity.BUYER_MAIN),
        UserIdentity.AGENT_LEVEL_2, Arrays.asList(UserIdentity.AGENT_LEVEL_3, UserIdentity.BUYER_MAIN),
        UserIdentity.AGENT_LEVEL_3, Collections.singletonList(UserIdentity.BUYER_MAIN)
    );

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private AgentCommissionAccountStatMapper agentCommissionAccountMapper;

    @Autowired
    private DrPriceConfigService drPriceConfigService;

    @Autowired(required = false)
    private UserHierarchyService hierarchyService;

    @Override
    public Map<String, Object> getManagedDeptsStatistics(Long userId) {
        Map<String, Object> statistics = new LinkedHashMap<>();

        try {
            if (userId == null || userId <= 0) {
                return statistics;
            }

            Set<Long> managedUserIds = collectManagedUserIds(userId);
            if (managedUserIds.isEmpty()) {
                initializeDeptStatistics(statistics, 0L, 0L, 0L, 0L, 0L, 0L);
                statistics.put("totalUsers", 0L);
                statistics.put("totalAgents", 0L);
                statistics.put("managedUserIds", Collections.emptySet());
                statistics.put("identityBreakdown", Collections.emptyMap());
                statistics.put("unknownUserCount", 0L);
                return statistics;
            }

            Map<UserIdentity, Set<Long>> membership = resolveIdentityMembership(managedUserIds);
            long activeUserTotal = Optional.ofNullable(userMapper.countActiveUsersByIds(managedUserIds)).orElse(0L);

            Map<UserIdentity, Long> identityCounts = new EnumMap<>(UserIdentity.class);
            Set<Long> knownUsers = new LinkedHashSet<>();
            for (UserIdentity identity : UserIdentity.values()) {
                Set<Long> users = membership.getOrDefault(identity, Collections.emptySet());
                identityCounts.put(identity, (long) users.size());
                knownUsers.addAll(users);
            }

            long systemCount = identityCounts.getOrDefault(UserIdentity.ADMIN, 0L);
            long level1AgentCount = identityCounts.getOrDefault(UserIdentity.AGENT_LEVEL_1, 0L);
            long level2AgentCount = identityCounts.getOrDefault(UserIdentity.AGENT_LEVEL_2, 0L);
            long level3AgentCount = identityCounts.getOrDefault(UserIdentity.AGENT_LEVEL_3, 0L);
            long buyerMainCount = identityCounts.getOrDefault(UserIdentity.BUYER_MAIN, 0L);
            long buyerSubCount = identityCounts.getOrDefault(UserIdentity.BUYER_SUB, 0L);

            long agentTotal = level1AgentCount + level2AgentCount + level3AgentCount;
            long knownTotal = knownUsers.size();
            long unknownCount = Math.max(0L, activeUserTotal - knownTotal);

            initializeDeptStatistics(statistics, systemCount, level1AgentCount, level2AgentCount, level3AgentCount,
                buyerMainCount, buyerSubCount);
            statistics.put("totalUsers", activeUserTotal);
            statistics.put("totalAgents", agentTotal);
            statistics.put("managedUserIds", Collections.unmodifiableSet(new LinkedHashSet<>(managedUserIds)));
            statistics.put("identityBreakdown", buildIdentityBreakdown(identityCounts, unknownCount));
            statistics.put("unknownUserCount", unknownCount);

            log.info("User {} managed identities: admin={}, lvl1={}, lvl2={}, lvl3={}, buyerMain={}, buyerSub={}, unknown={}, totalActive={}",
                userId, systemCount, level1AgentCount, level2AgentCount, level3AgentCount, buyerMainCount, buyerSubCount, unknownCount, activeUserTotal);

        } catch (Exception e) {
            log.error("Failed to build managed identity statistics: userId={}", userId, e);
            initializeDeptStatistics(statistics, 0L, 0L, 0L, 0L, 0L, 0L);
            statistics.put("totalUsers", 0L);
            statistics.put("totalAgents", 0L);
            statistics.put("managedUserIds", Collections.emptySet());
            statistics.put("identityBreakdown", Collections.emptyMap());
            statistics.put("unknownUserCount", 0L);
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getManagedAgentLevelsStatistics(Long userId) {
        Map<String, Object> statistics = new LinkedHashMap<>();

        try {
            if (userId == null || userId <= 0) {
                return statistics;
            }

            Set<Long> managedUserIds = collectManagedUserIds(userId);
            if (managedUserIds.isEmpty()) {
                initializeAgentStatistics(statistics, 0L, 0L, 0L);
                statistics.put("totalAgents", 0L);
                statistics.put("managedUserIds", Collections.emptySet());
                statistics.put("rechargeStatistics", Collections.emptyMap());
                statistics.put("commissionStatistics", Collections.emptyMap());
                statistics.put("merchantPerformance", Collections.emptyMap());
                return statistics;
            }

            Map<UserIdentity, Set<Long>> membership = resolveIdentityMembership(managedUserIds);

            Set<Long> level1Agents = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.AGENT_LEVEL_1, Collections.emptySet()));
            Set<Long> level2Agents = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.AGENT_LEVEL_2, Collections.emptySet()));
            Set<Long> level3Agents = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.AGENT_LEVEL_3, Collections.emptySet()));

            long level1Count = level1Agents.size();
            long level2Count = level2Agents.size();
            long level3Count = level3Agents.size();
            long totalAgents = level1Count + level2Count + level3Count;

            initializeAgentStatistics(statistics, level1Count, level2Count, level3Count);
            statistics.put("totalAgents", totalAgents);
            statistics.put("managedUserIds", Collections.unmodifiableSet(new LinkedHashSet<>(managedUserIds)));

            Set<Long> buyerMainIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_MAIN, Collections.emptySet()));
            Set<Long> buyerSubIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_SUB, Collections.emptySet()));
            Set<Long> buyerUserIds = new LinkedHashSet<>(buyerMainIds);
            buyerUserIds.addAll(buyerSubIds);

            Map<Long, BigDecimal> rechargeMap = fetchRechargeByUserIds(buyerUserIds);
            Map<Long, BigDecimal> commissionMap = fetchCommissionByUserIds(managedUserIds);

            statistics.put("rechargeStatistics",
                buildAgentRechargeStatistics(level1Agents, level2Agents, level3Agents, buyerMainIds, buyerSubIds, rechargeMap));
            statistics.put("commissionStatistics",
                buildAgentCommissionStatistics(level1Agents, level2Agents, level3Agents, commissionMap));
            statistics.put("merchantPerformance",
                buildMerchantPerformance(buyerMainIds, buyerSubIds, rechargeMap, buyerUserIds));

        } catch (Exception e) {
            log.error("Failed to build agent level statistics: userId={}", userId, e);
            initializeAgentStatistics(statistics, 0L, 0L, 0L);
            statistics.put("totalAgents", 0L);
            statistics.put("managedUserIds", Collections.emptySet());
            statistics.put("rechargeStatistics", Collections.emptyMap());
            statistics.put("commissionStatistics", Collections.emptyMap());
            statistics.put("merchantPerformance", Collections.emptyMap());
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getManagedBuyerAccountsStatistics(Long userId) {
        Map<String, Object> statistics = new LinkedHashMap<>();

        try {
            if (userId == null || userId <= 0) {
                return statistics;
            }

            Set<Long> managedUserIds = collectManagedUserIds(userId);
            if (managedUserIds.isEmpty()) {
                initializeBuyerStatistics(statistics, 0L, 0L);
                statistics.put("totalBuyerAccounts", 0L);
                statistics.put("buyerMainDetails", Collections.emptyList());
                statistics.put("managedUserIds", Collections.emptySet());
                return statistics;
            }

            Map<UserIdentity, Set<Long>> membership = resolveIdentityMembership(managedUserIds);
            Set<Long> buyerMainIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_MAIN, Collections.emptySet()));
            Set<Long> buyerSubIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_SUB, Collections.emptySet()));

            Set<Long> buyerUserIds = new LinkedHashSet<>(buyerMainIds);
            buyerUserIds.addAll(buyerSubIds);
            Map<Long, BigDecimal> rechargeMap = fetchRechargeByUserIds(buyerUserIds);

            long buyerMainCount = buyerMainIds.size();
            long buyerSubCount = buyerSubIds.size();

            initializeBuyerStatistics(statistics, buyerMainCount, buyerSubCount);
            statistics.put("totalBuyerAccounts", buyerMainCount + buyerSubCount);
            statistics.put("buyerMainDetails", buildBuyerMainDetails(buyerMainIds, buyerSubIds, rechargeMap));
            statistics.put("managedUserIds", Collections.unmodifiableSet(new LinkedHashSet<>(managedUserIds)));

        } catch (Exception e) {
            log.error("Failed to build buyer account statistics: userId={}", userId, e);
            initializeBuyerStatistics(statistics, 0L, 0L);
            statistics.put("totalBuyerAccounts", 0L);
            statistics.put("buyerMainDetails", Collections.emptyList());
            statistics.put("managedUserIds", Collections.emptySet());
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getDashboardStatistics(Long userId) {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        try {
            if (userId == null || userId <= 0) {
                return dashboard;
            }

            Map<String, Object> deptStats = getManagedDeptsStatistics(userId);
            Map<String, Object> agentStats = getManagedAgentLevelsStatistics(userId);
            Map<String, Object> buyerStats = getManagedBuyerAccountsStatistics(userId);

            dashboard.put("identityStatistics", deptStats);
            dashboard.put("agentStatistics", agentStats);
            dashboard.put("buyerStatistics", buyerStats);

        } catch (Exception e) {
            log.error("Failed to build dashboard statistics: userId={}", userId, e);
        }

        return dashboard;
    }

    @Override
    public Map<String, Object> getAdminAgentPerformanceStatistics(Long userId) {
        Map<String, Object> statistics = new LinkedHashMap<>();

        try {
            if (userId == null || userId <= 0) {
                return statistics;
            }

            // 收集可管理的用户ID
            Set<Long> managedUserIds = collectManagedUserIds(userId);
            if (managedUserIds.isEmpty()) {
                statistics.put("generalAgent", buildEmptyPerformance(UserIdentity.AGENT_LEVEL_1.getRoleKey(), "总代"));
                statistics.put("level1Agent", buildEmptyPerformance(UserIdentity.AGENT_LEVEL_2.getRoleKey(), "一级代理"));
                statistics.put("level2Agent", buildEmptyPerformance(UserIdentity.AGENT_LEVEL_3.getRoleKey(), "二级代理"));
                statistics.put("total", buildEmptyPerformance("total", "合计"));
                return statistics;
            }

            Map<UserIdentity, Set<Long>> membership = resolveIdentityMembership(managedUserIds);

            Set<Long> level1Agents = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.AGENT_LEVEL_1, Collections.emptySet()));
            Set<Long> level2Agents = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.AGENT_LEVEL_2, Collections.emptySet()));
            Set<Long> level3Agents = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.AGENT_LEVEL_3, Collections.emptySet()));
            Set<Long> buyerMainIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_MAIN, Collections.emptySet()));
            Set<Long> buyerSubIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_SUB, Collections.emptySet()));

            Set<Long> buyerUserIds = new LinkedHashSet<>(buyerMainIds);
            buyerUserIds.addAll(buyerSubIds);

            Map<Long, BigDecimal> rechargeMap = fetchRechargeByUserIds(buyerUserIds);
            Map<Long, BigDecimal> commissionMap = fetchCommissionByUserIds(managedUserIds);

            Map<String, Object> generalPerformance = buildAgentPerformance(
                UserIdentity.AGENT_LEVEL_1.getRoleKey(), "总代",
                level1Agents, buyerMainIds, buyerSubIds, rechargeMap, commissionMap);
            Map<String, Object> level1Performance = buildAgentPerformance(
                UserIdentity.AGENT_LEVEL_2.getRoleKey(), "一级代理",
                level2Agents, buyerMainIds, buyerSubIds, rechargeMap, commissionMap);
            Map<String, Object> level2Performance = buildAgentPerformance(
                UserIdentity.AGENT_LEVEL_3.getRoleKey(), "二级代理",
                level3Agents, buyerMainIds, buyerSubIds, rechargeMap, commissionMap);

            BigDecimal totalRecharge = toBigDecimal(generalPerformance.get("totalRecharge"))
                .add(toBigDecimal(level1Performance.get("totalRecharge")))
                .add(toBigDecimal(level2Performance.get("totalRecharge")));
            BigDecimal totalCommission = toBigDecimal(generalPerformance.get("totalCommission"))
                .add(toBigDecimal(level1Performance.get("totalCommission")))
                .add(toBigDecimal(level2Performance.get("totalCommission")));
            long totalAgentCount = Optional.ofNullable(parseLong(generalPerformance.get("agentCount"))).orElse(0L)
                + Optional.ofNullable(parseLong(level1Performance.get("agentCount"))).orElse(0L)
                + Optional.ofNullable(parseLong(level2Performance.get("agentCount"))).orElse(0L);

            Map<String, Object> totalPerformance = new LinkedHashMap<>();
            totalPerformance.put("identity", "total");
            totalPerformance.put("identityDisplay", "合计");
            totalPerformance.put("agentCount", totalAgentCount);
            totalPerformance.put("totalRecharge", formatAmount(totalRecharge));
            totalPerformance.put("totalCommission", formatAmount(totalCommission));

            statistics.put("generalAgent", generalPerformance);
            statistics.put("level1Agent", level1Performance);
            statistics.put("level2Agent", level2Performance);
            statistics.put("total", totalPerformance);

        } catch (Exception e) {
            log.error("Failed to build admin agent performance statistics: userId={}", userId, e);
            statistics.put("generalAgent", buildEmptyPerformance(UserIdentity.AGENT_LEVEL_1.getRoleKey(), "总代"));
            statistics.put("level1Agent", buildEmptyPerformance(UserIdentity.AGENT_LEVEL_2.getRoleKey(), "一级代理"));
            statistics.put("level2Agent", buildEmptyPerformance(UserIdentity.AGENT_LEVEL_3.getRoleKey(), "二级代理"));
            statistics.put("total", buildEmptyPerformance("total", "合计"));
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getAdminMerchantsPerformanceStatistics(Long userId) {
        Map<String, Object> statistics = new LinkedHashMap<>();

        try {
            if (userId == null || userId <= 0) {
                return statistics;
            }

            Set<Long> managedUserIds = collectManagedUserIds(userId);
            if (managedUserIds.isEmpty()) {
                statistics.put("merchantOverview", buildEmptyMerchantOverview());
                statistics.put("aiCharacterOverview", buildAiCharacterStatistics(Collections.emptySet()));
                statistics.put("marketingInstanceOverview", buildEmptyInstanceOverview());
                statistics.put("prospectingInstanceOverview", buildEmptyInstanceOverview());
                return statistics;
            }

            Map<UserIdentity, Set<Long>> membership = resolveIdentityMembership(managedUserIds);
            Set<Long> buyerMainIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_MAIN, Collections.emptySet()));
            Set<Long> buyerSubIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_SUB, Collections.emptySet()));
            Set<Long> buyerUserIds = new LinkedHashSet<>(buyerMainIds);
            buyerUserIds.addAll(buyerSubIds);

            Map<Long, BigDecimal> rechargeMap = fetchRechargeByUserIds(buyerMainIds);
            BigDecimal totalRecharge = formatAmount(sumRechargeForUsers(buyerMainIds, rechargeMap));

            Map<String, Object> merchantOverview = new LinkedHashMap<>();
            merchantOverview.put("merchantCount", (long) buyerMainIds.size());
            merchantOverview.put("employeeCount", (long) buyerSubIds.size());
            merchantOverview.put("totalPerformance", totalRecharge);
            statistics.put("merchantOverview", merchantOverview);

            statistics.put("aiCharacterOverview", buildAiCharacterStatistics(buyerUserIds));

            Map<String, Object> instanceStats = buildInstanceStatistics(buyerUserIds);
            Map<String, Object> marketingOverview = new LinkedHashMap<>();
            marketingOverview.put("instanceCount", instanceStats.getOrDefault("marketingInstanceCount", 0L));
            marketingOverview.put("platformBreakdown", instanceStats.getOrDefault("marketingPlatformBreakdown", Collections.emptyMap()));
            statistics.put("marketingInstanceOverview", marketingOverview);

            Map<String, Object> prospectingOverview = new LinkedHashMap<>();
            prospectingOverview.put("instanceCount", instanceStats.getOrDefault("prospectingInstanceCount", 0L));
            prospectingOverview.put("platformBreakdown", instanceStats.getOrDefault("prospectingPlatformBreakdown", Collections.emptyMap()));
            statistics.put("prospectingInstanceOverview", prospectingOverview);

        } catch (Exception e) {
            log.error("Failed to build admin merchant performance statistics: userId={}", userId, e);
            statistics.put("merchantOverview", buildEmptyMerchantOverview());
            statistics.put("aiCharacterOverview", buildAiCharacterStatistics(Collections.emptySet()));
            statistics.put("marketingInstanceOverview", buildEmptyInstanceOverview());
            statistics.put("prospectingInstanceOverview", buildEmptyInstanceOverview());
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getAdminMerchantsAssetStatistics(Long userId) {
        Map<String, Object> statistics = new LinkedHashMap<>();

        try {
            if (userId == null || userId <= 0) {
                return statistics;
            }

            Set<Long> managedUserIds = collectManagedUserIds(userId);
            if (managedUserIds.isEmpty()) {
                statistics.put("totalRecharge", formatAmount(BigDecimal.ZERO));
                statistics.put("settledCommission", formatAmount(BigDecimal.ZERO));
                statistics.put("netValue", formatAmount(BigDecimal.ZERO));
                return statistics;
            }

            Map<UserIdentity, Set<Long>> membership = resolveIdentityMembership(managedUserIds);
            Set<Long> buyerMainIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_MAIN, Collections.emptySet()));
            Set<Long> agentIds = new LinkedHashSet<>();
            agentIds.addAll(membership.getOrDefault(UserIdentity.AGENT_LEVEL_1, Collections.emptySet()));
            agentIds.addAll(membership.getOrDefault(UserIdentity.AGENT_LEVEL_2, Collections.emptySet()));
            agentIds.addAll(membership.getOrDefault(UserIdentity.AGENT_LEVEL_3, Collections.emptySet()));

            Map<Long, BigDecimal> rechargeMap = fetchRechargeByUserIds(buyerMainIds);
            BigDecimal totalRecharge = formatAmount(sumRechargeForUsers(buyerMainIds, rechargeMap));

            Map<Long, BigDecimal> commissionMap = fetchSettledCommissionByUserIds(agentIds);
            BigDecimal settledCommission = formatAmount(sumCommissionForAgents(agentIds, commissionMap));

            BigDecimal netValue = formatAmount(totalRecharge.subtract(settledCommission));

            statistics.put("totalRecharge", totalRecharge);
            statistics.put("settledCommission", settledCommission);
            statistics.put("netValue", netValue);

        } catch (Exception e) {
            log.error("Failed to build admin merchant asset statistics: userId={}", userId, e);
            statistics.put("totalRecharge", formatAmount(BigDecimal.ZERO));
            statistics.put("settledCommission", formatAmount(BigDecimal.ZERO));
            statistics.put("netValue", formatAmount(BigDecimal.ZERO));
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getGeneralAgentContributionStatistics(Long generalAgentId) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        try {
            if (generalAgentId == null || generalAgentId <= 0) {
                throw new IllegalArgumentException("总代ID无效");
            }
            SysUser generalAgent = userMapper.selectUserWithDept(generalAgentId);
            if (generalAgent == null) {
                throw new IllegalArgumentException("总代不存在");
            }

            Set<String> roleKeys = userMapper.selectRoleKeysByUserId(generalAgentId);
            if (roleKeys == null || !UserRoleUtils.hasIdentity(roleKeys, UserIdentity.AGENT_LEVEL_1)) {
                throw new IllegalArgumentException("仅支持总代查询");
            }

            statistics.put("level1Agent", buildGeneralAgentSummary(generalAgent));

            Set<Long> managedUserIds = collectManagedUserIds(generalAgentId);
            if (managedUserIds.size() <= 1) {
                statistics.put("level2Agents", buildAgentTierSummary(UserIdentity.AGENT_LEVEL_2, "一级代理", 0L, BigDecimal.ZERO));
                statistics.put("level3Agents", buildAgentTierSummary(UserIdentity.AGENT_LEVEL_3, "二级代理", 0L, BigDecimal.ZERO));
                statistics.put("merchants", buildMerchantSummary(UserIdentity.BUYER_MAIN, "商家", 0L, BigDecimal.ZERO));
                statistics.put("totals", buildTotalsSummary(BigDecimal.ZERO, BigDecimal.ZERO));
                return statistics;
            }

            Map<UserIdentity, Set<Long>> membership = resolveIdentityMembership(managedUserIds);
            Set<Long> agentLevel1Ids = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.AGENT_LEVEL_2, Collections.emptySet()));
            Set<Long> agentLevel2Ids = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.AGENT_LEVEL_3, Collections.emptySet()));
            Set<Long> buyerMainIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_MAIN, Collections.emptySet()));
            Set<Long> buyerSubIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_SUB, Collections.emptySet()));

            agentLevel1Ids.remove(generalAgentId);
            agentLevel2Ids.remove(generalAgentId);
            buyerMainIds.remove(generalAgentId);
            buyerSubIds.remove(generalAgentId);

            Set<Long> commissionTargets = new LinkedHashSet<>();
            commissionTargets.addAll(agentLevel1Ids);
            commissionTargets.addAll(agentLevel2Ids);
            Map<Long, BigDecimal> commissionMap = fetchCommissionByUserIds(commissionTargets);

            BigDecimal level1Commission = sumCommissionForAgents(agentLevel1Ids, commissionMap);
            BigDecimal level2Commission = sumCommissionForAgents(agentLevel2Ids, commissionMap);

            Set<Long> rechargeTargets = new LinkedHashSet<>(buyerMainIds);
            Map<Long, BigDecimal> rechargeMap = fetchRechargeByUserIds(rechargeTargets);
            BigDecimal merchantRecharge = sumRechargeForUsers(rechargeTargets, rechargeMap);

            statistics.put("level2Agents", buildAgentTierSummary(
                UserIdentity.AGENT_LEVEL_2, "一级代理", (long) agentLevel1Ids.size(), level1Commission));
            statistics.put("level3Agents", buildAgentTierSummary(
                UserIdentity.AGENT_LEVEL_3, "二级代理", (long) agentLevel2Ids.size(), level2Commission));
            statistics.put("merchants", buildMerchantSummary(
                UserIdentity.BUYER_MAIN, "商家", (long) buyerMainIds.size(), merchantRecharge));

            BigDecimal totalCommission = level1Commission.add(level2Commission);
            statistics.put("totals", buildTotalsSummary(totalCommission, merchantRecharge));
        } catch (Exception ex) {
            log.error("Failed to build general agent contribution statistics: generalAgentId={}", generalAgentId, ex);
            statistics.put("error", ex.getMessage());
        }
        return statistics;
    }

    @Override
    public Map<String, Object> getLevel1AgentContributionStatistics(Long level1AgentId) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        try {
            SysUser agent = validateAgentIdentity(level1AgentId, UserIdentity.AGENT_LEVEL_2,
                "一级代理ID无效", "一级代理不存在", "仅支持一级代理查询");
            statistics.put("level2Agent", buildGeneralAgentSummary(agent));

            Set<Long> managedIds = collectManagedUserIds(level1AgentId);
            if (managedIds.size() <= 1) {
                statistics.put("level3Agents", buildAgentTierSummary(UserIdentity.AGENT_LEVEL_3, "二级代理", 0L, BigDecimal.ZERO));
                statistics.put("merchants", buildMerchantSummary(UserIdentity.BUYER_MAIN, "商家", 0L, BigDecimal.ZERO));
                statistics.put("totals", buildTotalsSummary(BigDecimal.ZERO, BigDecimal.ZERO));
                return statistics;
            }

            Map<UserIdentity, Set<Long>> membership = resolveIdentityMembership(managedIds);
            Set<Long> level2Agents = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.AGENT_LEVEL_3, Collections.emptySet()));
            Set<Long> merchants = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_MAIN, Collections.emptySet()));

            level2Agents.remove(level1AgentId);
            merchants.remove(level1AgentId);

            Map<Long, BigDecimal> commissionMap = fetchCommissionByUserIds(level2Agents);
            BigDecimal level2Commission = sumCommissionForAgents(level2Agents, commissionMap);

            Map<Long, BigDecimal> rechargeMap = fetchRechargeByUserIds(merchants);
            BigDecimal merchantRecharge = sumRechargeForUsers(merchants, rechargeMap);

            statistics.put("level3Agents", buildAgentTierSummary(
                UserIdentity.AGENT_LEVEL_3, "二级代理", (long) level2Agents.size(), level2Commission));
            statistics.put("merchants", buildMerchantSummary(
                UserIdentity.BUYER_MAIN, "商家", (long) merchants.size(), merchantRecharge));
            statistics.put("totals", buildTotalsSummary(level2Commission, merchantRecharge));
        } catch (Exception ex) {
            log.error("Failed to build level1 agent contribution statistics: level1AgentId={}", level1AgentId, ex);
            statistics.put("error", ex.getMessage());
        }
        return statistics;
    }

    @Override
    public Map<String, Object> getLevel2AgentContributionStatistics(Long level2AgentId) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        try {
            SysUser agent = validateAgentIdentity(level2AgentId, UserIdentity.AGENT_LEVEL_3,
                "二级代理ID无效", "二级代理不存在", "仅支持二级代理查询");
            statistics.put("level3Agent", buildGeneralAgentSummary(agent));

            Set<Long> managedIds = collectManagedUserIds(level2AgentId);
            if (managedIds.size() <= 1) {
                statistics.put("merchants", buildMerchantSummary(UserIdentity.BUYER_MAIN, "商家", 0L, BigDecimal.ZERO));
                statistics.put("totals", buildTotalsSummary(BigDecimal.ZERO, BigDecimal.ZERO));
                return statistics;
            }

            Map<UserIdentity, Set<Long>> membership = resolveIdentityMembership(managedIds);
            Set<Long> merchants = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_MAIN, Collections.emptySet()));
            merchants.remove(level2AgentId);

            Map<Long, BigDecimal> rechargeMap = fetchRechargeByUserIds(merchants);
            BigDecimal merchantRecharge = sumRechargeForUsers(merchants, rechargeMap);

            statistics.put("merchants", buildMerchantSummary(
                UserIdentity.BUYER_MAIN, "商家", (long) merchants.size(), merchantRecharge));
            statistics.put("totals", buildTotalsSummary(BigDecimal.ZERO, merchantRecharge));
        } catch (Exception ex) {
            log.error("Failed to build level2 agent contribution statistics: level2AgentId={}", level2AgentId, ex);
            statistics.put("error", ex.getMessage());
        }
        return statistics;
    }

    @Override
    public Map<String, Object> getAgentChildrenStatistics(Long agentUserId) {
        Map<String, Object> statistics = new LinkedHashMap<>();

        try {
            if (agentUserId == null || agentUserId <= 0) {
                return statistics;
            }

            SysUser agent = userMapper.selectUserWithDept(agentUserId);
            if (agent == null) {
                throw new RuntimeException("用户不存在");
            }

            Set<String> roleKeys = userMapper.selectRoleKeysByUserId(agentUserId);
            if (roleKeys == null || roleKeys.isEmpty()) {
                throw new RuntimeException("用户未配置身份");
            }

            UserIdentity identity = resolvePrimaryIdentity(roleKeys);
            if (identity == null || !UserRoleUtils.hasAnyIdentity(roleKeys,
                UserIdentity.AGENT_LEVEL_1, UserIdentity.AGENT_LEVEL_2, UserIdentity.AGENT_LEVEL_3)) {
                throw new RuntimeException("仅代理用户支持该统计");
            }

            List<UserIdentity> children = DIRECT_CHILDREN_BY_IDENTITY.getOrDefault(identity, Collections.emptyList());
            Map<String, Long> childCounts = countDirectChildrenByRole(agentUserId);

            statistics.put("agentId", agentUserId);
            statistics.put("agentUsername", agent.getUsername());
            statistics.put("identity", identity.getRoleKey());

            for (UserIdentity childIdentity : children) {
                String key;
                switch (childIdentity) {
                    case AGENT_LEVEL_2:
                        key = "agentLevel2Count";
                        break;
                    case AGENT_LEVEL_3:
                        key = "agentLevel3Count";
                        break;
                    case BUYER_MAIN:
                        key = "merchantCount";
                        break;
                    default:
                        key = childIdentity.getRoleKey() + "Count";
                        break;
                }
                statistics.put(key, childCounts.getOrDefault(childIdentity.getRoleKey(), 0L));
            }

        } catch (Exception e) {
            log.error("Failed to build agent children statistics: agentUserId={}", agentUserId, e);
            statistics.put("error", e.getMessage());
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getBuyerOperationalStatistics(Long buyerUserId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        try {
            if (buyerUserId == null || buyerUserId <= 0) {
                throw new IllegalArgumentException("买家用户ID无效");
            }

            SysUser buyer = userMapper.selectUserWithDept(buyerUserId);
            if (buyer == null) {
                throw new RuntimeException("买家不存在");
            }

            Set<String> buyerRoles = userMapper.selectRoleKeysByUserId(buyerUserId);
            if (buyerRoles == null || buyerRoles.isEmpty()
                || !UserRoleUtils.hasIdentity(buyerRoles, UserIdentity.BUYER_MAIN)) {
                throw new RuntimeException("仅支持买家主账户查询");
            }

            BigDecimal drBalance = formatAmount(userMapper.selectDrBalanceByUserId(buyerUserId));

            Set<Long> managedUserIds = collectManagedUserIds(buyerUserId);
            if (managedUserIds.size() <= 1) {
                List<SysUser> directSubs = userMapper.selectSubAccountsByParentUserId(buyerUserId);
                if (directSubs != null) {
                    directSubs.stream()
                        .map(SysUser::getUserId)
                        .filter(Objects::nonNull)
                        .forEach(managedUserIds::add);
                }
            }

            Map<UserIdentity, Set<Long>> membership = resolveIdentityMembership(managedUserIds);
            Set<Long> staffIds = new LinkedHashSet<>(membership.getOrDefault(UserIdentity.BUYER_SUB, Collections.emptySet()));
            long staffCount = staffIds.size();

            Set<Long> characterOwners = new LinkedHashSet<>(staffIds);
            characterOwners.add(buyerUserId);
            Map<String, Object> aiStats = buildAiCharacterStatistics(characterOwners);
            Map<String, Long> aiCharacterCounts = new LinkedHashMap<>();
            aiCharacterCounts.put("customerService", safeLongValue(aiStats.get("customerServiceAiCount")));
            aiCharacterCounts.put("design", safeLongValue(aiStats.get("socialAiCount")));
            aiCharacterCounts.put("total", safeLongValue(aiStats.get("totalCharacters")));

            Set<Long> instanceOwners = new LinkedHashSet<>(staffIds);
            instanceOwners.add(buyerUserId);
            Map<String, Object> instanceStats = buildInstanceStatistics(instanceOwners);
            Map<String, Long> marketingBreakdown = castToLongMap(instanceStats.get("marketingPlatformBreakdown"));
            Map<String, Long> prospectingBreakdown = castToLongMap(instanceStats.get("prospectingPlatformBreakdown"));

            Map<String, Long> marketingPlatforms = projectPlatformCounts(marketingBreakdown, MARKETING_DISPLAY_PLATFORMS);
            Map<String, Long> prospectingPlatforms = projectPlatformCounts(prospectingBreakdown, PROSPECTING_DISPLAY_PLATFORMS);

            Map<String, Object> marketingOverview = new LinkedHashMap<>();
            marketingOverview.put("total", marketingPlatforms.values().stream().mapToLong(Long::longValue).sum());
            marketingOverview.put("platforms", marketingPlatforms);

            Map<String, Object> prospectingOverview = new LinkedHashMap<>();
            prospectingOverview.put("total", prospectingPlatforms.values().stream().mapToLong(Long::longValue).sum());
            prospectingOverview.put("platforms", prospectingPlatforms);

            long marketingCount = safeLongValue(marketingOverview.get("total"));
            long prospectingCount = safeLongValue(prospectingOverview.get("total"));
            BigDecimal expectedDailyDeduct = computeExpectedInstanceDailyDeduct(marketingCount, prospectingCount);

            snapshot.put("buyerId", buyerUserId);
            snapshot.put("buyerUsername", Objects.toString(buyer.getUsername(), ""));
            snapshot.put("buyerNickname", Objects.toString(buyer.getNickname(), ""));
            snapshot.put("drBalance", drBalance);
            snapshot.put("staffCount", staffCount);
            snapshot.put("aiCharacterCounts", aiCharacterCounts);
            snapshot.put("marketingInstances", marketingOverview);
            snapshot.put("prospectingInstances", prospectingOverview);
            snapshot.put("expectedInstanceDailyDeductAmount", expectedDailyDeduct);
        } catch (Exception e) {
            log.error("Failed to build buyer operational statistics: buyerUserId={}", buyerUserId, e);
            snapshot.put("error", e.getMessage());
        }
        return snapshot;
    }

    @Override
    public Map<String, Object> getAgentCommissionOverview(Long agentUserId) {
        Map<String, Object> overview = new LinkedHashMap<>();
        try {
            if (agentUserId == null || agentUserId <= 0) {
                throw new IllegalArgumentException("代理用户ID无效");
            }
            SysUser agent = userMapper.selectUserWithDept(agentUserId);
            if (agent == null) {
                throw new IllegalArgumentException("代理用户不存在");
            }

            Set<String> roles = userMapper.selectRoleKeysByUserId(agentUserId);
            if (roles == null || !UserRoleUtils.hasAnyIdentity(roles,
                UserIdentity.AGENT_LEVEL_1, UserIdentity.AGENT_LEVEL_2, UserIdentity.AGENT_LEVEL_3)) {
                throw new IllegalArgumentException("仅代理身份用户支持查询");
            }

            Set<Long> targetIds = Collections.singleton(agentUserId);
            Map<Long, BigDecimal> commissionMap = fetchCommissionByUserIds(targetIds);
            Map<Long, BigDecimal> settledMap = fetchSettledCommissionByUserIds(targetIds);
            Map<Long, BigDecimal> availableMap = fetchAvailableCommissionByUserIds(targetIds);

            BigDecimal totalCommission = commissionMap.getOrDefault(agentUserId, BigDecimal.ZERO);
            BigDecimal settledCommission = settledMap.getOrDefault(agentUserId, BigDecimal.ZERO);
            BigDecimal availableCommission = availableMap.getOrDefault(agentUserId,
                totalCommission.subtract(settledCommission));

            overview.put("agentUserId", agentUserId);
            overview.put("username", Objects.toString(agent.getUsername(), ""));
            overview.put("nickname", Objects.toString(agent.getNickname(), ""));
            overview.put("totalCommission", formatAmount(totalCommission));
            overview.put("settledCommission", formatAmount(settledCommission));
            overview.put("availableCommission", formatAmount(availableCommission));
        } catch (Exception e) {
            log.error("Failed to build agent commission overview: agentUserId={}", agentUserId, e);
            overview.put("error", e.getMessage());
        }
        return overview;
    }

    private BigDecimal computeExpectedInstanceDailyDeduct(long marketingCount, long prospectingCount) {
        BigDecimal marketingPrice = resolveActivePrice(DrPriceConfig.BUSINESS_TYPE_INSTANCE_MARKETING);
        BigDecimal prospectingPrice = resolveActivePrice(DrPriceConfig.BUSINESS_TYPE_INSTANCE_PROSPECTING);
        BigDecimal marketingAmount = marketingPrice.multiply(BigDecimal.valueOf(marketingCount));
        BigDecimal prospectingAmount = prospectingPrice.multiply(BigDecimal.valueOf(prospectingCount));
        return marketingAmount.add(prospectingAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveActivePrice(String businessType) {
        if (drPriceConfigService == null) {
            return BigDecimal.ZERO;
        }
        DrPriceConfig config = drPriceConfigService.selectDrPriceConfigByBusinessType(businessType);
        if (config == null || !config.isActive() || config.getDrPrice() == null) {
            return BigDecimal.ZERO;
        }
        return config.getDrPrice();
    }

    private Set<Long> collectManagedUserIds(Long rootUserId) {
        if (rootUserId == null || rootUserId <= 0) {
            return Collections.emptySet();
        }
        LinkedHashSet<Long> managedUserIds = new LinkedHashSet<>();
        managedUserIds.add(rootUserId);
        if (hierarchyService != null) {
            managedUserIds.addAll(hierarchyService.findDescendantIds(rootUserId));
        }
        return managedUserIds;
    }

    private Map<UserIdentity, Set<Long>> resolveIdentityMembership(Set<Long> userIds) {
        Map<UserIdentity, Set<Long>> membership = new EnumMap<>(UserIdentity.class);
        if (userIds == null || userIds.isEmpty()) {
            return membership;
        }
        List<Map<String, Object>> rows = userMapper.selectUserRoleMappings(userIds);
        if (rows == null) {
            return membership;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Long userId = parseLong(row.get("userId"));
            Object roleKeyObj = row.get("roleKey");
            if (userId == null || !(roleKeyObj instanceof String)) {
                continue;
            }
            String roleKey = ((String) roleKeyObj).trim();
            if (roleKey.isEmpty()) {
                continue;
            }
            UserIdentity.fromRoleKey(roleKey)
                .ifPresent(identity ->
                    membership.computeIfAbsent(identity, key -> new LinkedHashSet<>()).add(userId)
                );
        }
        return membership;
    }

    private Map<UserIdentity, Long> aggregateIdentityCounts(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = userMapper.countUsersByRoleKeys(userIds);
        Map<UserIdentity, Long> identityCounts = new EnumMap<>(UserIdentity.class);
        if (rows == null) {
            return identityCounts;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Object roleKeyObj = row.get("roleKey");
            Object countObj = row.get("userCount");
            if (!(roleKeyObj instanceof String) || !(countObj instanceof Number)) {
                continue;
            }
            String roleKey = (String) roleKeyObj;
            Number countNumber = (Number) countObj;
            UserIdentity.fromRoleKey(roleKey)
                .ifPresent(identity ->
                    identityCounts.merge(identity, countNumber.longValue(), Long::sum)
                );
        }
        return identityCounts;
    }

    private Map<String, Long> buildIdentityBreakdown(Map<UserIdentity, Long> counts, long unknownCount) {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        breakdown.put(UserIdentity.ADMIN.getRoleKey(), counts.getOrDefault(UserIdentity.ADMIN, 0L));
        breakdown.put(UserIdentity.AGENT_LEVEL_1.getRoleKey(), counts.getOrDefault(UserIdentity.AGENT_LEVEL_1, 0L));
        breakdown.put(UserIdentity.AGENT_LEVEL_2.getRoleKey(), counts.getOrDefault(UserIdentity.AGENT_LEVEL_2, 0L));
        breakdown.put(UserIdentity.AGENT_LEVEL_3.getRoleKey(), counts.getOrDefault(UserIdentity.AGENT_LEVEL_3, 0L));
        breakdown.put(UserIdentity.BUYER_MAIN.getRoleKey(), counts.getOrDefault(UserIdentity.BUYER_MAIN, 0L));
        breakdown.put(UserIdentity.BUYER_SUB.getRoleKey(), counts.getOrDefault(UserIdentity.BUYER_SUB, 0L));
        if (unknownCount > 0) {
            breakdown.put("unassigned", unknownCount);
        }
        return breakdown;
    }

    private Map<String, Object> buildAgentRechargeStatistics(Set<Long> level1Agents,
                                                             Set<Long> level2Agents,
                                                             Set<Long> level3Agents,
                                                             Set<Long> buyerMainIds,
                                                             Set<Long> buyerSubIds,
                                                             Map<Long, BigDecimal> rechargeMap) {
        Map<String, Object> rechargeStats = new LinkedHashMap<>();
        BigDecimal level1Recharge = sumRechargeForAgents(level1Agents, buyerMainIds, buyerSubIds, rechargeMap);
        BigDecimal level2Recharge = sumRechargeForAgents(level2Agents, buyerMainIds, buyerSubIds, rechargeMap);
        BigDecimal level3Recharge = sumRechargeForAgents(level3Agents, buyerMainIds, buyerSubIds, rechargeMap);
        BigDecimal totalRecharge = level1Recharge.add(level2Recharge).add(level3Recharge);

        rechargeStats.put("level1Recharge", formatAmount(level1Recharge));
        rechargeStats.put("level2Recharge", formatAmount(level2Recharge));
        rechargeStats.put("level3Recharge", formatAmount(level3Recharge));
        rechargeStats.put("level1commission", formatAmount(level1Recharge.multiply(LEVEL1_COMMISSION_RATE)));
        rechargeStats.put("level2commission", formatAmount(level2Recharge.multiply(LEVEL2_COMMISSION_RATE)));
        rechargeStats.put("level3commission", formatAmount(level3Recharge.multiply(LEVEL3_COMMISSION_RATE)));
        rechargeStats.put("totalRecharge", formatAmount(totalRecharge));
        return rechargeStats;
    }

    private Map<String, Object> buildAgentCommissionStatistics(Set<Long> level1Agents,
                                                               Set<Long> level2Agents,
                                                               Set<Long> level3Agents,
                                                               Map<Long, BigDecimal> commissionMap) {
        Map<String, Object> commissionStats = new LinkedHashMap<>();
        BigDecimal level1 = sumCommissionForAgents(level1Agents, commissionMap);
        BigDecimal level2 = sumCommissionForAgents(level2Agents, commissionMap);
        BigDecimal level3 = sumCommissionForAgents(level3Agents, commissionMap);
        BigDecimal total = level1.add(level2).add(level3);

        commissionStats.put("level1Commission", formatAmount(level1));
        commissionStats.put("level2Commission", formatAmount(level2));
        commissionStats.put("level3Commission", formatAmount(level3));
        commissionStats.put("totalCommission", formatAmount(total));
        return commissionStats;
    }

    private Map<String, Object> buildAgentPerformance(String identityKey,
                                                      String displayLabel,
                                                      Set<Long> agentIds,
                                                      Set<Long> buyerMainIds,
                                                      Set<Long> buyerSubIds,
                                                      Map<Long, BigDecimal> rechargeMap,
                                                      Map<Long, BigDecimal> commissionMap) {
        Map<String, Object> performance = new LinkedHashMap<>();
        BigDecimal recharge = sumRechargeForAgents(agentIds, buyerMainIds, buyerSubIds, rechargeMap);
        BigDecimal commission = sumCommissionForAgents(agentIds, commissionMap);

        performance.put("identity", identityKey);
        performance.put("identityDisplay", displayLabel);
        performance.put("agentCount", (long) (agentIds != null ? agentIds.size() : 0));
        performance.put("totalRecharge", formatAmount(recharge));
        performance.put("totalCommission", formatAmount(commission));
        return performance;
    }

    private Map<String, Object> buildEmptyPerformance(String identityKey, String displayLabel) {
        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("identity", identityKey);
        performance.put("identityDisplay", displayLabel);
        performance.put("agentCount", 0L);
        performance.put("totalRecharge", formatAmount(BigDecimal.ZERO));
        performance.put("totalCommission", formatAmount(BigDecimal.ZERO));
        return performance;
    }

    private Map<String, Object> buildEmptyMerchantOverview() {
        Map<String, Object> merchantOverview = new LinkedHashMap<>();
        merchantOverview.put("merchantCount", 0L);
        merchantOverview.put("employeeCount", 0L);
        merchantOverview.put("totalPerformance", formatAmount(BigDecimal.ZERO));
        return merchantOverview;
    }

    private Map<String, Object> buildEmptyInstanceOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("instanceCount", 0L);
        overview.put("platformBreakdown", Collections.emptyMap());
        return overview;
    }

    private Map<String, Long> countDirectChildrenByRole(Long parentUserId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        if (parentUserId == null || parentUserId <= 0) {
            return counts;
        }
        List<Map<String, Object>> rows = userMapper.countChildrenByRoleKey(parentUserId);
        if (rows == null) {
            return counts;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Object roleKey = row.get("roleKey");
            Object countObj = row.get("userCount");
            if (!(roleKey instanceof String) || !(countObj instanceof Number)) {
                continue;
            }
            counts.put((String) roleKey, ((Number) countObj).longValue());
        }
        return counts;
    }

    private UserIdentity resolvePrimaryIdentity(Set<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return null;
        }
        if (UserRoleUtils.hasIdentity(roleKeys, UserIdentity.AGENT_LEVEL_1)) {
            return UserIdentity.AGENT_LEVEL_1;
        }
        if (UserRoleUtils.hasIdentity(roleKeys, UserIdentity.AGENT_LEVEL_2)) {
            return UserIdentity.AGENT_LEVEL_2;
        }
        if (UserRoleUtils.hasIdentity(roleKeys, UserIdentity.AGENT_LEVEL_3)) {
            return UserIdentity.AGENT_LEVEL_3;
        }
        if (UserRoleUtils.hasIdentity(roleKeys, UserIdentity.BUYER_MAIN)) {
            return UserIdentity.BUYER_MAIN;
        }
        if (UserRoleUtils.hasIdentity(roleKeys, UserIdentity.BUYER_SUB)) {
            return UserIdentity.BUYER_SUB;
        }
        if (UserRoleUtils.hasIdentity(roleKeys, UserIdentity.ADMIN)) {
            return UserIdentity.ADMIN;
        }
        return null;
    }

    private Map<String, Object> buildMerchantPerformance(Set<Long> buyerMainIds,
                                                         Set<Long> buyerSubIds,
                                                         Map<Long, BigDecimal> rechargeMap,
                                                         Set<Long> buyerUserIds) {
        Map<String, Object> merchantStats = new LinkedHashMap<>();
        merchantStats.put("totalMerchants", (long) buyerMainIds.size());
        merchantStats.put("buyerSubAccountCount", (long) buyerSubIds.size());
        merchantStats.put("totalRecharge", formatAmount(sumRechargeForUsers(buyerUserIds, rechargeMap)));
        merchantStats.put("instanceStatistics", buildInstanceStatistics(buyerUserIds));
        merchantStats.put("aiCharacterStatistics", buildAiCharacterStatistics(buyerUserIds));
        merchantStats.put("buyerMainDetails", buildBuyerMainDetails(buyerMainIds, buyerSubIds, rechargeMap));
        return merchantStats;
    }

    private Map<Long, BigDecimal> fetchRechargeByUserIds(Set<Long> userIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<Map<String, Object>> rows = userMapper.sumTotalRechargeByUserIds(userIds);
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Long userId = parseLong(row.get("userId"));
            if (userId == null) {
                continue;
            }
            result.put(userId, formatAmount(toBigDecimal(row.get("totalRecharge"))));
        }
        return result;
    }

    private Map<Long, BigDecimal> fetchCommissionByUserIds(Set<Long> userIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<Map<String, Object>> rows = agentCommissionAccountMapper.selectCommissionByUserIds(userIds);
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Long userId = parseLong(row.get("userId"));
            if (userId == null) {
                continue;
            }
            result.put(userId, formatAmount(toBigDecimal(row.get("totalCommission"))));
        }
        return result;
    }

    private Map<Long, BigDecimal> fetchSettledCommissionByUserIds(Set<Long> userIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<Map<String, Object>> rows = agentCommissionAccountMapper.selectCommissionByUserIds(userIds);
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Long userId = parseLong(row.get("userId"));
            if (userId == null) {
                continue;
            }
            result.put(userId, formatAmount(toBigDecimal(row.get("settledCommission"))));
        }
        return result;
    }

    private Map<Long, BigDecimal> fetchAvailableCommissionByUserIds(Set<Long> userIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<Map<String, Object>> rows = agentCommissionAccountMapper.selectCommissionByUserIds(userIds);
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Long userId = parseLong(row.get("userId"));
            if (userId == null) {
                continue;
            }
            result.put(userId, formatAmount(toBigDecimal(row.get("availableCommission"))));
        }
        return result;
    }

    private BigDecimal sumRechargeForAgents(Set<Long> agentIds,
                                            Set<Long> buyerMainIds,
                                            Set<Long> buyerSubIds,
                                            Map<Long, BigDecimal> rechargeMap) {
        if (agentIds == null || agentIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (hierarchyService == null) {
            return BigDecimal.ZERO;
        }
        Set<Long> buyers = new LinkedHashSet<>();
        for (Long agentId : agentIds) {
            Set<Long> descendants = hierarchyService.findDescendantIds(agentId);
            for (Long descendant : descendants) {
                if (buyerMainIds.contains(descendant) || buyerSubIds.contains(descendant)) {
                    buyers.add(descendant);
                }
            }
        }
        return sumRechargeForUsers(buyers, rechargeMap);
    }

    private BigDecimal sumRechargeForUsers(Set<Long> userIds, Map<Long, BigDecimal> rechargeMap) {
        if (userIds == null || userIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Long userId : userIds) {
            total = total.add(rechargeMap.getOrDefault(userId, BigDecimal.ZERO));
        }
        return total;
    }

    private BigDecimal sumCommissionForAgents(Set<Long> agentIds, Map<Long, BigDecimal> commissionMap) {
        if (agentIds == null || agentIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Long agentId : agentIds) {
            total = total.add(commissionMap.getOrDefault(agentId, BigDecimal.ZERO));
        }
        return total;
    }

    private List<Map<String, Object>> buildBuyerMainDetails(Set<Long> buyerMainIds,
                                                            Set<Long> buyerSubIds,
                                                            Map<Long, BigDecimal> rechargeMap) {
        if (buyerMainIds == null || buyerMainIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUser> buyerMainUsers = userMapper.selectUsersByIds(buyerMainIds);
        Map<Long, SysUser> userIndex = buyerMainUsers == null ? Collections.emptyMap()
            : buyerMainUsers.stream()
                .filter(Objects::nonNull)
                .filter(user -> user.getUserId() != null)
                .collect(Collectors.toMap(SysUser::getUserId, user -> user, (left, right) -> left));

        List<Map<String, Object>> details = new ArrayList<>();
        for (Long mainId : buyerMainIds) {
            SysUser user = userIndex.get(mainId);
            long subCount = hierarchyService != null
                ? hierarchyService.findDescendantIds(mainId).stream()
                    .filter(buyerSubIds::contains)
                    .count()
                : 0L;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", mainId);
            row.put("username", user != null ? Objects.toString(user.getUsername(), "") : "");
            row.put("nickname", user != null ? Objects.toString(user.getNickname(), "") : "");
            row.put("subAccountCount", subCount);
            row.put("totalRecharge", formatAmount(rechargeMap.getOrDefault(mainId, BigDecimal.ZERO)));
            details.add(row);
        }
        details.sort(Comparator.comparingLong(
            (Map<String, Object> value) -> {
                Object count = value.get("subAccountCount");
                if (count instanceof Number) {
                    return ((Number) count).longValue();
                }
                return 0L;
            }).reversed());
        return details;
    }

    private Map<String, Long> initializePlatformMap(List<Map<String, Object>> platformRows, Set<String> filterTypes) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (platformRows != null) {
            for (Map<String, Object> row : platformRows) {
                if (row == null) {
                    continue;
                }
                String rawType = Objects.toString(row.get("platformType"), "");
                String normalizedType = normalizePlatformType(rawType);
                if (!filterTypes.contains(normalizedType)) {
                    continue;
                }
                String name = normalizePlatformName(Objects.toString(row.get("platformName"), "UNKNOWN_PLATFORM"));
                result.putIfAbsent(name, 0L);
            }
        }
        return result;
    }

    private String normalizePlatformName(String name) {
        if (name == null) {
            return "UNKNOWN_PLATFORM";
        }
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            return "UNKNOWN_PLATFORM";
        }
        switch (normalized.toLowerCase(Locale.ROOT)) {
            case "wechat":
            case "weixin":
                return "WeChat";
            case "qq":
                return "QQ";
            case "sms":
                return "SMS";
            case "whatsapp":
                return "WhatsApp";
            case "facebook":
                return "Facebook";
            case "telegram":
                return "Telegram";
            case "instagram":
                return "Instagram";
            case "tiktok":
                return "TikTok";
            case "x":
            case "twitter":
                return "X";
            case "googlevoice":
            case "google_voice":
            case "google voice":
                return "GoogleVoice";
            default:
                return normalized;
        }
    }

    private String normalizePlatformType(String rawType) {
        if (rawType == null) {
            return "unknown";
        }
        String type = rawType.trim().toLowerCase(Locale.ROOT);
        if (type.isEmpty()) {
            return "unknown";
        }
        return type.replace('_', '-');
    }

    private Map<String, Object> buildInstanceStatistics(Set<Long> userIds) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("marketingInstanceCount", 0L);
        statistics.put("prospectingInstanceCount", 0L);
        statistics.put("marketingPlatformBreakdown", Collections.emptyMap());
        statistics.put("prospectingPlatformBreakdown", Collections.emptyMap());

        List<Map<String, Object>> platformRows = userMapper.selectAllPlatforms();
        Map<String, Long> marketingBreakdown = initializePlatformMap(platformRows, MARKETING_PLATFORM_TYPES);
        Map<String, Long> prospectingBreakdown = initializePlatformMap(platformRows, PROSPECTING_PLATFORM_TYPES);

        if (userIds == null || userIds.isEmpty()) {
            statistics.put("marketingPlatformBreakdown", marketingBreakdown);
            statistics.put("prospectingPlatformBreakdown", prospectingBreakdown);
            return statistics;
        }

        List<Map<String, Object>> typeRows = userMapper.countInstancesByType(userIds);
        long marketingCount = 0L;
        long prospectingCount = 0L;

        if (typeRows != null) {
            for (Map<String, Object> row : typeRows) {
                String instanceType = Objects.toString(row.get("instanceType"), "");
                Long countValue = parseLong(row.get("count"));
                long count = countValue != null ? countValue : 0L;
                switch (instanceType) {
                    case "0":
                        marketingCount = count;
                        break;
                    case "1":
                        prospectingCount = count;
                        break;
                    default:
                        break;
                }
            }
        }

        statistics.put("marketingInstanceCount", marketingCount);
        statistics.put("prospectingInstanceCount", prospectingCount);

        List<Map<String, Object>> marketingRows = userMapper.countInstancesByPlatform(userIds, "0");
        if (marketingRows != null) {
            for (Map<String, Object> row : marketingRows) {
                String platform = normalizePlatformName(Objects.toString(row.get("platformName"), "UNKNOWN_PLATFORM"));
                Long countValue = parseLong(row.get("count"));
                long count = countValue != null ? countValue : 0L;
                long current = marketingBreakdown.getOrDefault(platform, 0L);
                marketingBreakdown.put(platform, current + count);
            }
        }

        List<Map<String, Object>> prospectingRows = userMapper.countInstancesByPlatform(userIds, "1");
        if (prospectingRows != null) {
            for (Map<String, Object> row : prospectingRows) {
                String platform = normalizePlatformName(Objects.toString(row.get("platformName"), "UNKNOWN_PLATFORM"));
                Long countValue = parseLong(row.get("count"));
                long count = countValue != null ? countValue : 0L;
                long current = prospectingBreakdown.getOrDefault(platform, 0L);
                prospectingBreakdown.put(platform, current + count);
            }
        }

        statistics.put("marketingPlatformBreakdown", marketingBreakdown);
        statistics.put("prospectingPlatformBreakdown", prospectingBreakdown);

        return statistics;
    }

    private Map<String, Object> buildAiCharacterStatistics(Set<Long> userIds) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("totalCharacters", 0L);
        statistics.put("socialAiCount", 0L);
        statistics.put("customerServiceAiCount", 0L);

        if (userIds == null || userIds.isEmpty()) {
            return statistics;
        }

        List<Map<String, Object>> rows = userMapper.countAiCharactersByType(userIds);
        long total = 0L;
        long social = 0L;
        long customerService = 0L;

        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String type = Objects.toString(row.get("type"), "");
                Long countValue = parseLong(row.get("count"));
                long count = countValue != null ? countValue : 0L;
                total += count;
                if ("emotion".equalsIgnoreCase(type) || "social".equalsIgnoreCase(type)) {
                    social += count;
                } else if ("business".equalsIgnoreCase(type)
                    || "service".equalsIgnoreCase(type)
                    || "customer_service".equalsIgnoreCase(type)
                    || "customer".equalsIgnoreCase(type)) {
                    customerService += count;
                }
            }
        }

        statistics.put("totalCharacters", total);
        statistics.put("socialAiCount", social);
        statistics.put("customerServiceAiCount", customerService);

        return statistics;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number || value instanceof String) {
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException ex) {
                log.warn("Unable to parse amount: {}", value, ex);
            }
        }
        return BigDecimal.ZERO;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ex) {
                log.warn("Unable to parse long value: {}", value, ex);
            }
        }
        return null;
    }

    private long safeLongValue(Object value) {
        Long parsed = parseLong(value);
        return parsed != null ? parsed : 0L;
    }

    private BigDecimal formatAmount(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private Map<String, Long> castToLongMap(Object value) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry == null || entry.getKey() == null) {
                    continue;
                }
                String key = Objects.toString(entry.getKey(), "UNKNOWN_PLATFORM");
                result.put(key, safeLongValue(entry.getValue()));
            }
        }
        return result;
    }

    private Map<String, Long> projectPlatformCounts(Map<String, Long> source, List<String> displayKeys) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (displayKeys == null) {
            return result;
        }
        for (String key : displayKeys) {
            result.put(key, source != null ? source.getOrDefault(key, 0L) : 0L);
        }
        return result;
    }

    private void initializeDeptStatistics(Map<String, Object> statistics,
                                          Long systemDept, Long level1Agent, Long level2Agent,
                                          Long level3Agent, Long buyerMain, Long buyerSub) {
        statistics.put("systemDeptCount", systemDept);
        statistics.put("level1AgentCount", level1Agent);
        statistics.put("level2AgentCount", level2Agent);
        statistics.put("level3AgentCount", level3Agent);
        statistics.put("buyerMainAccountCount", buyerMain);
        statistics.put("buyerSubAccountCount", buyerSub);
    }

    private void initializeAgentStatistics(Map<String, Object> statistics,
                                           Long level1, Long level2, Long level3) {
        statistics.put("level1AgentCount", level1);
        statistics.put("level2AgentCount", level2);
        statistics.put("level3AgentCount", level3);
    }

    private void initializeBuyerStatistics(Map<String, Object> statistics,
                                           Long buyerMain, Long buyerSub) {
        statistics.put("buyerMainAccountCount", buyerMain);
        statistics.put("buyerSubAccountCount", buyerSub);
    }

    private Map<String, Object> buildLevelSummary(Long count, BigDecimal totalCommission) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("count", count != null ? count : 0L);
        summary.put("totalCommission", formatAmount(totalCommission));
        return summary;
    }

    private Map<String, Object> buildBuyerSummary(Long count, BigDecimal totalRecharge) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("count", count != null ? count : 0L);
        summary.put("totalRecharge", formatAmount(totalRecharge));
        return summary;
    }

    private Map<String, Object> buildGeneralAgentSummary(SysUser generalAgent) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("userId", generalAgent.getUserId());
        summary.put("username", Objects.toString(generalAgent.getUsername(), ""));
        summary.put("nickname", Objects.toString(generalAgent.getNickname(), ""));
        summary.put("realName", Objects.toString(generalAgent.getRealName(), ""));
        summary.put("phone", Objects.toString(generalAgent.getPhone(), ""));
        summary.put("email", Objects.toString(generalAgent.getEmail(), ""));
        return summary;
    }

    private Map<String, Object> buildAgentTierSummary(UserIdentity identity,
                                                      String displayName,
                                                      Long count,
                                                      BigDecimal totalCommission) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("identity", identity != null ? identity.getRoleKey() : "");
        summary.put("displayName", displayName != null ? displayName : "");
        summary.put("agentCount", count != null ? count : 0L);
        summary.put("totalCommission", formatAmount(totalCommission));
        return summary;
    }

    private Map<String, Object> buildMerchantSummary(UserIdentity identity,
                                                     String displayName,
                                                     Long count,
                                                     BigDecimal totalRecharge) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("identity", identity != null ? identity.getRoleKey() : "");
        summary.put("displayName", displayName != null ? displayName : "");
        summary.put("merchantCount", count != null ? count : 0L);
        summary.put("totalRecharge", formatAmount(totalRecharge));
        return summary;
    }

    private Map<String, Object> buildTotalsSummary(BigDecimal totalCommission,
                                                   BigDecimal totalRecharge) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCommission", formatAmount(totalCommission));
        summary.put("totalRecharge", formatAmount(totalRecharge));
        return summary;
    }

    private SysUser validateAgentIdentity(Long userId,
                                          UserIdentity requiredIdentity,
                                          String invalidMessage,
                                          String missingMessage,
                                          String identityMessage) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(invalidMessage);
        }
        SysUser agent = userMapper.selectUserWithDept(userId);
        if (agent == null) {
            throw new IllegalArgumentException(missingMessage);
        }
        Set<String> roles = userMapper.selectRoleKeysByUserId(userId);
        if (roles == null || !UserRoleUtils.hasIdentity(roles, requiredIdentity)) {
            throw new IllegalArgumentException(identityMessage);
        }
        return agent;
    }
}
