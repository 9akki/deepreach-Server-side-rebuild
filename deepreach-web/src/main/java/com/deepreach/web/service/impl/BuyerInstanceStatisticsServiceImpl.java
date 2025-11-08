package com.deepreach.web.service.impl;

import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.mapper.SysUserMapper;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.security.UserRoleUtils;
import com.deepreach.common.security.enums.UserIdentity;
import com.deepreach.web.entity.AiInstance;
import com.deepreach.common.core.domain.entity.UserDrBalance;
import com.deepreach.web.entity.dto.AiCharacterStatistics;
import com.deepreach.web.mapper.AiCharacterMapper;
import com.deepreach.web.mapper.AiInstanceMapper;
import com.deepreach.web.mapper.UserDrBalanceMapper;
import com.deepreach.web.service.BuyerInstanceStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 买家实例统计服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerInstanceStatisticsServiceImpl implements BuyerInstanceStatisticsService {

    private final SysUserMapper userMapper;
    private final SysUserService userService;
    private final AiInstanceMapper aiInstanceMapper;
    private final AiCharacterMapper aiCharacterMapper;
    private final UserDrBalanceMapper userDrBalanceMapper;

    @Override
    public Map<String, Object> getBuyerSubInstanceStatistics(Long buyerMainUserId) {
        SysUser buyerMainUser = requireBuyerMainUser(buyerMainUserId);
        BuyerHierarchyData hierarchyData = resolveHierarchyData(buyerMainUser);
        return buildDetailedStatistics(hierarchyData);
    }

    @Override
    public Map<String, Object> getBuyerSubInstanceStatistics(String buyerMainUsername) {
        SysUser buyerMainUser = requireBuyerMainUser(buyerMainUsername);
        BuyerHierarchyData hierarchyData = resolveHierarchyData(buyerMainUser);
        return buildDetailedStatistics(hierarchyData);
    }

    @Override
    public Map<String, Object> getBuyerHierarchyOverview(Long buyerMainUserId) {
        SysUser buyerMainUser = requireBuyerMainUser(buyerMainUserId);
        BuyerHierarchyData hierarchyData = resolveHierarchyData(buyerMainUser);

        Map<String, Object> overview = buildBaseOverview(hierarchyData);
        overview.put("subUserCount", hierarchyData.getSubUsers().size());
        overview.put("instanceCount", hierarchyData.getAllInstances().size());
        overview.put("instanceTypeStatistics", buildInstanceTypeStatistics(hierarchyData.getAllInstances()));
        overview.put("platformStatistics", buildPlatformStatistics(hierarchyData.getAllInstances()));
        overview.put("subUsers", buildSubUserDetails(hierarchyData));
        overview.put("aiCharacterStatistics", buildAiCharacterStatistics(collectCharacterUserIds(hierarchyData)));
        overview.put("drAccount", buildDrAccountSnapshot(buyerMainUser.getUserId()));
        return overview;
    }

    private SysUser requireBuyerMainUser(Long buyerMainUserId) {
        if (buyerMainUserId == null) {
            throw new IllegalArgumentException("商家总账号用户ID不能为空");
        }
        SysUser buyerMainUser = userMapper.selectUserById(buyerMainUserId);
        if (buyerMainUser == null) {
            throw new IllegalArgumentException("商家总账号用户不存在");
        }
        Set<String> roleKeys = userMapper.selectRoleKeysByUserId(buyerMainUserId);
        if (!UserRoleUtils.hasIdentity(roleKeys, UserIdentity.BUYER_MAIN)) {
            throw new IllegalArgumentException("指定用户不是商家总账号用户");
        }
        buyerMainUser.setRoles(roleKeys);
        return buyerMainUser;
    }

    private SysUser requireBuyerMainUser(String buyerMainUsername) {
        if (buyerMainUsername == null || buyerMainUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("商家总账号用户名不能为空");
        }
        SysUser buyerMainUser = userMapper.selectUserByUsername(buyerMainUsername.trim());
        if (buyerMainUser == null) {
            throw new IllegalArgumentException("商家总账号用户不存在");
        }
        Set<String> roleKeys = userMapper.selectRoleKeysByUserId(buyerMainUser.getUserId());
        if (!UserRoleUtils.hasIdentity(roleKeys, UserIdentity.BUYER_MAIN)) {
            throw new IllegalArgumentException("指定用户不是商家总账号用户");
        }
        buyerMainUser.setRoles(roleKeys);
        return buyerMainUser;
    }

    private BuyerHierarchyData resolveHierarchyData(SysUser buyerMainUser) {
        Long buyerMainUserId = buyerMainUser.getUserId();
        if (buyerMainUserId == null) {
            throw new IllegalArgumentException("商家总账号用户ID无效");
        }

        List<SysUser> buyerSubUsers = userService.selectUsersWithinHierarchy(buyerMainUserId, "buyer_sub", null);
        List<SysUser> sortedSubUsers = CollectionUtils.isEmpty(buyerSubUsers)
            ? Collections.emptyList()
            : buyerSubUsers.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SysUser::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        Set<Long> subUserIdSet = sortedSubUsers.stream()
            .map(SysUser::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, Set<String>> subRoleKeys = loadRoleKeys(subUserIdSet);
        sortedSubUsers.forEach(user ->
            user.setRoles(subRoleKeys.getOrDefault(user.getUserId(), Collections.emptySet())));

        List<Long> buyerSubUserIds = sortedSubUsers.stream()
            .map(SysUser::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<AiInstance> allInstances = CollectionUtils.isEmpty(buyerSubUserIds)
            ? Collections.emptyList()
            : aiInstanceMapper.selectByUserIds(buyerSubUserIds);

        Map<Long, List<AiInstance>> instanceMap = CollectionUtils.isEmpty(allInstances)
            ? Collections.emptyMap()
            : allInstances.stream()
                .filter(instance -> instance.getUserId() != null)
                .collect(Collectors.groupingBy(AiInstance::getUserId, HashMap::new, Collectors.toList()));

        return new BuyerHierarchyData(buyerMainUser, sortedSubUsers, allInstances, instanceMap);
    }

    private Map<String, Object> buildDetailedStatistics(BuyerHierarchyData hierarchyData) {
        Map<String, Object> result = buildBaseOverview(hierarchyData);
        result.put("subUserCount", hierarchyData.getSubUsers().size());
        result.put("instanceCount", hierarchyData.getAllInstances().size());
        result.put("instanceTypeStatistics", buildInstanceTypeStatistics(hierarchyData.getAllInstances()));
        result.put("subUsers", buildSubUserDetails(hierarchyData));
        return result;
    }

    private Map<String, Object> buildBaseOverview(BuyerHierarchyData hierarchyData) {
        Map<String, Object> overview = new LinkedHashMap<>();
        SysUser buyerMainUser = hierarchyData.getBuyerMainUser();
        overview.put("buyerMainUserId", buyerMainUser.getUserId());
        overview.put("buyerMainUsername", buyerMainUser.getUsername());
        overview.put("buyerMainNickname", buyerMainUser.getNickname());
        overview.put("buyerMainRoles", buyerMainUser.getRoles());
        return overview;
    }

    private Map<String, Object> buildInstanceTypeStatistics(List<AiInstance> instances) {
        long marketingCount = instances.stream()
            .filter(instance -> "0".equals(instance.getInstanceType()))
            .count();
        long prospectingCount = instances.stream()
            .filter(instance -> "1".equals(instance.getInstanceType()))
            .count();
        long otherCount = instances.size() - marketingCount - prospectingCount;

        Map<String, Object> typeStats = new LinkedHashMap<>();
        typeStats.put("marketing", marketingCount);
        typeStats.put("prospecting", prospectingCount);
        typeStats.put("other", Math.max(otherCount, 0));
        return typeStats;
    }

    private List<Map<String, Object>> buildPlatformStatistics(List<AiInstance> instances) {
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyList();
        }

        Map<Integer, Long> platformCounts = instances.stream()
            .map(AiInstance::getPlatformId)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<Map<String, Object>> statistics = new ArrayList<>();
        platformCounts.entrySet().stream()
            .sorted(Map.Entry.<Integer, Long>comparingByValue(Comparator.reverseOrder()))
            .forEach(entry -> {
                Map<String, Object> item = new LinkedHashMap<>();
                Integer platformId = entry.getKey();
                item.put("platformId", platformId);
                item.put("instanceCount", entry.getValue());
                item.put("platformLabel", platformId != null ? "平台" + platformId : "未绑定平台");
                statistics.add(item);
            });
        return statistics;
    }

    private List<Map<String, Object>> buildSubUserDetails(BuyerHierarchyData hierarchyData) {
        if (CollectionUtils.isEmpty(hierarchyData.getSubUsers())) {
            return Collections.emptyList();
        }

        Map<Long, List<AiInstance>> instanceMap = hierarchyData.getInstanceMap();
        List<Map<String, Object>> subUserDetails = new ArrayList<>();
        for (SysUser subUser : hierarchyData.getSubUsers()) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("userId", subUser.getUserId());
            detail.put("username", subUser.getUsername());
            detail.put("nickname", subUser.getNickname());
            detail.put("realName", subUser.getRealName());
            detail.put("roles", subUser.getRoles());

            List<AiInstance> userInstances = instanceMap.getOrDefault(subUser.getUserId(), Collections.emptyList());
            detail.put("instanceCount", userInstances.size());
            detail.put("instances", userInstances);
            subUserDetails.add(detail);
        }
        return subUserDetails;
    }

    private Map<String, Object> buildAiCharacterStatistics(List<Long> userIds) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCount", 0L);
        stats.put("systemCount", 0L);
        stats.put("userCreatedCount", 0L);
        stats.put("emotionCount", 0L);
        stats.put("businessCount", 0L);

        if (CollectionUtils.isEmpty(userIds)) {
            return stats;
        }

        List<Long> distinctUserIds = userIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(distinctUserIds)) {
            return stats;
        }

        AiCharacterStatistics rawStats = aiCharacterMapper.countStatisticsByUserIds(distinctUserIds);
        if (rawStats != null) {
            stats.put("totalCount", rawStats.safeValue(rawStats.getTotalCount()));
            stats.put("systemCount", rawStats.safeValue(rawStats.getSystemCount()));
            stats.put("userCreatedCount", rawStats.safeValue(rawStats.getUserCreatedCount()));
            stats.put("emotionCount", rawStats.safeValue(rawStats.getEmotionCount()));
            stats.put("businessCount", rawStats.safeValue(rawStats.getBusinessCount()));
        }
        return stats;
    }

    private Map<String, Object> buildDrAccountSnapshot(Long userId) {
        UserDrBalance balance = userDrBalanceMapper.selectByUserId(userId);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("userId", userId);
        snapshot.put("drBalance", balance != null && balance.getDrBalance() != null ? balance.getDrBalance() : BigDecimal.ZERO);
        snapshot.put("preDeductedBalance", balance != null && balance.getPreDeductedBalance() != null ? balance.getPreDeductedBalance() : BigDecimal.ZERO);
        snapshot.put("totalRecharge", balance != null && balance.getTotalRecharge() != null ? balance.getTotalRecharge() : BigDecimal.ZERO);
        snapshot.put("totalConsume", balance != null && balance.getTotalConsume() != null ? balance.getTotalConsume() : BigDecimal.ZERO);
        snapshot.put("totalRefund", balance != null && balance.getTotalRefund() != null ? balance.getTotalRefund() : BigDecimal.ZERO);
        snapshot.put("frozenAmount", balance != null && balance.getFrozenAmount() != null ? balance.getFrozenAmount() : BigDecimal.ZERO);
        snapshot.put("status", balance != null ? balance.getStatus() : "0");
        return snapshot;
    }

    private List<Long> collectCharacterUserIds(BuyerHierarchyData hierarchyData) {
        List<Long> userIds = new ArrayList<>();
        userIds.add(hierarchyData.getBuyerMainUser().getUserId());
        hierarchyData.getSubUsers().stream()
            .map(SysUser::getUserId)
            .filter(Objects::nonNull)
            .forEach(userIds::add);
        return userIds;
    }

    private Map<Long, Set<String>> loadRoleKeys(Set<Long> userIds) {
        Map<Long, Set<String>> result = new HashMap<>();
        if (CollectionUtils.isEmpty(userIds)) {
            return result;
        }
        List<Map<String, Object>> rows = userMapper.selectUserRoleMappings(userIds);
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Object roleKeyObj = row.get("roleKey");
            Long userId = parseLong(row.get("userId"));
            if (userId == null || !(roleKeyObj instanceof String)) {
                continue;
            }
            String roleKey = ((String) roleKeyObj).trim();
            if (roleKey.isEmpty()) {
                continue;
            }
            result.computeIfAbsent(userId, id -> new HashSet<>()).add(roleKey);
        }
        return result;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class BuyerHierarchyData {
        private final SysUser buyerMainUser;
        private final List<SysUser> subUsers;
        private final List<AiInstance> allInstances;
        private final Map<Long, List<AiInstance>> instanceMap;

        BuyerHierarchyData(SysUser buyerMainUser,
                           List<SysUser> subUsers,
                           List<AiInstance> allInstances,
                           Map<Long, List<AiInstance>> instanceMap) {
            this.buyerMainUser = buyerMainUser;
            this.subUsers = subUsers != null ? subUsers : Collections.emptyList();
            this.allInstances = allInstances != null ? allInstances : Collections.emptyList();
            this.instanceMap = instanceMap != null ? instanceMap : Collections.emptyMap();
        }

        public SysUser getBuyerMainUser() {
            return buyerMainUser;
        }

        public List<SysUser> getSubUsers() {
            return subUsers;
        }

        public List<AiInstance> getAllInstances() {
            return allInstances;
        }

        public Map<Long, List<AiInstance>> getInstanceMap() {
            return instanceMap;
        }
    }
}
