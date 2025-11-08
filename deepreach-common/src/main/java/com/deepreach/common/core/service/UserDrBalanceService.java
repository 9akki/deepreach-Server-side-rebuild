package com.deepreach.common.core.service;

import com.deepreach.common.core.domain.entity.UserDrBalance;
import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.dto.DeductResponse;
import com.deepreach.common.core.dto.DrBalanceAdjustResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 用户DR积分余额服务接口
 *
 * @author DeepReach Team
 * @version 1.0
 */
public interface UserDrBalanceService {

    /**
     * 根据用户ID获取余额信息
     *
     * @param userId 用户ID
     * @return 余额信息
     */
    UserDrBalance getByUserId(Long userId);

    /**
     * 分页查询余额列表
     *
     * @param balance 查询条件
     * @return 余额列表
     */
    List<UserDrBalance> selectBalancePage(UserDrBalance balance);

    /**
     * DR积分充值
     *
     * 使用事务管理确保数据一致性
     *
     * @param userId 用户ID
     * @param amount 充值金额
     * @param operatorId 操作者ID
     * @return 充值后的余额信息
     */
    @Transactional(rollbackFor = Exception.class)
    com.deepreach.common.core.dto.RechargeResult recharge(DrBillingRecord record, Long operatorId);

    /**
     * 营销实例创建预扣费
     *
     * 使用事务管理确保数据一致性
     *
     * @param userId 用户ID
     * @param amount 预扣费金额
     * @param operatorId 操作者ID
     * @return 是否预扣费成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean preDeductForInstance(Long userId, BigDecimal amount, Long operatorId);

    /**
     * 消费DR积分（优先使用预扣费余额）
     *
     * 使用事务管理确保数据一致性
     *
     * @param userId 用户ID
     * @param amount 消费金额
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param billingType 结算类型
     * @param operatorId 操作者ID
     * @param description 描述
     * @return 是否消费成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean consume(Long userId, BigDecimal amount, String businessType, Long businessId,
                   Integer billingType, Long operatorId, String description);

    /**
     * 退款DR积分
     *
     * 使用事务管理确保数据一致性
     *
     * @param userId 用户ID
     * @param amount 退款金额
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param operatorId 操作者ID
     * @param description 描述
     * @return 是否退款成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean refund(Long userId, BigDecimal amount, String businessType, Long businessId,
                  Long operatorId, String description);

    /**
     * 获取用户可创建的营销实例数量
     *
     * @param userId 用户ID
     * @return 可创建的营销实例数量
     */
    int getAvailableMarketingInstanceCount(Long userId);

    /**
     * 冻结用户余额
     *
     * 使用事务管理确保数据一致性
     *
     * @param userId 用户ID
     * @param amount 冻结金额
     * @param operatorId 操作者ID
     * @return 是否冻结成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean freezeBalance(Long userId, BigDecimal amount, Long operatorId);

    /**
     * 解冻用户余额
     *
     * 使用事务管理确保数据一致性
     *
     * @param userId 用户ID
     * @param amount 解冻金额
     * @param operatorId 操作者ID
     * @return 是否解冻成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean unfreezeBalance(Long userId, BigDecimal amount, Long operatorId);

    /**
     * 检查用户余额是否充足
     *
     * @param userId 用户ID
     * @param amount 需要的金额
     * @return 是否充足
     */
    boolean checkBalanceSufficient(Long userId, BigDecimal amount);

    /**
     * 尝试消耗一次AI人设免费创建次数
     *
     * @param userId 商家总账号ID
     * @return true表示成功消耗，false表示无可用次数
     */
    boolean tryConsumeAiCharacterFreeTimes(Long userId);

    /**
     * 创建用户余额账户
     *
     * @param userId 用户ID
     * @return 创建的余额账户
     */
    UserDrBalance createBalanceAccount(Long userId);

    /**
     * 更新用户余额状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @param operatorId 操作者ID
     * @return 是否更新成功
     */
    boolean updateBalanceStatus(Long userId, String status, Long operatorId);

    /**
     * 获取用户余额统计信息
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getBalanceStatistics(Long userId);

    /**
     * DR积分扣费（返回详细信息）
     *
     * 使用事务管理确保数据一致性
     *
     * @param record 扣费记录
     * @param originalUserId 原始请求用户ID（员工）
     * @return 扣费响应信息
     */
    @Transactional(rollbackFor = Exception.class)
    DeductResponse deductWithDetails(DrBillingRecord record, Long originalUserId);

    /**
     * DR每日汇总扣费：实时扣余额，账单改为日结
     */
    @Transactional(rollbackFor = Exception.class)
    DeductResponse deductWithDailyAggregation(DrBillingRecord record, Long originalUserId);

    /**
     * DR积分扣费（兼容旧版本）
     *
     * 使用事务管理确保数据一致性
     *
     * @param record 扣费记录
     * @return 扣费结果消息
     */
    @Transactional(rollbackFor = Exception.class)
    String deduct(DrBillingRecord record);

    /**
     * 商家DR余额手动调账（正数调增，负数调减）
     *
     * @param userId 用户ID
     * @param amount 调账金额
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 调账结果
     */
    @Transactional(rollbackFor = Exception.class)
    DrBalanceAdjustResult manualAdjustBalance(Long userId, BigDecimal amount, Long operatorId, String remark);

    /**
     * 查询存在日累计消费的账户
     */
    List<UserDrBalance> listUsersWithDailyConsume();

    /**
     * 扣减指定用户的日累计消费金额
     */
    boolean subtractDailyConsume(Long userId, BigDecimal amount);
}
