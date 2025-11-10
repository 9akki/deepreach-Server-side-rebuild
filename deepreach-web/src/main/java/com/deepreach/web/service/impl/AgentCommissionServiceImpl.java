package com.deepreach.web.service.impl;

import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.mapper.SysUserMapper;
import com.deepreach.common.core.service.UserHierarchyService;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.common.security.UserRoleUtils;
import com.deepreach.common.security.enums.UserIdentity;
import com.deepreach.common.utils.StringUtils;
import com.deepreach.web.dto.AdminSettlementQueryRequest;
import com.deepreach.web.entity.AgentCommissionAccount;
import com.deepreach.web.entity.AgentCommissionRecord;
import com.deepreach.web.entity.AgentCommissionSettlement;
import com.deepreach.web.entity.AgentCommissionSettlementRecord;
import com.deepreach.common.core.domain.entity.DrPriceConfig;
import com.deepreach.web.mapper.AgentCommissionAccountMapper;
import com.deepreach.web.mapper.AgentCommissionRecordMapper;
import com.deepreach.web.mapper.AgentCommissionSettlementMapper;
import com.deepreach.web.mapper.AgentCommissionSettlementRecordMapper;
import com.deepreach.web.service.AgentCommissionService;
import com.deepreach.common.core.service.DrPriceConfigService;
import com.deepreach.web.dto.AgentCommissionAccountDTO;
import com.deepreach.web.dto.AgentCommissionSummaryResponse;
import com.deepreach.web.dto.AgentCommissionRecordDTO;
import com.deepreach.web.dto.AgentCommissionRecordQuery;
import com.deepreach.web.dto.AgentCommissionOverviewRequest;
import com.deepreach.web.dto.AgentCommissionOverviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代理佣金业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCommissionServiceImpl implements AgentCommissionService {

    private final AgentCommissionAccountMapper accountMapper;
    private final AgentCommissionRecordMapper recordMapper;
    private final AgentCommissionSettlementMapper settlementMapper;
    private final AgentCommissionSettlementRecordMapper settlementRecordMapper;
    private final SysUserMapper userMapper;
    private final DrPriceConfigService drPriceConfigService;
    private final SysUserService userService;
    private final UserHierarchyService hierarchyService;

    private static final BigDecimal DEFAULT_LEVEL1_RATE = new BigDecimal("0.30");
    private static final BigDecimal DEFAULT_LEVEL2_RATE = new BigDecimal("0.20");
    private static final BigDecimal DEFAULT_LEVEL3_RATE = new BigDecimal("0.10");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final String BUSINESS_TYPE_AGENT_LEVEL1 = DrPriceConfig.BUSINESS_TYPE_AGENT_LEVEL1_COMMISSION;
    private static final String BUSINESS_TYPE_AGENT_LEVEL2 = DrPriceConfig.BUSINESS_TYPE_AGENT_LEVEL2_COMMISSION;
    private static final String BUSINESS_TYPE_AGENT_LEVEL3 = DrPriceConfig.BUSINESS_TYPE_AGENT_LEVEL3_COMMISSION;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void distributeRechargeCommission(Long buyerUserId,
                                             Long buyerDeptId,
                                             BigDecimal rechargeAmount,
                                             Long operatorId,
                                             Long billingId) {
        if (buyerUserId == null) {
            log.warn("无法进行佣金分发：缺少买家用户信息");
            return;
        }
        if (rechargeAmount == null || rechargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("充值金额无效，跳过佣金分发：userId={}, amount={}", buyerUserId, rechargeAmount);
            return;
        }

        List<AgentHierarchyNode> agentNodes = resolveAgentHierarchy(buyerUserId);
        if (agentNodes.isEmpty()) {
            log.info("买家用户无代理上线，跳过佣金分发：userId={}", buyerUserId);
            return;
        }

        CommissionRateConfig rateConfig = loadCommissionRates();
        int maxOrder = agentNodes.stream()
            .mapToInt(AgentHierarchyNode::getOrder)
            .max()
            .orElse(agentNodes.size());

        Map<Long, BigDecimal> allocatedRates = calculateCommissionRateByGrade(agentNodes, rateConfig, maxOrder);

        for (AgentHierarchyNode node : agentNodes) {
            BigDecimal rate = allocatedRates.getOrDefault(node.agentUserId, ZERO);
            if (rate.compareTo(ZERO) <= 0) {
                continue;
            }

            BigDecimal commission = rechargeAmount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
            if (commission.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            AgentCommissionAccount account = getOrCreateAccount(node.agentUserId);
            if (!account.isNormal()) {
                log.warn("代理佣金账户状态异常，跳过分发：agentUserId={}", node.agentUserId);
                continue;
            }

            int updated = accountMapper.incrementCommission(node.agentUserId, commission, commission);
            if (updated <= 0) {
                log.warn("更新代理佣金账户失败：agentUserId={}", node.agentUserId);
                continue;
            }

            AgentCommissionRecord record = new AgentCommissionRecord();
            record.setAgentUserId(node.agentUserId);
            record.setBuyerUserId(buyerUserId);
            record.setTriggerBillingId(billingId);
            record.setTriggerAmount(rechargeAmount);
            record.setCommissionAmount(commission);
            record.setCommissionRate(rate);
            int hierarchyLevel = resolveAgentLevel(node, maxOrder);
            record.setHierarchyLevel(hierarchyLevel);
            record.setDirection(AgentCommissionRecord.DIRECTION_CREDIT);
            record.setBusinessType(AgentCommissionRecord.BUSINESS_TYPE_RECHARGE);
            record.setStatus(AgentCommissionRecord.STATUS_SUCCESS);
            record.setOperatorId(operatorId);
            record.setDescription(buildCommissionDescription(hierarchyLevel, rechargeAmount, commission));
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            recordMapper.insert(record);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentCommissionAccount getOrCreateAccount(Long agentUserId) {
        AgentCommissionAccount account = accountMapper.selectByAgentUserId(agentUserId);
        if (account != null) {
            return account;
        }

        AgentCommissionAccount newAccount = AgentCommissionAccount.createForAgent(agentUserId);
        newAccount.setCreateBy("system");
        newAccount.setCreateTime(LocalDateTime.now());
        newAccount.setUpdateBy("system");
        newAccount.setUpdateTime(LocalDateTime.now());
        try {
            accountMapper.insert(newAccount);
            return newAccount;
        } catch (DuplicateKeyException ex) {
            log.debug("代理佣金账户已存在，忽略并重新查询：agentUserId={}", agentUserId);
            return accountMapper.selectByAgentUserId(agentUserId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentCommissionSettlement createSettlement(AgentCommissionSettlement settlement) {
        if (settlement == null || settlement.getAgentUserId() == null) {
            throw new IllegalArgumentException("结算申请信息不完整");
        }
        if (settlement.getRequestAmount() == null || settlement.getRequestAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("结算申请金额必须大于0");
        }

        AgentCommissionAccount account = getOrCreateAccount(settlement.getAgentUserId());
        BigDecimal available = account.getAvailableCommission();
        if (available == null) {
            available = ZERO;
        }
        if (available.compareTo(settlement.getRequestAmount()) < 0) {
            throw new IllegalStateException("可用佣金不足，无法发起结算申请");
        }

        int updated = accountMapper.adjustAvailableCommission(
            settlement.getAgentUserId(),
            settlement.getRequestAmount().negate(),
            ZERO,
            settlement.getRequestAmount(),
            ZERO
        );
        if (updated <= 0) {
            throw new IllegalStateException("更新佣金账户失败，请重试");
        }

        settlement.setStatus(AgentCommissionSettlement.STATUS_PENDING);
        settlement.setCreateTime(LocalDateTime.now());
        settlement.setCreateBy(settlement.getCreateBy() == null ? "system" : settlement.getCreateBy());
        settlement.setUpdateTime(settlement.getCreateTime());
        settlement.setUpdateBy(settlement.getCreateBy());
        settlementMapper.insert(settlement);
        return settlement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentCommissionSettlement applySettlement(Long agentUserId,
                                                     BigDecimal amount,
                                                     Long operatorId,
                                                     String remark,
                                                     String network,
                                                     String address) {
        if (agentUserId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("结算申请参数不完整");
        }
        if (network == null || network.trim().isEmpty()) {
            throw new IllegalArgumentException("network不能为空");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("address不能为空");
        }
        AgentCommissionAccount account = getOrCreateAccount(agentUserId);
        BigDecimal available = account.getAvailableCommission();
        if (available.compareTo(amount) < 0) {
            throw new IllegalStateException("可用佣金不足，无法发起结算申请");
        }

        int adjust = accountMapper.adjustAvailableCommission(
            agentUserId,
            amount.negate(),
            ZERO,
            amount,
            ZERO
        );
        if (adjust <= 0) {
            throw new IllegalStateException("冻结佣金失败，请重试");
        }

        AgentCommissionSettlement settlement = new AgentCommissionSettlement();
        settlement.setAgentUserId(agentUserId);
        settlement.setRequestAmount(amount);
        settlement.setApprovedAmount(BigDecimal.ZERO);
        settlement.setStatus(AgentCommissionSettlement.STATUS_PENDING);
        settlement.setRemark(remark);
        settlement.setNetwork(network);
        settlement.setAddress(address);
        settlement.setCreateBy(operatorId == null ? String.valueOf(agentUserId) : String.valueOf(operatorId));
        LocalDateTime now = LocalDateTime.now();
        settlement.setCreateTime(now);
        settlement.setUpdateTime(now);
        settlement.setUpdateBy(settlement.getCreateBy());
        settlementMapper.insert(settlement);

        AgentCommissionSettlementRecord record = buildSettlementRecord(
            settlement.getSettlementId(),
            agentUserId,
            amount,
            AgentCommissionSettlementRecord.DIRECTION_DEBIT,
            AgentCommissionSettlementRecord.STATUS_SUCCESS,
            operatorId,
            "提交结算申请，转入待结算",
            null
        );
        settlementRecordMapper.insert(record);

        return settlement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentCommissionSettlement approveSettlement(Long settlementId,
                                                       BigDecimal approvedAmount,
                                                       Long operatorId,
                                                       String remark) {
        AgentCommissionSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new IllegalArgumentException("结算申请不存在");
        }
        if (!AgentCommissionSettlement.STATUS_PENDING.equals(settlement.getStatus())) {
            throw new IllegalStateException("结算申请已处理");
        }

        BigDecimal amount = approvedAmount != null ? approvedAmount : settlement.getRequestAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("审批金额必须大于0");
        }
        if (amount.compareTo(settlement.getRequestAmount()) > 0) {
            throw new IllegalArgumentException("审批金额不能超过申请金额");
        }

        BigDecimal requestAmount = settlement.getRequestAmount();
        BigDecimal unapprovedAmount = requestAmount.subtract(amount);
        int adjust = accountMapper.adjustAvailableCommission(
            settlement.getAgentUserId(),
            unapprovedAmount,
            ZERO,
            requestAmount.negate(),
            amount
        );
        if (adjust <= 0) {
            throw new IllegalStateException("更新佣金账户失败，请重试");
        }

        settlement.setApprovedAmount(amount);
        settlement.setStatus(AgentCommissionSettlement.STATUS_APPROVED);
        settlement.setApprovalUserId(operatorId);
        settlement.setApprovalTime(LocalDateTime.now());
        settlement.setRemark(remark);
        settlementMapper.update(settlement);

        AgentCommissionSettlementRecord record = buildSettlementRecord(
            settlementId,
            settlement.getAgentUserId(),
            amount,
            AgentCommissionSettlementRecord.DIRECTION_DEBIT,
            AgentCommissionSettlementRecord.STATUS_SUCCESS,
            operatorId,
            "结算审批通过，金额结算",
            null
        );
        settlementRecordMapper.insert(record);

        if (unapprovedAmount.compareTo(ZERO) > 0) {
            AgentCommissionSettlementRecord refundRecord = buildSettlementRecord(
                settlementId,
                settlement.getAgentUserId(),
                unapprovedAmount,
                AgentCommissionSettlementRecord.DIRECTION_CREDIT,
                AgentCommissionSettlementRecord.STATUS_SUCCESS,
                operatorId,
                "结算审批未通过部分返还",
                null
            );
            settlementRecordMapper.insert(refundRecord);
        }

        return settlement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentCommissionSettlement rejectSettlement(Long settlementId,
                                                      Long operatorId,
                                                      String remark) {
        AgentCommissionSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new IllegalArgumentException("结算申请不存在");
        }
        if (!AgentCommissionSettlement.STATUS_PENDING.equals(settlement.getStatus())) {
            throw new IllegalStateException("结算申请已处理");
        }

        BigDecimal amount = settlement.getRequestAmount();
        int adjust = accountMapper.adjustAvailableCommission(
            settlement.getAgentUserId(),
            amount,
            ZERO,
            amount.negate(),
            ZERO
        );
        if (adjust <= 0) {
            throw new IllegalStateException("更新佣金账户失败，请重试");
        }

        settlement.setApprovedAmount(BigDecimal.ZERO);
        settlement.setStatus(AgentCommissionSettlement.STATUS_REJECTED);
        settlement.setApprovalUserId(operatorId);
        settlement.setApprovalTime(LocalDateTime.now());
        settlement.setRemark(remark);
        settlementMapper.update(settlement);

        AgentCommissionSettlementRecord record = buildSettlementRecord(
            settlementId,
            settlement.getAgentUserId(),
            amount,
            AgentCommissionSettlementRecord.DIRECTION_CREDIT,
            AgentCommissionSettlementRecord.STATUS_SUCCESS,
            operatorId,
            "结算审批拒绝，返还佣金",
            null
        );
        settlementRecordMapper.insert(record);

        return settlement;
    }

    @Override
    public AgentCommissionAccountDTO getCommissionAccount(Long agentUserId) {
        if (agentUserId == null) {
            throw new IllegalArgumentException("代理用户ID不能为空");
        }
        SysUser agentUser = userMapper.selectUserById(agentUserId);
        if (agentUser == null) {
            throw new IllegalArgumentException("代理用户不存在");
        }
        Map<Long, Set<String>> roleKeys = loadRoleKeys(Collections.singleton(agentUserId));
        Integer agentLevel = resolveAgentLevel(roleKeys.getOrDefault(agentUserId, Collections.emptySet()));
        AgentCommissionAccount account = accountMapper.selectByAgentUserId(agentUserId);
        AgentCommissionAccountDTO dto = buildAccountDTO(agentUser, agentLevel, account);
        dto.setEarnedCommissionInRange(BigDecimal.ZERO);
        return dto;
    }

    @Override
    public AgentCommissionSummaryResponse getAgentCommissionSummary(Long agentUserId) {
        if (agentUserId == null) {
            throw new IllegalArgumentException("代理用户ID不能为空");
        }
        SysUser agentUser = userMapper.selectUserById(agentUserId);
        if (agentUser == null) {
            throw new IllegalArgumentException("代理用户不存在");
        }
        AgentCommissionAccount account = accountMapper.selectByAgentUserId(agentUserId);
        AgentCommissionAccount source = account != null ? account : AgentCommissionAccount.createForAgent(agentUserId);
        AgentCommissionSummaryResponse summary = new AgentCommissionSummaryResponse();
        summary.setAgentUserId(agentUserId);
        summary.setTotalCommission(defaultZero(source.getTotalCommission()));
        summary.setSettlementCommission(defaultZero(source.getSettledCommission()));
        summary.setAvailableCommission(defaultZero(source.getAvailableCommission()));
        return summary;
    }

    @Override
    public List<AgentCommissionRecordDTO> getCommissionRecords(Long agentUserId, AgentCommissionRecordQuery query) {
        if (agentUserId == null) {
            throw new IllegalArgumentException("代理用户ID不能为空");
        }
        AgentCommissionRecordQuery safeQuery = query != null ? query : new AgentCommissionRecordQuery();
        List<Map<String, Object>> rows = recordMapper.selectRecordsByAgent(
            agentUserId,
            safeQuery.getStartTime(),
            safeQuery.getEndTime(),
            safeQuery.getMinAmount(),
            safeQuery.getMaxAmount()
        );
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
            .map(this::convertRecordRow)
            .collect(Collectors.toList());
    }

    @Override
    public AgentCommissionOverviewResponse getAgentCommissionOverview(Long currentUserId, AgentCommissionOverviewRequest request) {
        AgentCommissionOverviewResponse response = new AgentCommissionOverviewResponse();
        AgentCommissionOverviewRequest safeRequest = request != null ? request : new AgentCommissionOverviewRequest();

        LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        if (currentUser == null) {
            return response;
        }

        Long scopeRoot = null;
        if (!currentUser.isAdminIdentity() && !SecurityUtils.hasPermission("system:user:list")) {
            scopeRoot = currentUser.getUserId();
        }

        List<SysUser> agents = userService.selectUsersWithinHierarchy(scopeRoot, "agent", null);
        if (agents == null || agents.isEmpty()) {
            return response;
        }

        String keyword = safeRequest.getUsername() != null ? safeRequest.getUsername().trim().toLowerCase() : null;
        List<SysUser> filteredAgents = agents.stream()
            .filter(Objects::nonNull)
            .filter(user -> user.getUserId() != null)
            .filter(user -> {
                if (keyword == null || keyword.isEmpty()) {
                    return true;
                }
                String username = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
                String nickname = user.getNickname() != null ? user.getNickname().toLowerCase() : "";
                return username.contains(keyword) || nickname.contains(keyword);
            })
            .collect(Collectors.toList());

        if (filteredAgents.isEmpty()) {
            return response;
        }

        Set<Long> agentUserIds = filteredAgents.stream()
            .map(SysUser::getUserId)
            .collect(Collectors.toCollection(HashSet::new));

        Map<Long, Set<String>> roleKeyMap = loadRoleKeys(agentUserIds);

        List<Map<String, Object>> accountRows = accountMapper.selectAccountsByAgentUserIds(new HashSet<>(agentUserIds));
        Map<Long, Map<String, Object>> accountRowMap = accountRows == null ? Collections.emptyMap()
            : accountRows.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(row -> parseLong(row.get("agentUserId")), row -> row));

        List<Map<String, Object>> sumRows = recordMapper.sumCommissionByAgents(
            new ArrayList<>(agentUserIds),
            safeRequest.getStartTime(),
            safeRequest.getEndTime(),
            safeRequest.getMinAmount(),
            safeRequest.getMaxAmount()
        );
        Map<Long, BigDecimal> sumMap = sumRows == null ? Collections.emptyMap()
            : sumRows.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(row -> parseLong(row.get("agentUserId")), row -> toBigDecimal(row.get("commissionSum"))));

        List<AgentCommissionAccountDTO> resultList = new ArrayList<>();

        for (SysUser agent : filteredAgents) {
            Set<String> roleKeys = roleKeyMap.getOrDefault(agent.getUserId(), Collections.emptySet());
            Integer agentLevel = resolveAgentLevel(roleKeys);

            AgentCommissionAccountDTO dto = buildAccountDTO(
                agent,
                agentLevel,
                accountRowMap.containsKey(agent.getUserId())
                    ? toAccount(accountRowMap.get(agent.getUserId()))
                    : null);

            BigDecimal inRange = sumMap.getOrDefault(agent.getUserId(), BigDecimal.ZERO);
            dto.setEarnedCommissionInRange(inRange);
            if (!sumMap.containsKey(agent.getUserId()) && safeRequest.getMinAmount() != null) {
                if (BigDecimal.ZERO.compareTo(safeRequest.getMinAmount()) < 0) {
                    // 如果设置了最小金额且该代理没有佣金，仍保留，但earnedCommissionInRange为0
                }
            }
            response.setTotalSettlementCommission(response.getTotalSettlementCommission().add(dto.getSettlementCommission()));
            response.setEarnedCommissionInRange(response.getEarnedCommissionInRange().add(inRange));
            resultList.add(dto);
        }

        resultList.sort(Comparator.comparing(AgentCommissionAccountDTO::getDeptLevel, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(AgentCommissionAccountDTO::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)));

        response.setAgentCount(resultList.size());
        response.setAgents(resultList);
        return response;
    }

    @Override
    public List<AgentCommissionSettlement> listSettlementsByStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }
        return settlementMapper.selectByStatuses(statuses);
    }

    @Override
    public List<AgentCommissionSettlement> searchAdminSettlements(AdminSettlementQueryRequest query) {
        AdminSettlementQueryRequest effective = query != null ? query : new AdminSettlementQueryRequest();
        String username = StringUtils.trimToNull(effective.getUsername());
        LocalDateTime beginTime = parseDate(effective.getBeginTime(), true);
        LocalDateTime endTime = parseDate(effective.getEndTime(), false);
        return settlementMapper.searchAdminSettlements(
            effective.getSettlementId(),
            effective.getUserId(),
            username,
            beginTime,
            endTime
        );
    }

    @Override
    public BigDecimal sumSettledCommission() {
        BigDecimal sum = accountMapper.sumSettledCommission();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentCommissionAccountDTO manualAdjustCommission(Long agentUserId,
                                                            BigDecimal amount,
                                                            Long operatorId,
                                                            String remark) {
        if (agentUserId == null) {
            throw new IllegalArgumentException("代理用户ID不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("调整金额不能为空且不能为0");
        }

        SysUser agentUser = userMapper.selectUserById(agentUserId);
        if (agentUser == null) {
            throw new IllegalArgumentException("代理用户不存在");
        }
        Map<Long, Set<String>> roleKeys = loadRoleKeys(Collections.singleton(agentUserId));
        Integer agentLevel = resolveAgentLevel(roleKeys.getOrDefault(agentUserId, Collections.emptySet()));
        if (agentLevel == null) {
            throw new IllegalArgumentException("指定用户不是代理账号");
        }

        AgentCommissionAccount account = accountMapper.selectByAgentUserId(agentUserId);
        if (account == null) {
            account = getOrCreateAccount(agentUserId);
        }
        if (account == null || !account.isNormal()) {
            throw new IllegalStateException("代理佣金账户不存在或状态异常");
        }

        boolean isIncrease = amount.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal changeAmount = amount.abs();
        BigDecimal appliedChange = ZERO;
        if (isIncrease) {
            appliedChange = changeAmount;
            int updated = accountMapper.adjustAvailableCommission(
                agentUserId,
                appliedChange,
                ZERO,
                ZERO,
                ZERO
            );
            if (updated <= 0) {
                throw new IllegalStateException("调整可用佣金失败，请重试");
            }
        } else {
            BigDecimal available = account.getAvailableCommission() != null ? account.getAvailableCommission() : ZERO;
            if (available.compareTo(ZERO) > 0) {
                appliedChange = changeAmount.min(available);
                if (appliedChange.compareTo(ZERO) > 0) {
                    int updated = accountMapper.adjustAvailableCommission(
                        agentUserId,
                        appliedChange.negate(),
                        ZERO,
                        ZERO,
                        ZERO
                    );
                    if (updated <= 0) {
                        throw new IllegalStateException("扣减佣金账户失败，请重试");
                    }
                }
            }
        }

        AgentCommissionAccount updatedAccount = accountMapper.selectByAgentUserId(agentUserId);
        if (updatedAccount == null) {
            throw new IllegalStateException("更新后佣金账户不存在");
        }

        if (appliedChange.compareTo(ZERO) > 0) {
            AgentCommissionRecord record = new AgentCommissionRecord();
            record.setAgentUserId(agentUserId);
            record.setAgentDeptId(null);
            record.setBuyerUserId(agentUserId);
            record.setBuyerDeptId(null);
            record.setTriggerBillingId(null);
            record.setTriggerAmount(appliedChange);
            record.setCommissionAmount(appliedChange);
            record.setCommissionRate(BigDecimal.ZERO);
            record.setHierarchyLevel(agentLevel != null ? agentLevel : 0);
            record.setDirection(isIncrease ? AgentCommissionRecord.DIRECTION_CREDIT : AgentCommissionRecord.DIRECTION_DEBIT);
            record.setBusinessType(AgentCommissionRecord.BUSINESS_TYPE_MANUAL_ADJUST);
            record.setStatus(AgentCommissionRecord.STATUS_SUCCESS);
            record.setOperatorId(operatorId != null ? operatorId : agentUserId);
            record.setDescription(remark != null && !remark.trim().isEmpty()
                ? remark.trim()
                : (isIncrease ? "手动调增佣金" : "手动调减佣金"));
            LocalDateTime now = LocalDateTime.now();
            record.setCreateTime(now);
            record.setUpdateTime(now);
            recordMapper.insert(record);
        }

        AgentCommissionAccountDTO dto = buildAccountDTO(agentUser, agentLevel, updatedAccount);
        dto.setEarnedCommissionInRange(BigDecimal.ZERO);
        return dto;
    }


    private AgentCommissionAccountDTO buildAccountDTO(SysUser agent, Integer agentLevel, AgentCommissionAccount account) {
        AgentCommissionAccountDTO dto = new AgentCommissionAccountDTO();
        dto.setAgentUserId(agent.getUserId());
        dto.setUsername(agent.getUsername());
        dto.setNickname(agent.getNickname());
        dto.setDeptId(null);
        dto.setDeptName(null);
        dto.setDeptLevel(agentLevel);

        AgentCommissionAccount source = account != null ? account : AgentCommissionAccount.createForAgent(agent.getUserId());
        dto.setTotalCommission(defaultZero(source.getTotalCommission()));
        dto.setAvailableCommission(defaultZero(source.getAvailableCommission()));
        dto.setFrozenCommission(defaultZero(source.getFrozenCommission()));
        dto.setPendingSettlementCommission(defaultZero(source.getPendingSettlementCommission()));
        dto.setSettlementCommission(defaultZero(source.getSettledCommission()));
        return dto;
    }

    private AgentCommissionRecordDTO convertRecordRow(Map<String, Object> row) {
        AgentCommissionRecordDTO dto = new AgentCommissionRecordDTO();
        dto.setRecordId(parseLong(row.get("record_id")));
        dto.setAgentUserId(parseLong(row.get("agent_user_id")));
        dto.setAgentDeptId(parseLong(row.get("agent_dept_id")));
        dto.setBuyerUserId(parseLong(row.get("buyer_user_id")));
        dto.setBuyerUsername((String) row.get("buyerUsername"));
        dto.setTriggerBillingId(parseLong(row.get("trigger_billing_id")));
        dto.setBillingNo((String) row.get("bill_no"));
        dto.setTriggerAmount(toBigDecimal(row.get("trigger_amount")));
        dto.setCommissionAmount(toBigDecimal(row.get("commission_amount")));
        dto.setCommissionRate(toBigDecimal(row.get("commission_rate")));
        dto.setHierarchyLevel(row.get("hierarchy_level") != null ? ((Number) row.get("hierarchy_level")).intValue() : null);
        dto.setBusinessType((String) row.get("business_type"));
        dto.setStatus((String) row.get("status"));
        dto.setDescription((String) row.get("description"));
        dto.setCreateTime(toLocalDateTime(row.get("create_time")));
        return dto;
    }

    private AgentCommissionAccount toAccount(Map<String, Object> row) {
        AgentCommissionAccount account = AgentCommissionAccount.createForAgent(parseLong(row.get("agentUserId")));
        account.setTotalCommission(toBigDecimal(row.get("totalCommission")));
        account.setAvailableCommission(toBigDecimal(row.get("availableCommission")));
        account.setFrozenCommission(toBigDecimal(row.get("frozenCommission")));
        account.setPendingSettlementCommission(toBigDecimal(row.get("pendingSettlementCommission")));
        account.setSettledCommission(toBigDecimal(row.get("settledCommission")));
        account.setStatus(row.get("accountStatus") != null ? String.valueOf(row.get("accountStatus")) : "0");
        account.setUpdateTime(toLocalDateTime(row.get("accountUpdateTime")));
        return account;
    }

    private Map<Long, Set<String>> loadRoleKeys(Set<Long> userIds) {
        Map<Long, Set<String>> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
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
            Long userId = parseLong(row.get("userId"));
            Object roleKeyObj = row.get("roleKey");
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

    private Integer resolveAgentLevel(Set<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return null;
        }
        if (UserRoleUtils.hasIdentity(roleKeys, UserIdentity.AGENT_LEVEL_1)) {
            return 1;
        }
        if (UserRoleUtils.hasIdentity(roleKeys, UserIdentity.AGENT_LEVEL_2)) {
            return 2;
        }
        if (UserRoleUtils.hasIdentity(roleKeys, UserIdentity.AGENT_LEVEL_3)) {
            return 3;
        }
        return null;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        return null;
    }
    private List<AgentHierarchyNode> resolveAgentHierarchy(Long buyerUserId) {
        List<AgentHierarchyNode> nodes = new ArrayList<>();
        if (buyerUserId == null) {
            return nodes;
        }

        List<Long> ancestorIds = new ArrayList<>();
        Long current = hierarchyService.findParentId(buyerUserId);
        int safetyCounter = 0;
        while (current != null && safetyCounter++ < 20 && ancestorIds.size() < 6) {
            ancestorIds.add(current);
            current = hierarchyService.findParentId(current);
        }
        if (ancestorIds.isEmpty()) {
            return nodes;
        }

        Map<Long, Set<String>> roleKeys = loadRoleKeys(new HashSet<>(ancestorIds));
        Set<Long> visited = new HashSet<>();
        int order = 1;
        for (Long ancestorId : ancestorIds) {
            if (order > 3) {
                break;
            }
            if (!visited.add(ancestorId)) {
                continue;
            }
            Integer agentLevel = resolveAgentLevel(roleKeys.getOrDefault(ancestorId, Collections.emptySet()));
            if (agentLevel == null) {
                continue;
            }
            nodes.add(new AgentHierarchyNode(ancestorId, order, agentLevel));
            order++;
        }

        return nodes;
    }

    private CommissionRateConfig loadCommissionRates() {
        BigDecimal level1 = fetchCommissionRate(BUSINESS_TYPE_AGENT_LEVEL1, DEFAULT_LEVEL1_RATE);
        BigDecimal level2 = fetchCommissionRate(BUSINESS_TYPE_AGENT_LEVEL2, DEFAULT_LEVEL2_RATE);
        BigDecimal level3 = fetchCommissionRate(BUSINESS_TYPE_AGENT_LEVEL3, DEFAULT_LEVEL3_RATE);

        if (level2.compareTo(level1) > 0) {
            level2 = level1;
        }
        if (level3.compareTo(level2) > 0) {
            level3 = level2;
        }

        return new CommissionRateConfig(level1, level2, level3);
    }

    private BigDecimal fetchCommissionRate(String businessType, BigDecimal defaultValue) {
        try {
            DrPriceConfig config = drPriceConfigService.selectDrPriceConfigByBusinessType(businessType);
            if (config != null && config.getDrPrice() != null) {
                return config.getDrPrice().setScale(4, RoundingMode.HALF_UP);
            }
        } catch (Exception ex) {
            log.warn("读取佣金配置失败，将使用默认值: {}", businessType, ex);
        }
        return defaultValue.setScale(4, RoundingMode.HALF_UP);
    }

    private Map<Long, BigDecimal> calculateCommissionRateByGrade(List<AgentHierarchyNode> nodes,
                                                                 CommissionRateConfig rateConfig,
                                                                 int maxOrder) {
        Map<Long, BigDecimal> distribution = new HashMap<>();
        if (nodes == null || nodes.isEmpty()) {
            return distribution;
        }

        List<AgentHierarchyNode> sorted = new ArrayList<>(nodes);
        sorted.sort(Comparator.comparingInt((AgentHierarchyNode node) ->
            resolveAgentLevel(node, maxOrder)).reversed());

        BigDecimal totalAllocated = ZERO;
        BigDecimal maxLowerLevelRate = ZERO;
        for (AgentHierarchyNode node : sorted) {
            int level = resolveAgentLevel(node, maxOrder);
            BigDecimal targetRate = rateConfig.getRateByAgentLevel(level);
            if (targetRate.compareTo(ZERO) <= 0) {
                continue;
            }

            BigDecimal share = targetRate.subtract(maxLowerLevelRate);
            if (share.compareTo(ZERO) <= 0) {
                continue;
            }

            BigDecimal remainingCap = rateConfig.getTotalCap().subtract(totalAllocated);
            if (remainingCap.compareTo(ZERO) <= 0) {
                break;
            }
            if (share.compareTo(remainingCap) > 0) {
                share = remainingCap;
            }

            distribution.put(node.agentUserId, share.setScale(4, RoundingMode.HALF_UP));
            totalAllocated = totalAllocated.add(share);
            if (targetRate.compareTo(maxLowerLevelRate) > 0) {
                maxLowerLevelRate = targetRate;
            }
        }

        return distribution;
    }

    private String buildCommissionDescription(int level, BigDecimal rechargeAmount, BigDecimal commission) {
        return String.format("第%d级代理获得充值佣金，充值金额:%s, 佣金:%s", level,
            rechargeAmount.stripTrailingZeros().toPlainString(),
            commission.stripTrailingZeros().toPlainString());
    }

    private static class AgentHierarchyNode {
        private final Long agentUserId;
        private final int order;
        private final Integer agentLevel;

        private AgentHierarchyNode(Long agentUserId, int order, Integer agentLevel) {
            this.agentUserId = agentUserId;
            this.order = order;
            this.agentLevel = agentLevel;
        }

        private int getOrder() {
            return order;
        }

        private Integer getAgentLevel() {
            return agentLevel;
        }
    }

    private static class CommissionRateConfig {
        private final BigDecimal level1Rate;
        private final BigDecimal level2Rate;
        private final BigDecimal level3Rate;

        private CommissionRateConfig(BigDecimal level1Rate, BigDecimal level2Rate, BigDecimal level3Rate) {
            this.level1Rate = level1Rate;
            this.level2Rate = level2Rate;
            this.level3Rate = level3Rate;
        }

        private BigDecimal getRateByAgentLevel(Integer agentLevel) {
            if (agentLevel == null) {
                return level1Rate;
            }
            switch (agentLevel) {
                case 1:
                    return level1Rate;
                case 2:
                    return level2Rate;
                case 3:
                    return level3Rate;
                default:
                    return ZERO;
            }
        }

        private BigDecimal getTotalCap() {
            return level1Rate;
        }
    }

    private int resolveAgentLevel(AgentHierarchyNode node, int maxOrder) {
        int level = node.getAgentLevel() != null ? node.getAgentLevel() : -1;
        if (level >= 1 && level <= 3) {
            return level;
        }
        int inferred = maxOrder - node.getOrder() + 1;
        if (inferred < 1) {
            inferred = 1;
        } else if (inferred > 3) {
            inferred = 3;
        }
        return inferred;
    }

    private AgentCommissionSettlementRecord buildSettlementRecord(Long settlementId,
                                                                  Long agentUserId,
                                                                  BigDecimal amount,
                                                                  String direction,
                                                                  String status,
                                                                  Long operatorId,
                                                                  String description,
                                                                  String extraData) {
        AgentCommissionSettlementRecord record = new AgentCommissionSettlementRecord();
        record.setSettlementId(settlementId);
        record.setAgentUserId(agentUserId);
        record.setChangeAmount(amount);
        record.setDirection(direction);
        record.setStatus(status);
        record.setOperatorId(operatorId);
        record.setDescription(description);
        record.setExtraData(extraData);
        record.setCreateBy(operatorId == null ? "system" : String.valueOf(operatorId));
        LocalDateTime now = LocalDateTime.now();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setUpdateBy(record.getCreateBy());
        return record;
    }

    private static final DateTimeFormatter[] ADMIN_QUERY_FORMATTERS = new DateTimeFormatter[]{
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ISO_DATE
    };

    private LocalDateTime parseDate(String value, boolean isBegin) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (DateTimeFormatter formatter : ADMIN_QUERY_FORMATTERS) {
            try {
                if (formatter == DateTimeFormatter.ISO_DATE) {
                    LocalDateTime startOfDay = java.time.LocalDate.parse(value, formatter).atStartOfDay();
                    return isBegin ? startOfDay : startOfDay.plusDays(1).minusSeconds(1);
                }
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
