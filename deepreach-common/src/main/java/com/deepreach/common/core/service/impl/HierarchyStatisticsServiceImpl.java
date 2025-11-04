package com.deepreach.common.core.service.impl;

import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.mapper.AgentCommissionAccountStatMapper;
import com.deepreach.common.core.mapper.SysUserMapper;
import com.deepreach.common.core.service.HierarchyStatisticsService;
import com.deepreach.common.core.service.UserHierarchyService;
import com.deepreach.common.security.enums.UserIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户树统计实现，替代原部门统计逻辑。
 */
@Slf4j
@Service
public class HierarchyStatisticsServiceImpl implements HierarchyStatisticsService {

    private static final BigDecimal LEVEL1_COMMISSION_RATE = new BigDecimal("0.30");
    private static final BigDecimal LEVEL2_COMMISSION_RATE = new BigDecimal("0.20");
    private static final BigDecimal LEVEL3_COMMISSION_RATE = new BigDecimal("0.10");
    private static final int MONEY_SCALE = 2;
    private static final Set<String> MARKETING_PLATFORM_TYPES =
        Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("marketing")));
    private static final Set<String> PROSPECTING_PLATFORM_TYPES =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList("customer-acquisition", "prospecting")));

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private AgentCommissionAccountStatMapper agentCommissionAccountMapper;

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

    private BigDecimal formatAmount(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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
}
