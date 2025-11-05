package com.deepreach.web.controller;

import com.deepreach.common.annotation.Log;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.web.BaseController;
import com.deepreach.common.web.Result;
import com.deepreach.common.web.page.TableDataInfo;
import com.deepreach.common.enums.BusinessType;
import com.deepreach.web.dto.DrBalanceAdjustRequest;
import com.deepreach.web.dto.DrBalanceAdjustResult;
import com.deepreach.web.entity.UserDrBalance;
import com.deepreach.web.entity.DrBillingRecord;
import com.deepreach.web.service.UserDrBalanceService;
import com.deepreach.web.service.DrBillingRecordService;
import com.deepreach.web.dto.DeductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DR积分管理控制器
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/dr")
@RequiredArgsConstructor
public class DrBalanceController extends BaseController {

    @Autowired
    private SysUserService userService;


    private final UserDrBalanceService balanceService;
    private final DrBillingRecordService billingRecordService;

    /**
     * 查询DR积分余额列表
     */
    // @PreAuthorize("@ss.hasPermi('system:dr:list')")
    @GetMapping("/balance/list")
    public TableDataInfo<UserDrBalance> balanceList(UserDrBalance balance) {
        startPage();
        List<UserDrBalance> list = balanceService.selectBalancePage(balance);
        return getDataTable(list);
    }

    /**
     * DR积分手动调账（商家调账，正数调增、负数调减）
     *
     * @param request 调账请求
     */
    @Log(title = "DR余额调账", businessType = BusinessType.UPDATE)
    @PostMapping("/balance/manual-adjust")
    public Result<DrBalanceAdjustResult> manualAdjust(@Validated @RequestBody DrBalanceAdjustRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return error("调账金额不能为空且不能为0");
        }
        if (!isBuyerMainAccount(request.getUserId())) {
            return error("只能为买家总账户类型的用户调账");
        }

        DrBalanceAdjustResult result = balanceService.manualAdjustBalance(
            request.getUserId(),
            request.getAmount(),
            getCurrentUserId(),
            request.getRemark()
        );
        return success("调账成功", result);
    }

    /**
     * 获取DR积分余额详情
     */
    // @PreAuthorize("@ss.hasPermi('system:dr:query')")
    @GetMapping("/balance/{userId}")
    public Result<UserDrBalance> getBalance(@PathVariable Long userId) {
        UserDrBalance balance = balanceService.getByUserId(userId);
        return success(balance);
    }

    /**
     * DR积分充值
     */
    // @PreAuthorize("@ss.hasPermi('system:dr:recharge')")
    @Log(title = "DR积分充值", businessType = BusinessType.UPDATE)
    @PostMapping("/balance/recharge")
    public Result<com.deepreach.web.dto.RechargeResult> recharge(@Validated @RequestBody DrBillingRecord request) {
        // 验证用户是否为买家总账户类型
        if (request.getUserId() == null || request.getDrAmount() == null
                || request.getDrAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return error("用户ID和充值金额不能为空且必须大于0");
        }
        if (!isBuyerMainAccount(request.getUserId())) {
            return error("只能为买家总账户类型的用户充值");
        }

        var result = balanceService.recharge(request, getCurrentUserId());

        return success("充值成功", result);
    }
    /**
     * DR积分扣费
     *
     * 支持买家子账户扣费：从买家子账户的父账户（买家总账户）中扣除费用
     * 返回详细的扣费结果和用户余额信息
     */
    @Log(title = "DR积分扣费", businessType = BusinessType.UPDATE)
    @PostMapping("/balance/deduct")
    public Result<DeductResponse> deduct(@Validated @RequestBody DrBillingRecord record) {
        try {
            // 1. 参数校验
            if (record.getUserId() == null) {
                return Result.error(DeductResponse.error("用户ID不能为空").getMessage());
            }
            if (record.getDrAmount() == null || record.getDrAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(DeductResponse.error("扣费金额必须大于0").getMessage());
            }

            // 2. 获取用户信息并判断是否为买家子账户用户
            SysUser user = userService.selectUserWithDept(record.getUserId());
            if (user == null) {
                return Result.error(DeductResponse.error("用户不存在").getMessage());
            }

            // 检查是否为买家子账户（部门类型为4）
            if (!user.isBuyerSubIdentity()) {
                return Result.error(DeductResponse.error("只能从买家子账户用户进行扣费").getMessage());
            }

            // 3. 获取父用户ID（买家总账户）
            Long parentUserId = user.getParentUserId();
            if (parentUserId == null) {
                return Result.error(DeductResponse.error("用户没有关联的买家总账户").getMessage());
            }

            // 4. 验证父用户是否为买家总账户类型
            SysUser parentUser = userService.selectUserWithDept(parentUserId);
            if (parentUser == null) {
                return Result.error(DeductResponse.error("关联的买家总账户用户不存在").getMessage());
            }

            if (!parentUser.isBuyerMainIdentity()) {
                return Result.error(DeductResponse.error("关联的父用户不是买家总账户类型").getMessage());
            }

            // 5. 检查买家总账户余额是否充足
            UserDrBalance parentBalance = balanceService.getByUserId(parentUserId);
            if (parentBalance == null) {
                return Result.error(DeductResponse.error("买家总账户余额账户不存在").getMessage());
            }

            // 6. 买家总账户就余额不足也要扣成负数
            if (parentBalance.getDrBalance().compareTo(record.getDrAmount()) < 0) {
                return Result.error(DeductResponse.error("买家总账户余额不足，当前余额：" + parentBalance.getDrBalance()).getMessage());
            }

            // 6. 设置扣费记录的相关信息
            record.setUserId(parentUserId); // 实际扣费的是买家总账户
            record.setOperatorId(record.getUserId()); // 由于是公开接口，设置为被扣费用户自身
            record.setBillType(2); // 消费类型
            record.setBillingType(1); // 秒结秒扣
            record.setBusinessType(record.getBusinessType() != null ? record.getBusinessType() : "CONSUME");
            record.setDescription(record.getDescription() != null ?
                record.getDescription() :
                "买家子账户[" + user.getUsername() + "]消费扣费");

            // 7. 执行扣费操作并获取详细信息
            DeductResponse result = balanceService.deductWithDetails(record, record.getUserId());

            if (result.isSuccess()) {
                return Result.success("扣费成功，已从买家总账户扣除", result);
            } else {
                return Result.error(result.getMessage());
            }

        } catch (Exception e) {
            log.error("扣费操作失败：用户ID={}, 扣费金额={}", record.getUserId(), record.getDrAmount(), e);
            return Result.error("扣费操作异常：" + e.getMessage());
        }
    }

    /**
     * 查询DR账单记录列表
     */
    // @PreAuthorize("@ss.hasPermi('system:dr:bill:list')")
    @PostMapping("/bill/list")
    public TableDataInfo<DrBillingRecord> billingList(@RequestBody(required = false) DrBillingRecord record) {
        startPage();
        DrBillingRecord query = record != null ? record : new DrBillingRecord();
        List<DrBillingRecord> list = billingRecordService.selectRecordPage(query);
        return getDataTable(list);
    }

    /**
     * 获取充值订单列表（仅充值类型）
     */
    
    @GetMapping("/bill/list")
    public TableDataInfo<DrBillingRecord> billingListGet(DrBillingRecord record) {
        startPage();
        List<DrBillingRecord> list = billingRecordService.selectRecordPage(record);
        return getDataTable(list);
    }

    @PostMapping("/bill/recharge")
    public TableDataInfo<DrBillingRecord> rechargeOrders(
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestBody(required = false) DrBillingRecord record) {
        startPage();
        DrBillingRecord query = record != null ? record : new DrBillingRecord();
        query.setBillType(1);
        query.setBusinessType(DrBillingRecord.BUSINESS_TYPE_RECHARGE);
        Long effectiveUserId = resolveUserId(userId, record);
        if (effectiveUserId != null) {
            query.setUserId(effectiveUserId);
        }
        List<DrBillingRecord> list = billingRecordService.selectRechargeOrders(query);
        return getDataTable(list);
    }

    @GetMapping("/bill/recharge")
    public TableDataInfo<DrBillingRecord> rechargeOrdersGet(
        @RequestParam(value = "userId", required = false) Long userId,
        DrBillingRecord record) {
        startPage();
        DrBillingRecord query = record != null ? record : new DrBillingRecord();
        query.setBillType(1);
        query.setBusinessType(DrBillingRecord.BUSINESS_TYPE_RECHARGE);
        Long effectiveUserId = resolveUserId(userId, record);
        if (effectiveUserId != null) {
            query.setUserId(effectiveUserId);
        }
        List<DrBillingRecord> list = billingRecordService.selectRechargeOrders(query);
        return getDataTable(list);
    }

    private Long resolveUserId(Long userIdParam, DrBillingRecord record) {
        if (userIdParam != null && userIdParam > 0) {
            return userIdParam;
        }
        if (record != null && record.getUserId() != null && record.getUserId() > 0) {
            return record.getUserId();
        }
        if (record != null && record.getParams() != null) {
            Object value = record.getParams().get("userId");
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                try {
                    return Long.parseLong(((String) value).trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * 获取DR账单详情
     */
    // @PreAuthorize("@ss.hasPermi('system:dr:bill:query')")
    @GetMapping("/bill/{billId}")
    public Result<DrBillingRecord> getBillingRecord(@PathVariable Long billId) {
        DrBillingRecord record = billingRecordService.getById(billId);
        return success(record);
    }

    /**
     * 获取用户账单统计
     */
    // @PreAuthorize("@ss.hasPermi('system:dr:statistics')")
    @GetMapping("/bill/statistics/{userId}")
    public Result<Map<String, Object>> getStatistics(@PathVariable Long userId,
                                                     @RequestParam(required = false) String startDate,
                                                     @RequestParam(required = false) String endDate) {
        Map<String, Object> statistics = billingRecordService.getUserBillStatistics(userId, startDate, endDate);
        return success(statistics);
    }

    /**
     * 获取用户可创建营销实例数量
     */
    // @PreAuthorize("@ss.hasPermi('system:dr:query')")
    @GetMapping("/available-instances/{userId}")
    public Result<Integer> getAvailableInstances(@PathVariable Long userId) {
        int count = balanceService.getAvailableMarketingInstanceCount(userId);
        return success("可创建营销实例数量", count);
    }

    /**
     * 营销实例创建预扣费
     */
    // @PreAuthorize("@ss.hasPermi('system:dr:preDeduct')")
    @Log(title = "营销实例预扣费", businessType = BusinessType.UPDATE)
    @PostMapping("/pre-deduct")
    public Result<Object> preDeductForInstance(@Validated @RequestBody PreDeductRequest request) {
        // 验证用户是否为买家总账户类型
        if (!isBuyerMainAccount(request.getUserId())) {
            return error("只能为买家总账户类型的用户预扣费");
        }

        boolean result = balanceService.preDeductForInstance(
            request.getUserId(),
            request.getAmount(),
            getCurrentUserId()
        );

        if (result) {
            return success("预扣费成功");
        } else {
            return error("预扣费失败，余额不足");
        }
    }

    /**
     * 预扣费请求对象
     */
    public static class PreDeductRequest {
        private Long userId;
        private BigDecimal amount;
        private Long instanceId;

        // getter和setter方法
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Long getInstanceId() { return instanceId; }
        public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    }

    /**
     * 检查用户是否为买家总账户类型
     */
    private boolean isBuyerMainAccount(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            SysUser user = userService.selectUserWithDept(userId);
            if (user == null) {
                return false;
            }

            return user.isBuyerMainIdentity();
        } catch (Exception e) {
            log.error("检查用户类型失败：用户ID={}", userId, e);
            return false;
        }
    }
}
