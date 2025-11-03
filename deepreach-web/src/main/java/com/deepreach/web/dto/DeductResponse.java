package com.deepreach.web.dto;

import com.deepreach.web.entity.UserDrBalance;
import com.deepreach.web.entity.DrBillingRecord;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DR积分扣费响应对象
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Data
public class DeductResponse {

    /**
     * 扣费是否成功
     */
    private boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 扣费后的用户余额信息（包含所有字段）
     */
    private UserDrBalance userBalance;

    /**
     * 扣费记录信息
     */
    private DrBillingRecord billingRecord;

    /**
     * 扣费金额
     */
    private BigDecimal deductAmount;

    /**
     * 扣费前余额
     */
    private BigDecimal balanceBefore;

    /**
     * 扣费后余额
     */
    private BigDecimal balanceAfter;

    /**
     * 可用余额（扣除冻结金额后）
     */
    private BigDecimal availableBalance;

    /**
     * 预扣费余额
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
     * 实际扣费用户ID（买家总账户ID）
     */
    private Long actualUserId;

    /**
     * 原始请求用户ID（买家子账户ID）
     */
    private Long requestUserId;

    /**
     * 操作者ID
     */
    private Long operatorId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 账单编号
     */
    private String billNo;

    /**
     * 创建成功响应
     *
     * @param message 消息
     * @param userBalance 用户余额
     * @param billingRecord 账单记录
     * @param deductAmount 扣费金额
     * @param balanceBefore 扣费前余额
     * @param actualUserId 实际扣费用户ID
     * @param requestUserId 请求用户ID
     * @return 响应对象
     */
    public static DeductResponse success(String message, UserDrBalance userBalance,
                                         DrBillingRecord billingRecord, BigDecimal deductAmount,
                                         BigDecimal balanceBefore, Long actualUserId, Long requestUserId) {
        DeductResponse response = new DeductResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setUserBalance(userBalance);
        response.setBillingRecord(billingRecord);
        response.setDeductAmount(deductAmount);
        response.setBalanceBefore(balanceBefore);
        response.setBalanceAfter(userBalance.getDrBalance());
        response.setAvailableBalance(userBalance.getAvailableBalance());
        response.setPreDeductedBalance(userBalance.getPreDeductedBalance());
        response.setTotalRecharge(userBalance.getTotalRecharge());
        response.setTotalConsume(userBalance.getTotalConsume());
        response.setTotalRefund(userBalance.getTotalRefund());
        response.setFrozenAmount(userBalance.getFrozenAmount());
        response.setActualUserId(actualUserId);
        response.setRequestUserId(requestUserId);
        response.setOperatorId(billingRecord != null ? billingRecord.getOperatorId() : null);
        response.setBusinessType(billingRecord != null ? billingRecord.getBusinessType() : null);
        response.setBillNo(billingRecord != null ? billingRecord.getBillNo() : null);
        return response;
    }

    /**
     * 创建失败响应
     *
     * @param message 错误消息
     * @return 响应对象
     */
    public static DeductResponse error(String message) {
        DeductResponse response = new DeductResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}