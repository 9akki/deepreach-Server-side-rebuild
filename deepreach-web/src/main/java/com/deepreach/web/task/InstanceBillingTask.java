package com.deepreach.web.task;

import com.deepreach.common.core.domain.entity.Instance;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.domain.entity.DrPriceConfig;
import com.deepreach.common.core.domain.entity.UserDrBalance;
import com.deepreach.web.mapper.AiInstanceMapper;
import com.deepreach.web.service.DrBillingRecordService;
import com.deepreach.common.core.service.DrPriceConfigService;
import com.deepreach.common.core.service.UserDrBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实例计费定时任务
 *
 * 每天北京时间00:00执行，统计每个用户账号下现存的instance数量，
 * 按照价格表中的价格统一对其总账户parent_user_id用户进行积分扣除
 * 注意：即使用户积分不足也会扣费，允许余额变成负数
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstanceBillingTask {

    private final AiInstanceMapper aiInstanceMapper;
    private final DrPriceConfigService priceConfigService;
    private final UserDrBalanceService balanceService;
    private final DrBillingRecordService billingRecordService;
    private final SysUserService userService;

    /**
     * 每天北京时间00:00执行的实例计费任务
     *
     * cron表达式: 0 0 0 * * ?
     * - 秒: 0
     * - 分: 0
     * - 时: 0
     * - 日: *
     * - 月: *
     * - 周: ?
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void executeDailyInstanceBilling() {
        log.info("开始执行每日实例计费任务，执行时间：{}", LocalDateTime.now());

        try {
            // 1. 获取所有实例
            List<Instance> allInstances = aiInstanceMapper.selectAllInstances();
            log.info("查询到实例数量：{}", allInstances.size());

            if (allInstances.isEmpty()) {
                log.info("没有实例，计费任务结束");
                return;
            }

            // 2. 按父用户ID分组统计实例数量
            Map<Long, Integer> marketingInstanceCountMap = new HashMap<>();
            Map<Long, Integer> prospectingInstanceCountMap = new HashMap<>();

            for (Instance instance : allInstances) {
                try {
                    // 获取用户信息
                    SysUser user = userService.selectUserWithDept(instance.getUserId());
                    if (user == null) {
                        log.warn("用户不存在，实例ID：{}, 用户ID：{}", instance.getInstanceId(), instance.getUserId());
                        continue;
                    }

                    // 只对员工的实例进行计费
                    if (!user.isBuyerSubIdentity()) {
                        log.debug("跳过非员工的实例，实例ID：{}, 用户ID：{}", instance.getInstanceId(), instance.getUserId());
                        continue;
                    }

                    // 获取父用户ID（商家总账号）
                    Long parentUserId = user.getParentUserId();
                    if (parentUserId == null) {
                        log.warn("用户没有关联的商家总账号，实例ID：{}, 用户ID：{}", instance.getInstanceId(), instance.getUserId());
                        continue;
                    }

                    // 按实例类型统计
                    if (Integer.valueOf(0).equals(instance.getType())) {
                        // 营销实例
                        marketingInstanceCountMap.put(parentUserId,
                            marketingInstanceCountMap.getOrDefault(parentUserId, 0) + 1);
                    } else if (Integer.valueOf(1).equals(instance.getType())) {
                        // 拓客实例
                        prospectingInstanceCountMap.put(parentUserId,
                            prospectingInstanceCountMap.getOrDefault(parentUserId, 0) + 1);
                    }

                } catch (Exception e) {
                    log.error("处理实例统计时发生异常，实例ID：{}", instance.getInstanceId(), e);
                }
            }

            log.info("营销实例统计：{}", marketingInstanceCountMap);
            log.info("侦查实例统计：{}", prospectingInstanceCountMap);

            // 3. 获取价格配置
            DrPriceConfig marketingPriceConfig = priceConfigService.selectDrPriceConfigByBusinessType(
                DrPriceConfig.BUSINESS_TYPE_INSTANCE_MARKETING);
            DrPriceConfig prospectingPriceConfig = priceConfigService.selectDrPriceConfigByBusinessType(
                DrPriceConfig.BUSINESS_TYPE_INSTANCE_PROSPECTING);

            if (marketingPriceConfig == null || !marketingPriceConfig.isActive()) {
                log.warn("营销实例价格配置未启用，跳过营销实例计费");
            }
            if (prospectingPriceConfig == null || !prospectingPriceConfig.isActive()) {
                log.warn("侦查实例价格配置未启用，跳过侦查实例计费");
            }

            // 4. 执行积分扣除
            int successCount = 0;
            int failCount = 0;

            // 处理营销实例计费
            if (marketingPriceConfig != null && marketingPriceConfig.isActive()) {
                for (Map.Entry<Long, Integer> entry : marketingInstanceCountMap.entrySet()) {
                    Long parentUserId = entry.getKey();
                    Integer instanceCount = entry.getValue();

                    try {
                        BigDecimal totalAmount = marketingPriceConfig.getDrPrice().multiply(new BigDecimal(instanceCount));

                        boolean success = deductBalanceForInstances(parentUserId, totalAmount,
                            "营销实例计费", instanceCount, marketingPriceConfig);

                        if (success) {
                            successCount++;
                            log.info("营销实例计费成功，父用户ID：{}, 实例数量：{}, 扣费金额：{} DR",
                                parentUserId, instanceCount, totalAmount);
                        } else {
                            failCount++;
                            log.warn("营销实例计费失败，父用户ID：{}, 实例数量：{}", parentUserId, instanceCount);
                        }
                    } catch (Exception e) {
                        failCount++;
                        log.error("营销实例计费异常，父用户ID：{}, 实例数量：{}", parentUserId, instanceCount, e);
                    }
                }
            }

            // 处理侦查实例计费
            if (prospectingPriceConfig != null && prospectingPriceConfig.isActive()) {
                for (Map.Entry<Long, Integer> entry : prospectingInstanceCountMap.entrySet()) {
                    Long parentUserId = entry.getKey();
                    Integer instanceCount = entry.getValue();

                    try {
                        BigDecimal totalAmount = prospectingPriceConfig.getDrPrice().multiply(new BigDecimal(instanceCount));

                        boolean success = deductBalanceForInstances(parentUserId, totalAmount,
                            "侦查实例计费", instanceCount, prospectingPriceConfig);

                        if (success) {
                            successCount++;
                            log.info("侦查实例计费成功，父用户ID：{}, 实例数量：{}, 扣费金额：{} DR",
                                parentUserId, instanceCount, totalAmount);
                        } else {
                            failCount++;
                            log.warn("侦查实例计费失败，父用户ID：{}, 实例数量：{}", parentUserId, instanceCount);
                        }
                    } catch (Exception e) {
                        failCount++;
                        log.error("侦查实例计费异常，父用户ID：{}, 实例数量：{}", parentUserId, instanceCount, e);
                    }
                }
            }

            log.info("每日实例计费任务执行完成，成功：{}, 失败：{}", successCount, failCount);

        } catch (Exception e) {
            log.error("每日实例计费任务执行异常", e);
        }
    }

    /**
     * 为实例计费扣除积分（允许余额为负数）
     *
     * 使用事务管理确保数据一致性
     *
     * @param parentUserId 父用户ID（商家总账号）
     * @param totalAmount 扣费总金额
     * @param description 扣费描述
     * @param instanceCount 实例数量
     * @param priceConfig 价格配置
     * @return 是否扣费成功
     */
    @Transactional(rollbackFor = Exception.class)
    private boolean deductBalanceForInstances(Long parentUserId, BigDecimal totalAmount,
                                           String description, Integer instanceCount,
                                           DrPriceConfig priceConfig) {
        try {
            // 1. 获取当前用户余额信息
            UserDrBalance currentBalance = balanceService.getByUserId(parentUserId);
            if (currentBalance == null) {
                log.warn("用户余额账户不存在，父用户ID：{}", parentUserId);
                return false;
            }

            BigDecimal balanceBefore = currentBalance.getDrBalance();

            // 2. 创建扣费记录
            DrBillingRecord billingRecord = new DrBillingRecord();
            billingRecord.setUserId(parentUserId); // 实际扣费的是商家总账号
            billingRecord.setOperatorId(parentUserId); // 系统自动扣费，操作者设置为被扣费用户
            billingRecord.setBillType(2); // 消费类型
            billingRecord.setBillingType(priceConfig.getBillingType()); // 结算类型
            billingRecord.setBusinessType(priceConfig.getBusinessType());
            billingRecord.setDrAmount(totalAmount);
            billingRecord.setBalanceBefore(balanceBefore); // 扣费前余额
            billingRecord.setBalanceAfter(balanceBefore.subtract(totalAmount)); // 扣费后余额（可能为负数）
            billingRecord.setDescription(String.format("%s - %d个实例", description, instanceCount));
            billingRecord.setStatus(1); // 成功状态
            billingRecord.setCreateBy("system");
            billingRecord.setCreateTime(LocalDateTime.now());

            // 3. 直接调用服务层的扣除方法，不检查余额是否充足
            // 这里我们使用deductWithDetails方法，它会处理余额更新和记录创建
            var deductResponse = balanceService.deductWithDetails(billingRecord, parentUserId);

            if (deductResponse.isSuccess()) {
                log.info("实例计费扣费成功，父用户ID：{}, 金额：{} DR, 扣费前余额：{} DR, 扣费后余额：{} DR, 描述：{}",
                    parentUserId, totalAmount, balanceBefore, deductResponse.getUserBalance().getDrBalance(),
                    billingRecord.getDescription());
                return true;
            } else {
                log.error("实例计费扣费失败，父用户ID：{}, 金额：{} DR, 错误：{}",
                    parentUserId, totalAmount, deductResponse.getMessage());
                return false;
            }

        } catch (Exception e) {
            log.error("实例计费扣费异常，父用户ID：{}, 金额：{} DR", parentUserId, totalAmount, e);
            return false;
        }
    }
}
