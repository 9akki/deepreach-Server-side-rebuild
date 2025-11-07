package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 用户DR积分余额实体
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDrBalance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 余额ID
     */
    private Long balanceId;

    /**
     * 用户ID（商家总账号）
     */
    private Long userId;

    /**
     * DR积分余额
     */
    private BigDecimal drBalance;

    /**
     * 预扣费余额（营销实例创建时预扣的费用）
     */
    private BigDecimal preDeductedBalance;

    /**
     * 累计充值金额
     */
    private BigDecimal totalRecharge;

    /**
     * 累计消费金额
     */
    private BigDecimal totalConsume;

    /**
     * 累计退款金额
     */
    private BigDecimal totalRefund;

    /**
     * 冻结金额
     */
    private BigDecimal frozenAmount;

    /**
     * 版本号（乐观锁）
     */
    private Integer version;

    /**
     * 状态（0正常 1冻结 2注销）
     */
    private String status;

    /**
     * AI人设免费创建次数
     *
     * 商家总账号与其子账户共享
     */
    private Integer aiCharacterFreeTimes;

    // ==================== 业务判断方法 ====================

    /**
     * 判断余额是否充足
     *
     * @param amount 需要的金额
     * @return true如果余额充足，false否则
     */
    public boolean isBalanceSufficient(BigDecimal amount) {
        if (drBalance == null || amount == null) {
            return false;
        }
        return drBalance.compareTo(amount) >= 0;
    }

    /**
     * 获取可用余额（基本余额减去冻结金额）
     *
     * @return 可用余额
     */
    public BigDecimal getAvailableBalance() {
        if (drBalance == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal frozen = frozenAmount != null ? frozenAmount : BigDecimal.ZERO;
        return drBalance.subtract(frozen);
    }

    /**
     * 获取预扣费余额
     *
     * @return 预扣费余额
     */
    public BigDecimal getPreDeductedBalance() {
        return preDeductedBalance != null ? preDeductedBalance : BigDecimal.ZERO;
    }

    /**
     * 获取总可用余额（基本余额 + 预扣费余额 - 冻结金额）
     *
     * @return 总可用余额
     */
    public BigDecimal getTotalAvailableBalance() {
        BigDecimal available = getAvailableBalance();
        BigDecimal preDeducted = getPreDeductedBalance();
        return available.add(preDeducted);
    }

    /**
     * 检查基本余额是否充足
     *
     * @param amount 需要的金额
     * @return true如果基本余额充足，false否则
     */
    public boolean isBaseBalanceSufficient(BigDecimal amount) {
        return getAvailableBalance().compareTo(amount) >= 0;
    }

    /**
     * 检查预扣费余额是否充足
     *
     * @param amount 需要的金额
     * @return true如果预扣费余额充足，false否则
     */
    public boolean isPreDeductedBalanceSufficient(BigDecimal amount) {
        return getPreDeductedBalance().compareTo(amount) >= 0;
    }

    /**
     * 检查总可用余额是否充足
     *
     * @param amount 需要的金额
     * @return true如果总可用余额充足，false否则
     */
    public boolean isTotalBalanceSufficient(BigDecimal amount) {
        return getTotalAvailableBalance().compareTo(amount) >= 0;
    }

    /**
     * 判断是否为正常状态
     *
     * @return true如果正常，false否则
     */
    public boolean isNormal() {
        return "0".equals(status);
    }

    /**
     * 判断是否为冻结状态
     *
     * @return true如果冻结，false否则
     */
    public boolean isFrozen() {
        return "1".equals(status);
    }

    /**
     * 判断是否为注销状态
     *
     * @return true如果注销，false否则
     */
    public boolean isCancelled() {
        return "2".equals(status);
    }

    /**
     * 创建用于注册的余额对象
     *
     * @param userId 用户ID
     * @return 余额对象
     */
    public static UserDrBalance createForUser(Long userId) {
        UserDrBalance balance = new UserDrBalance();
        balance.setUserId(userId);
        balance.setDrBalance(BigDecimal.ZERO);
        balance.setPreDeductedBalance(BigDecimal.ZERO);
        balance.setTotalRecharge(BigDecimal.ZERO);
        balance.setTotalConsume(BigDecimal.ZERO);
        balance.setTotalRefund(BigDecimal.ZERO);
        balance.setFrozenAmount(BigDecimal.ZERO);
        balance.setVersion(0);
        balance.setStatus("0");
        balance.setAiCharacterFreeTimes(1);
        return balance;
    }
}
