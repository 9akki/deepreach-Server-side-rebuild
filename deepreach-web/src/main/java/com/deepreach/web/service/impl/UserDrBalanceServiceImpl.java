package com.deepreach.web.service.impl;

import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.web.dto.DrBalanceAdjustResult;
import com.deepreach.web.dto.RechargeResult;
import com.deepreach.web.entity.UserDrBalance;
import com.deepreach.web.entity.DrBillingRecord;
import com.deepreach.web.mapper.UserDrBalanceMapper;
import com.deepreach.web.service.AgentCommissionService;
import com.deepreach.web.service.UserDrBalanceService;
import com.deepreach.web.service.DrBillingRecordService;
import com.deepreach.web.dto.DeductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * 用户DR积分余额服务实现类
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDrBalanceServiceImpl implements UserDrBalanceService {

    private final UserDrBalanceMapper balanceMapper;
    private final DrBillingRecordService billingRecordService;
    private final AgentCommissionService agentCommissionService;
    private final SysUserService sysUserService;

    private static final BigDecimal INSTANCE_PRE_DEDUCT_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal INSTANCE_CREATION_RATIO = new BigDecimal("100.00");

    @Override
    public UserDrBalance getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        UserDrBalance balance = balanceMapper.selectByUserId(userId);
        if (balance == null) {
            balance = createBalanceAccount(userId);
        }
        return balance;
    }

    @Override
    public List<UserDrBalance> selectBalancePage(UserDrBalance balance) {
        return balanceMapper.selectBalancePage(balance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeResult recharge(DrBillingRecord requestRecord, Long operatorId) {
        if (requestRecord == null) {
            throw new IllegalArgumentException("充值记录不能为空");
        }
        Long userId = requestRecord.getUserId();
        BigDecimal amount = requestRecord.getDrAmount();
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("用户ID和充值金额不能为空且必须大于0");
        }

        UserDrBalance balance = getByUserId(userId);
        if (balance == null) {
            balance = createBalanceAccount(userId);
            if (balance == null) {
                throw new RuntimeException("创建用户余额账户失败");
            }
        }

        if (!balance.isNormal()) {
            throw new RuntimeException("用户余额账户状态异常，无法充值");
        }

        BigDecimal balanceBefore = balance.getDrBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        BigDecimal totalRechargeAfter = balance.getTotalRecharge().add(amount);

        int result = balanceMapper.updateBalanceWithVersion(
            userId,
            balanceAfter,
            balance.getPreDeductedBalance(),
            totalRechargeAfter,
            balance.getTotalConsume(),
            balance.getTotalRefund(),
            balance.getFrozenAmount(),
            balance.getVersion()
        );

        if (result == 0) {
            throw new RuntimeException("充值失败，请重试");
        }

        // 创建充值账单记录
        DrBillingRecord billingRecord = DrBillingRecord.createRechargeRecord(
            userId, operatorId, amount, balanceBefore, balanceAfter
        );
        billingRecord.setBillNo(generateBillNo());
        billingRecord.setCreateBy(operatorId == null ? "system" : operatorId.toString());
        billingRecord.setCreateTime(LocalDateTime.now());
        billingRecord.setExtraData(requestRecord.getExtraData());
        billingRecord.setRemark(requestRecord.getRemark());
        billingRecord.setBusinessId(requestRecord.getBusinessId());
        billingRecord.setConsumer(resolveConsumerUsername(operatorId, userId));
        DrBillingRecord createdRecord = billingRecordService.createRecord(billingRecord);

        SysUser buyerUser = sysUserService.selectUserWithDept(userId);
        Long buyerDeptId = null;
        if (buyerUser != null) {
            buyerDeptId = buyerUser.getDeptId();
            if (buyerDeptId == null && buyerUser.getDept() != null) {
                buyerDeptId = buyerUser.getDept().getDeptId();
            }
        }

        // 向代理发放佣金
        agentCommissionService.distributeRechargeCommission(
            userId,
            buyerDeptId,
            amount,
            operatorId,
            createdRecord != null ? createdRecord.getBillId() : null
        );

        // 重新查询余额信息
        UserDrBalance updatedBalance = getByUserId(userId);
        DrBillingRecord responseRecord = createdRecord != null ? createdRecord : billingRecord;
        return new RechargeResult(updatedBalance, responseRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DrBalanceAdjustResult manualAdjustBalance(Long userId, BigDecimal amount, Long operatorId, String remark) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("调账金额不能为空且不能为0");
        }

        UserDrBalance balance = getByUserId(userId);
        if (balance == null) {
            throw new RuntimeException("用户余额账户不存在");
        }
        if (!balance.isNormal()) {
            throw new RuntimeException("用户余额账户状态异常，无法调账");
        }

        BigDecimal balanceBefore = balance.getDrBalance() != null ? balance.getDrBalance() : BigDecimal.ZERO;
        BigDecimal appliedChange;
        BigDecimal newBalance;
        BigDecimal totalRecharge = balance.getTotalRecharge() != null ? balance.getTotalRecharge() : BigDecimal.ZERO;
        BigDecimal totalConsume = balance.getTotalConsume() != null ? balance.getTotalConsume() : BigDecimal.ZERO;
        BigDecimal totalRefund = balance.getTotalRefund() != null ? balance.getTotalRefund() : BigDecimal.ZERO;
        BigDecimal frozenAmount = balance.getFrozenAmount() != null ? balance.getFrozenAmount() : BigDecimal.ZERO;
        BigDecimal newTotalRecharge = totalRecharge;
        BigDecimal newTotalConsume = totalConsume;

        boolean isIncrease = amount.compareTo(BigDecimal.ZERO) > 0;
        if (isIncrease) {
            appliedChange = amount;
            newBalance = balanceBefore.add(appliedChange);
            newTotalRecharge = newTotalRecharge.add(appliedChange);
        } else {
            BigDecimal deduction = amount.abs();
            BigDecimal actualDeduction = balanceBefore.min(deduction);
            appliedChange = actualDeduction.negate();
            newBalance = balanceBefore.add(appliedChange);
            newTotalConsume = newTotalConsume.add(actualDeduction);
        }

        int updated = balanceMapper.updateBalanceWithVersion(
            userId,
            newBalance,
            balance.getPreDeductedBalance(),
            newTotalRecharge,
            newTotalConsume,
            totalRefund,
            frozenAmount,
            balance.getVersion()
        );
        if (updated <= 0) {
            throw new RuntimeException("调账失败，请稍后重试");
        }

        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(userId);
        record.setOperatorId(operatorId);
        record.setBillNo(generateBillNo());
        record.setBillType(isIncrease ? 1 : 2);
        record.setBillingType(1);
        record.setBusinessType(DrBillingRecord.BUSINESS_TYPE_MANUAL_ADJUST);
        record.setDrAmount(isIncrease ? appliedChange : appliedChange.negate());
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(newBalance);
        record.setDescription(StringUtils.isNotBlank(remark)
            ? remark.trim()
            : (isIncrease ? "手动调增DR余额" : "手动调减DR余额"));
        record.setStatus(1);
        record.setCreateBy(operatorId == null ? "system" : operatorId.toString());
        record.setCreateTime(LocalDateTime.now());
        record.setConsumer(resolveConsumerUsername(operatorId, userId));
        DrBillingRecord persistedRecord = billingRecordService.createRecord(record);

        UserDrBalance updatedBalance = balanceMapper.selectByUserId(userId);
        return new DrBalanceAdjustResult(updatedBalance, persistedRecord, appliedChange);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean preDeductForInstance(Long userId, BigDecimal amount, Long operatorId) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("用户ID和预扣费金额不能为空且必须大于0");
        }

        UserDrBalance balance = getByUserId(userId);
        if (balance == null) {
            throw new RuntimeException("用户余额账户不存在");
        }

        if (!balance.isNormal()) {
            throw new RuntimeException("用户余额账户状态异常，无法预扣费");
        }

        // 预扣费操作需要检查基本余额是否充足，防止产生负数余额
        if (!balance.isBaseBalanceSufficient(amount)) {
            return false;
        }

        BigDecimal balanceBefore = balance.getDrBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        BigDecimal preDeductedBefore = balance.getPreDeductedBalance();
        BigDecimal preDeductedAfter = preDeductedBefore.add(amount);

        int result = balanceMapper.updateBalanceWithVersion(
            userId,
            balanceAfter,
            preDeductedAfter,
            balance.getTotalRecharge(),
            balance.getTotalConsume(),
            balance.getTotalRefund(),
            balance.getFrozenAmount(),
            balance.getVersion()
        );

        if (result == 0) {
            throw new RuntimeException("预扣费失败，请重试");
        }

        // 创建预扣费账单记录
        DrBillingRecord billingRecord = DrBillingRecord.createPreDeductRecord(
            userId, operatorId, amount, balanceBefore, balanceAfter,
            preDeductedBefore, preDeductedAfter
        );
        billingRecord.setBillNo(generateBillNo());
        billingRecord.setCreateBy(operatorId.toString());
        billingRecord.setCreateTime(LocalDateTime.now());
        billingRecord.setConsumer(resolveConsumerUsername(operatorId, userId));
        billingRecordService.createRecord(billingRecord);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean consume(Long userId, BigDecimal amount, String businessType, Long businessId,
                          Integer billingType, Long operatorId, String description) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("用户ID和消费金额不能为空且必须大于0");
        }

        UserDrBalance balance = getByUserId(userId);
        if (balance == null) {
            throw new RuntimeException("用户余额账户不存在");
        }

        if (!balance.isNormal()) {
            throw new RuntimeException("用户余额账户状态异常，无法消费");
        }

        // 检查总可用余额（预扣费余额 + 基本余额），允许余额为负数但记录警告
        boolean isTotalBalanceSufficient = balance.isTotalBalanceSufficient(amount);
        if (!isTotalBalanceSufficient) {
            log.warn("总余额不足但仍执行消费，用户ID：{}，总余额：{}，需要扣除：{}",
                userId, balance.getTotalAvailableBalance(), amount);
        }

        BigDecimal preDeductedBalance = balance.getPreDeductedBalance();
        BigDecimal consumeFromPreDeducted = BigDecimal.ZERO;
        BigDecimal consumeFromBaseBalance = amount;

        // 优先使用预扣费余额
        if (preDeductedBalance.compareTo(BigDecimal.ZERO) > 0) {
            if (preDeductedBalance.compareTo(amount) >= 0) {
                // 预扣费余额足够
                consumeFromPreDeducted = amount;
                consumeFromBaseBalance = BigDecimal.ZERO;
            } else {
                // 预扣费余额不足，需要从基本余额中扣除差额
                consumeFromPreDeducted = preDeductedBalance;
                consumeFromBaseBalance = amount.subtract(preDeductedBalance);
            }
        }

        BigDecimal newPreDeductedBalance = preDeductedBalance.subtract(consumeFromPreDeducted);
        BigDecimal newDrBalance = balance.getDrBalance().subtract(consumeFromBaseBalance);
        BigDecimal newTotalConsume = balance.getTotalConsume().add(amount);

        int result = balanceMapper.updateBalanceWithVersion(
            userId,
            newDrBalance,
            newPreDeductedBalance,
            balance.getTotalRecharge(),
            newTotalConsume,
            balance.getTotalRefund(),
            balance.getFrozenAmount(),
            balance.getVersion()
        );

        if (result == 0) {
            throw new RuntimeException("消费失败，请重试");
        }

        // 创建消费账单记录
        DrBillingRecord billingRecord = DrBillingRecord.createConsumeRecord(
            userId, operatorId, businessType, businessId, amount, billingType,
            balance.getDrBalance(), newDrBalance, description
        );
        billingRecord.setBillNo(generateBillNo());
        billingRecord.setCreateBy(operatorId.toString());
        billingRecord.setCreateTime(LocalDateTime.now());
        billingRecord.setConsumer(resolveConsumerUsername(operatorId, userId));
        billingRecordService.createRecord(billingRecord);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(Long userId, BigDecimal amount, String businessType, Long businessId,
                         Long operatorId, String description) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("用户ID和退款金额不能为空且必须大于0");
        }

        UserDrBalance balance = getByUserId(userId);
        if (balance == null) {
            throw new RuntimeException("用户余额账户不存在");
        }

        if (!balance.isNormal()) {
            throw new RuntimeException("用户余额账户状态异常，无法退款");
        }

        BigDecimal balanceBefore = balance.getDrBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        BigDecimal totalRefundAfter = balance.getTotalRefund().add(amount);

        int result = balanceMapper.updateBalanceWithVersion(
            userId,
            balanceAfter,
            balance.getPreDeductedBalance(),
            balance.getTotalRecharge(),
            balance.getTotalConsume(),
            totalRefundAfter,
            balance.getFrozenAmount(),
            balance.getVersion()
        );

        if (result == 0) {
            throw new RuntimeException("退款失败，请重试");
        }

        // 创建退款账单记录
        DrBillingRecord billingRecord = new DrBillingRecord();
        billingRecord.setUserId(userId);
        billingRecord.setOperatorId(operatorId);
        billingRecord.setBillType(3); // 退款
        billingRecord.setBusinessType(businessType);
        billingRecord.setBusinessId(businessId);
        billingRecord.setDrAmount(amount);
        billingRecord.setBalanceBefore(balanceBefore);
        billingRecord.setBalanceAfter(balanceAfter);
        billingRecord.setDescription(description);
        billingRecord.setStatus(1); // 成功
        billingRecord.setBillNo(generateBillNo());
        billingRecord.setCreateBy(operatorId.toString());
        billingRecord.setCreateTime(LocalDateTime.now());
        billingRecord.setConsumer(resolveConsumerUsername(operatorId, userId));
        billingRecordService.createRecord(billingRecord);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int getAvailableMarketingInstanceCount(Long userId) {
        UserDrBalance balance = getByUserId(userId);
        if (balance == null || !balance.isNormal()) {
            return 0;
        }

        // 可创建实例数量 = 总可用余额 / 100
        BigDecimal totalAvailableBalance = balance.getTotalAvailableBalance();
        return totalAvailableBalance.divide(INSTANCE_CREATION_RATIO, 0, BigDecimal.ROUND_DOWN).intValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freezeBalance(Long userId, BigDecimal amount, Long operatorId) {
        UserDrBalance balance = getByUserId(userId);
        if (balance == null || !balance.isNormal()) {
            return false;
        }

        if (!balance.isBalanceSufficient(amount)) {
            return false;
        }

        BigDecimal newFrozenAmount = balance.getFrozenAmount().add(amount);

        int result = balanceMapper.updateBalanceWithVersion(
            userId,
            balance.getDrBalance(),
            balance.getPreDeductedBalance(),
            balance.getTotalRecharge(),
            balance.getTotalConsume(),
            balance.getTotalRefund(),
            newFrozenAmount,
            balance.getVersion()
        );

        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfreezeBalance(Long userId, BigDecimal amount, Long operatorId) {
        UserDrBalance balance = getByUserId(userId);
        if (balance == null) {
            return false;
        }

        BigDecimal frozenAmount = balance.getFrozenAmount();
        if (frozenAmount.compareTo(amount) < 0) {
            return false;
        }

        BigDecimal newFrozenAmount = frozenAmount.subtract(amount);

        int result = balanceMapper.updateBalanceWithVersion(
            userId,
            balance.getDrBalance(),
            balance.getPreDeductedBalance(),
            balance.getTotalRecharge(),
            balance.getTotalConsume(),
            balance.getTotalRefund(),
            newFrozenAmount,
            balance.getVersion()
        );

        return result > 0;
    }

    @Override
    public boolean checkBalanceSufficient(Long userId, BigDecimal amount) {
        UserDrBalance balance = getByUserId(userId);
        return balance != null && balance.isTotalBalanceSufficient(amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryConsumeAiCharacterFreeTimes(Long userId) {
        if (userId == null) {
            return false;
        }
        getByUserId(userId);
        return balanceMapper.consumeAiCharacterFreeTimes(userId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDrBalance createBalanceAccount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        // 检查是否已存在
        UserDrBalance existing = balanceMapper.selectByUserId(userId);
        if (existing != null) {
            return existing;
        }

        UserDrBalance balance = UserDrBalance.createForUser(userId);
        balance.setCreateBy("system");
        balance.setCreateTime(LocalDateTime.now());

        int result = balanceMapper.insert(balance);
        if (result <= 0) {
            throw new RuntimeException("创建用户余额账户失败");
        }

        return balance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBalanceStatus(Long userId, String status, Long operatorId) {
        if (userId == null || status == null) {
            return false;
        }

        int result = balanceMapper.updateStatus(userId, status, operatorId.toString());
        return result > 0;
    }

    @Override
    public Map<String, Object> getBalanceStatistics(Long userId) {
        Map<String, Object> statistics = balanceMapper.selectBalanceStatistics(userId);
        if (statistics == null) {
            return new HashMap<>();
        }
        return statistics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeductResponse deductWithDetails(DrBillingRecord record, Long originalUserId) {
        try {
            // 1. 获取用户余额信息
            UserDrBalance userBalance = balanceMapper.selectByUserId(record.getUserId());
            if (userBalance == null) {
                return DeductResponse.error("用户余额账户不存在");
            }

            // 2. 检查总可用余额（预扣费余额 + 基本余额），允许余额为负数但记录警告
            boolean isTotalBalanceSufficient = userBalance.isTotalBalanceSufficient(record.getDrAmount());
            if (!isTotalBalanceSufficient) {
                log.warn("总余额不足但仍执行扣费，用户ID：{}，总余额：{}，需要扣除：{}",
                    record.getUserId(), userBalance.getTotalAvailableBalance(), record.getDrAmount());
            }

            // 3. 生成账单编号
            record.setBillNo(generateBillNo());
            record.setConsumer(resolveConsumerUsername(record.getOperatorId(), originalUserId));

            // 4. 计算扣费策略：优先使用预扣费余额
            BigDecimal amount = record.getDrAmount();
            BigDecimal preDeductedBalance = userBalance.getPreDeductedBalance();
            BigDecimal baseBalanceBefore = userBalance.getDrBalance();
            BigDecimal totalBalanceBefore = baseBalanceBefore.add(preDeductedBalance);

            BigDecimal deductFromPreDeducted = BigDecimal.ZERO;
            BigDecimal deductFromBaseBalance = amount;

            // 优先使用预扣费余额
            if (preDeductedBalance.compareTo(BigDecimal.ZERO) > 0) {
                if (preDeductedBalance.compareTo(amount) >= 0) {
                    // 预扣费余额足够
                    deductFromPreDeducted = amount;
                    deductFromBaseBalance = BigDecimal.ZERO;
                } else {
                    // 预扣费余额不足，用完预扣费余额，剩余部分扣基本余额
                    deductFromPreDeducted = preDeductedBalance;
                    deductFromBaseBalance = amount.subtract(preDeductedBalance);
                }
            }

            // 5. 计算扣费后的余额
            BigDecimal newPreDeductedBalance = preDeductedBalance.subtract(deductFromPreDeducted);
            BigDecimal newBaseBalance = baseBalanceBefore.subtract(deductFromBaseBalance);
            BigDecimal newTotalConsume = userBalance.getTotalConsume().add(amount);

            // 6. 更新余额信息
            userBalance.setDrBalance(newBaseBalance);
            userBalance.setPreDeductedBalance(newPreDeductedBalance);
            userBalance.setTotalConsume(newTotalConsume);

            // 7. 执行数据库操作（使用通用的余额更新方法）
            int updateResult = balanceMapper.updateBalanceWithVersion(
                record.getUserId(),
                newBaseBalance,
                newPreDeductedBalance,
                userBalance.getTotalRecharge(),
                newTotalConsume,
                userBalance.getTotalRefund(),
                userBalance.getFrozenAmount(),
                userBalance.getVersion()
            );

            if (updateResult <= 0) {
                return DeductResponse.error("扣费失败，请稍后重试");
            }

            // 8. 设置账单记录的余额信息（记录基本余额的变化）
            record.setBalanceBefore(totalBalanceBefore);
            record.setBalanceAfter(newBaseBalance.add(newPreDeductedBalance));

            // 8. 创建账单记录
            DrBillingRecord createdRecord = billingRecordService.createRecord(record);
            if (createdRecord == null) {
                // 如果账单记录创建失败，记录日志但不回滚余额（因为余额已经扣除）
                log.error("账单记录创建失败，但余额已扣除：用户ID={}, 扣费金额={}, 账单编号={}",
                    record.getUserId(), record.getDrAmount(), record.getBillNo());
            }

            // 9. 重新查询更新后的用户余额信息
            UserDrBalance updatedBalance = balanceMapper.selectByUserId(record.getUserId());

            // 10. 检查总余额状况并返回相应的提示信息
            String message = "扣费成功";
            BigDecimal totalBalanceAfter = updatedBalance.getDrBalance().add(updatedBalance.getPreDeductedBalance());
            if (!isTotalBalanceSufficient) {
                message = "扣费成功，但总余额不足，当前总余额：" + totalBalanceAfter + " DR";
            }

            return DeductResponse.success(message, updatedBalance, createdRecord,
                record.getDrAmount(), baseBalanceBefore, record.getUserId(), originalUserId);

        } catch (Exception e) {
            log.error("扣费操作异常：用户ID={}, 扣费金额={}", record.getUserId(), record.getDrAmount(), e);
            return DeductResponse.error("扣费失败：" + e.getMessage());
        }
    }

    @Override
    public String deduct(DrBillingRecord record) {
        try {
            // 1. 获取用户余额信息
            UserDrBalance userBalance = balanceMapper.selectByUserId(record.getUserId());
            if (userBalance == null) {
                return "用户余额账户不存在";
            }

            // 2. 检查总可用余额（预扣费余额 + 基本余额），允许余额为负数但记录警告
            boolean isTotalBalanceSufficient = userBalance.isTotalBalanceSufficient(record.getDrAmount());
            if (!isTotalBalanceSufficient) {
                log.warn("总余额不足但仍执行扣费，用户ID：{}，总余额：{}，需要扣除：{}",
                    record.getUserId(), userBalance.getTotalAvailableBalance(), record.getDrAmount());
            }

            // 3. 生成账单编号
            record.setBillNo(generateBillNo());

            // 4. 计算扣费策略：优先使用预扣费余额
            BigDecimal amount = record.getDrAmount();
            BigDecimal preDeductedBalance = userBalance.getPreDeductedBalance();
            BigDecimal baseBalanceBefore = userBalance.getDrBalance();
            BigDecimal totalBalanceBefore = baseBalanceBefore.add(preDeductedBalance);

            BigDecimal deductFromPreDeducted = BigDecimal.ZERO;
            BigDecimal deductFromBaseBalance = amount;

            // 优先使用预扣费余额
            if (preDeductedBalance.compareTo(BigDecimal.ZERO) > 0) {
                if (preDeductedBalance.compareTo(amount) >= 0) {
                    // 预扣费余额足够
                    deductFromPreDeducted = amount;
                    deductFromBaseBalance = BigDecimal.ZERO;
                } else {
                    // 预扣费余额不足，用完预扣费余额，剩余部分扣基本余额
                    deductFromPreDeducted = preDeductedBalance;
                    deductFromBaseBalance = amount.subtract(preDeductedBalance);
                }
            }

            // 5. 计算扣费后的余额
            BigDecimal newPreDeductedBalance = preDeductedBalance.subtract(deductFromPreDeducted);
            BigDecimal newBaseBalance = baseBalanceBefore.subtract(deductFromBaseBalance);
            BigDecimal newTotalConsume = userBalance.getTotalConsume().add(amount);

            // 6. 更新余额信息
            userBalance.setDrBalance(newBaseBalance);
            userBalance.setPreDeductedBalance(newPreDeductedBalance);
            userBalance.setTotalConsume(newTotalConsume);

            // 7. 执行数据库操作（使用通用的余额更新方法）
            int updateResult = balanceMapper.updateBalanceWithVersion(
                record.getUserId(),
                newBaseBalance,
                newPreDeductedBalance,
                userBalance.getTotalRecharge(),
                newTotalConsume,
                userBalance.getTotalRefund(),
                userBalance.getFrozenAmount(),
                userBalance.getVersion()
            );

            if (updateResult <= 0) {
                return "扣费失败，请稍后重试";
            }

            // 8. 设置账单记录的余额信息（记录基本余额的变化）
            record.setBalanceBefore(totalBalanceBefore);
            record.setBalanceAfter(newBaseBalance.add(newPreDeductedBalance));

            // 8. 创建账单记录
            DrBillingRecord createdRecord = billingRecordService.createRecord(record);
            if (createdRecord == null) {
                // 如果账单记录创建失败，记录日志但不回滚余额（因为余额已经扣除）
                log.error("账单记录创建失败，但余额已扣除：用户ID={}, 扣费金额={}, 账单编号={}",
                    record.getUserId(), record.getDrAmount(), record.getBillNo());
            }

            // 根据总余额情况返回不同的提示信息
            BigDecimal totalBalanceAfter = newBaseBalance.add(newPreDeductedBalance);
            return isTotalBalanceSufficient ? "扣费成功" : "扣费成功，但总余额不足，当前总余额：" + totalBalanceAfter + " DR";

        } catch (Exception e) {
            log.error("扣费操作异常：用户ID={}, 扣费金额={}", record.getUserId(), record.getDrAmount(), e);
            return "扣费失败：" + e.getMessage();
        }
    }

    /**
     * 生成账单编号
     */
    private String generateBillNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomNum = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        return "DR" + timestamp + randomNum;
    }

    private String resolveConsumerUsername(Long operatorId, Long fallbackUserId) {
        Long targetUserId = operatorId != null ? operatorId : fallbackUserId;
        if (targetUserId == null) {
            return null;
        }
        try {
            SysUser operator = sysUserService.selectUserById(targetUserId);
            if (operator != null) {
                if (operator.getUsername() != null && !operator.getUsername().isEmpty()) {
                    return operator.getUsername();
                }
                if (operator.getNickname() != null && !operator.getNickname().isEmpty()) {
                    return operator.getNickname();
                }
            }
        } catch (Exception e) {
            log.warn("获取用户用户名失败：userId={}", targetUserId, e);
        }
        return null;
    }
}
