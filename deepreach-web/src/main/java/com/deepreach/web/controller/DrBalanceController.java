package com.deepreach.web.controller;

import com.deepreach.common.annotation.Log;
import com.deepreach.common.core.domain.BaseEntity;
import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.domain.entity.DrPriceConfig;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.domain.entity.UserDrBalance;
import com.deepreach.common.core.dto.DeductResponse;
import com.deepreach.common.core.dto.DrBalanceAdjustResult;
import com.deepreach.common.core.page.PageDomain;
import com.deepreach.common.core.page.TableSupport;
import com.deepreach.common.core.service.DrPriceConfigService;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.core.service.UserDrBalanceService;
import com.deepreach.common.enums.BusinessType;
import com.deepreach.common.utils.PageUtils;
import com.deepreach.common.web.BaseController;
import com.deepreach.common.web.Result;
import com.deepreach.common.web.page.TableDataInfo;
import com.deepreach.web.dto.AiProduceDeductRequest;
import com.deepreach.web.dto.DrBalanceAdjustRequest;
import com.deepreach.web.service.DrBillingRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
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
    private final DrPriceConfigService drPriceConfigService;

    private static final Set<String> AI_BUSINESS_TYPES = Set.of(
        "AI_PRODUCE_PICTURE",
        "AI_PRODUCE_NORMAL_VIDEO",
        "AI_PRODUCE_LONG_VIDEO"
    );

    @GetMapping("/balance/pricing-mode")
    public Result<Map<String, Object>> getPricingMode() {
        DrPriceConfig tokenConfig = drPriceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_TOKEN);
        DrPriceConfig byTimesConfig = drPriceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_BY_TIMES);
        Map<String, Object> body = Map.of(
            "mode", resolvePricingMode(tokenConfig, byTimesConfig),
            "tokenPrice", tokenConfig != null ? tokenConfig.getDrPrice() : null,
            "byTimesPrice", byTimesConfig != null ? byTimesConfig.getDrPrice() : null
        );
        return success(body);
    }

    /**
     * 查询DR积分余额列表
     */
    // @PreAuthorize("@ss.hasPermi('system:dr:list')")
    @GetMapping("/balance/list")
    public TableDataInfo<UserDrBalance> balanceList(UserDrBalance balance) {
        PageDomain pageDomain = TableSupport.buildPageRequest(balance);
        PageUtils.startPage(pageDomain);
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
            return error("只能为商家总账号类型的用户调账");
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
    public Result<com.deepreach.common.core.dto.RechargeResult> recharge(@Validated @RequestBody DrBillingRecord request) {
        // 验证用户是否为商家总账号类型
        if (request.getUserId() == null || request.getDrAmount() == null
                || request.getDrAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return error("用户ID和充值金额不能为空且必须大于0");
        }
        if (!isBuyerMainAccount(request.getUserId())) {
            return error("只能为商家总账号类型的用户充值");
        }

        var result = balanceService.recharge(request, getCurrentUserId());

        return success("充值成功", result);
    }
    /**
     * DR积分扣费
     *
     * 支持员工扣费：从员工的父账户（商家总账号）中扣除费用
     * 返回详细的扣费结果和用户余额信息
     */
    @Log(title = "DR积分扣费", businessType = BusinessType.UPDATE)
    @PostMapping("/balance/deduct")
    public Result<DeductResponse> deduct(@Validated @RequestBody DrBillingRecord record) {
        try {
            if (record.getUserId() == null) {
                return Result.error(DeductResponse.error("用户ID不能为空").getMessage());
            }

            String pricingError = applyPricingStrategy(record);
            if (pricingError != null) {
                return Result.error(pricingError);
            }

            if (record.getDrAmount() == null || record.getDrAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(DeductResponse.error("扣费金额必须大于0").getMessage());
            }

            Long requestUserId = record.getUserId();
            Long originalUserId = requestUserId;

            SysUser user = userService.selectUserWithDept(requestUserId);
            if (user == null) {
                return Result.error(DeductResponse.error("用户不存在").getMessage());
            }

            Long chargeAccountId;
            String defaultDescription;

            if (user.isBuyerMainIdentity()) {
                chargeAccountId = user.getUserId();
                defaultDescription = "商家总账号[" + user.getUsername() + "]消费扣费";
            } else if (user.isBuyerSubIdentity()) {
                Long parentUserId = user.getParentUserId();
                if (parentUserId == null) {
                    return Result.error(DeductResponse.error("用户没有关联的商家总账号").getMessage());
                }

                SysUser parentUser = userService.selectUserWithDept(parentUserId);
                if (parentUser == null || !parentUser.isBuyerMainIdentity()) {
                    return Result.error(DeductResponse.error("关联的父用户不是有效的商家总账号").getMessage());
                }

                chargeAccountId = parentUserId;
                defaultDescription = "员工[" + user.getUsername() + "]消费扣费";
            } else {
                return Result.error(DeductResponse.error("仅支持商家总账号或子账户进行扣费").getMessage());
            }

            UserDrBalance chargeAccountBalance = balanceService.getByUserId(chargeAccountId);
            if (chargeAccountBalance == null) {
                return Result.error(DeductResponse.error("商家总账号余额账户不存在").getMessage());
            }

            if (chargeAccountBalance.getDrBalance().compareTo(record.getDrAmount()) < 0) {
                return Result.error(DeductResponse.error("商家总账号余额不足，当前余额：" + chargeAccountBalance.getDrBalance()).getMessage());
            }

            record.setUserId(chargeAccountId);
            record.setOperatorId(originalUserId);
            record.setBillType(2);
            record.setBillingType(1);
            record.setBusinessType(record.getBusinessType() != null ? record.getBusinessType() : "CONSUME");
            record.setDescription(record.getDescription() != null ? record.getDescription() : defaultDescription);
            return performDeduct(record, user, originalUserId);
        } catch (Exception e) {
            log.error("扣费操作失败：用户ID={}, 扣费金额={}", record.getUserId(), record.getDrAmount(), e);
            return Result.error("扣费操作异常：" + e.getMessage());
        }
    }

    @PostMapping("/balance/deduct/ai")
    public Result<DeductResponse> deductForAiProduce(@Validated @RequestBody AiProduceDeductRequest request) {
        try {
            if (!AI_BUSINESS_TYPES.contains(request.getBusinessType())) {
                return Result.error("不支持的业务类型");
            }
            DrPriceConfig config = drPriceConfigService.selectDrPriceConfigByBusinessType(request.getBusinessType());
            if (config == null || config.getDrPrice() == null || !config.isActive()) {
                return Result.error("业务类型未配置单价或未启用");
            }
            SysUser user = userService.selectUserWithDept(request.getUserId());
            if (user == null) {
                return Result.error("用户不存在");
            }
            DrBillingRecord record = new DrBillingRecord();
            record.setUserId(request.getUserId());
            record.setOperatorId(request.getUserId());
            record.setBillType(2);
            record.setBillingType(1);
            record.setBusinessType(request.getBusinessType());
            record.setDrAmount(config.getDrPrice());
            record.setDescription(org.springframework.util.StringUtils.hasText(request.getDescription())
                ? request.getDescription()
                : ("AI生成扣费-" + request.getBusinessType()));
            record.setRemark("AiProduce");
            return performDeduct(record, user, request.getUserId());
        } catch (Exception ex) {
            log.error("AI扣费失败 userId={} businessType={}", request.getUserId(), request.getBusinessType(), ex);
            return Result.error("扣费操作异常：" + ex.getMessage());
        }
    }

    private Result<DeductResponse> performDeduct(DrBillingRecord record, SysUser user, Long originalUserId) {
        Long requestUserId = record.getUserId();
        if (user == null) {
            return Result.error(DeductResponse.error("用户不存在").getMessage());
        }

        Long chargeAccountId;
        if (user.isBuyerMainIdentity()) {
            chargeAccountId = user.getUserId();
        } else if (user.isBuyerSubIdentity()) {
            Long parentUserId = user.getParentUserId();
            if (parentUserId == null) {
                return Result.error(DeductResponse.error("用户没有关联的商家总账号").getMessage());
            }
            SysUser parentUser = userService.selectUserWithDept(parentUserId);
            if (parentUser == null || !parentUser.isBuyerMainIdentity()) {
                return Result.error(DeductResponse.error("关联的父用户不是有效的商家总账号").getMessage());
            }
            chargeAccountId = parentUserId;
        } else {
            return Result.error(DeductResponse.error("仅支持商家总账号或子账户进行扣费").getMessage());
        }

        UserDrBalance chargeAccountBalance = balanceService.getByUserId(chargeAccountId);
        if (chargeAccountBalance == null) {
            return Result.error(DeductResponse.error("商家总账号余额账户不存在").getMessage());
        }

        if (record.getDrAmount() == null || record.getDrAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("扣费金额必须大于0");
        }

        if (chargeAccountBalance.getDrBalance().compareTo(record.getDrAmount()) < 0) {
            return Result.error(DeductResponse.error("商家总账号余额不足，当前余额：" + chargeAccountBalance.getDrBalance()).getMessage());
        }

        record.setUserId(chargeAccountId);
        record.setOperatorId(originalUserId);

        DeductResponse result = balanceService.deductWithDetails(record, originalUserId);

        if (result.isSuccess()) {
            return Result.success("扣费成功", result);
        } else {
            return Result.error(result.getMessage());
        }
    }

    /**
     * 按配置决定本次扣费应使用的计费方式与金额。
     * 当 TOKEN 与 BY_TIMES 同时启用时优先走 TOKEN；TOKEN 未启用时若 BY_TIMES 启用，则按次计费，每次扣除配置单价。
     * 其余场景沿用请求中的金额/业务类型。
     */
    private String applyPricingStrategy(DrBillingRecord record) {
        DrPriceConfig tokenConfig = drPriceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_TOKEN);
        DrPriceConfig byTimesConfig = drPriceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_BY_TIMES);

        boolean tokenActive = tokenConfig != null && tokenConfig.isActive();
        boolean byTimesActive = byTimesConfig != null && byTimesConfig.isActive();

        if (tokenActive) {
            record.setBusinessType(DrBillingRecord.BUSINESS_TYPE_TOKEN);
            if (record.getDrAmount() == null || record.getDrAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return "TOKEN 计价模式下，drAmount必须为已按token折算后的正数金额";
            }
            return null;
        }

        if (byTimesActive) {
            if (byTimesConfig.getDrPrice() == null || byTimesConfig.getDrPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return "按次计价配置缺失或金额非法";
            }
            record.setBusinessType(DrBillingRecord.BUSINESS_TYPE_BY_TIMES);
            record.setDrAmount(byTimesConfig.getDrPrice());
            return null;
        }

        // 没有启用 TOKEN/BY_TIMES 时沿用请求入参
        if (record.getBusinessType() == null) {
            record.setBusinessType("CONSUME");
        }
        return null;
    }

    private String resolvePricingMode(DrPriceConfig tokenConfig, DrPriceConfig byTimesConfig) {
        boolean tokenActive = tokenConfig != null && tokenConfig.isActive();
        if (tokenActive) {
            return DrPriceConfig.BUSINESS_TYPE_TOKEN;
        }
        boolean byTimesActive = byTimesConfig != null && byTimesConfig.isActive();
        if (byTimesActive) {
            return DrPriceConfig.BUSINESS_TYPE_BY_TIMES;
        }
        return "CONSUME";
    }

    /**
     * 查询DR账单记录列表
     */
    // @PreAuthorize("@ss.hasPermi('system:dr:bill:list')")
    @PostMapping("/bill/list")
    public TableDataInfo<DrBillingRecord> billingList(@RequestBody(required = false) DrBillingRecord record) {
        DrBillingRecord query = record != null ? record : new DrBillingRecord();
        PageUtils.clearPage();
        List<DrBillingRecord> list = billingRecordService.selectRecordPage(query);
        return buildPagedResponse(list, query);
    }

    /**
     * 获取充值订单列表（仅充值类型）
     */
    
    @GetMapping("/bill/list")
    public TableDataInfo<DrBillingRecord> billingListGet(DrBillingRecord record) {
        PageUtils.clearPage();
        List<DrBillingRecord> list = billingRecordService.selectRecordPage(record);
        return buildPagedResponse(list, record);
    }

    @PostMapping("/bill/recharge")
    public TableDataInfo<DrBillingRecord> rechargeOrders(
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestBody(required = false) DrBillingRecord record) {
        DrBillingRecord query = record != null ? record : new DrBillingRecord();
        query.setBillType(1);
        query.setBusinessType(DrBillingRecord.BUSINESS_TYPE_RECHARGE);
        Long effectiveUserId = resolveUserId(userId, record);
        if (effectiveUserId != null) {
            query.setUserId(effectiveUserId);
        }
        PageUtils.clearPage();
        List<DrBillingRecord> list = billingRecordService.selectRechargeOrders(query);
        return buildPagedResponse(list, query);
    }

    @GetMapping("/bill/recharge")
    public TableDataInfo<DrBillingRecord> rechargeOrdersGet(
        @RequestParam(value = "userId", required = false) Long userId,
        DrBillingRecord record) {
        DrBillingRecord query = record != null ? record : new DrBillingRecord();
        query.setBillType(1);
        query.setBusinessType(DrBillingRecord.BUSINESS_TYPE_RECHARGE);
        Long effectiveUserId = resolveUserId(userId, record);
        if (effectiveUserId != null) {
            query.setUserId(effectiveUserId);
        }
        PageUtils.clearPage();
        List<DrBillingRecord> list = billingRecordService.selectRechargeOrders(query);
        return buildPagedResponse(list, record);
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

    private <T> TableDataInfo<T> buildPagedResponse(List<T> allRows, BaseEntity entity) {
        Integer entityPageNum = entity != null ? entity.getPageNum() : null;
        Integer entityPageSize = entity != null ? entity.getPageSize() : null;
        PageDomain requestDomain = TableSupport.buildPageRequest();
        int pageNum = entityPageNum != null && entityPageNum > 0
            ? entityPageNum
            : (requestDomain.getPageNum() != null ? requestDomain.getPageNum() : 1);
        int pageSize = entityPageSize != null && entityPageSize > 0
            ? entityPageSize
            : (requestDomain.getPageSize() != null ? requestDomain.getPageSize() : 10);
        List<T> rows = PageUtils.manualPage(allRows, pageNum, pageSize);
        PageUtils.PageState state = PageUtils.getCurrentPageState();
        long total = state != null ? state.getTotal() : (allRows == null ? 0 : allRows.size());
        int respPageNum = state != null ? state.getPageNum() : pageNum;
        int respPageSize = state != null ? state.getPageSize() : pageSize;
        PageUtils.clearManualPage();
        return TableDataInfo.success(rows, total, respPageNum, respPageSize);
    }

    private <T> TableDataInfo<T> buildPagedResponse(List<T> allRows, Object sourceEntity) {
        PageDomain pageDomain = null;
        if (sourceEntity instanceof com.deepreach.common.core.domain.BaseEntity baseEntity) {
            pageDomain = TableSupport.buildPageRequest(baseEntity);
        } else {
            pageDomain = TableSupport.buildPageRequest();
        }
        int pageNum = pageDomain.getPageNum() != null && pageDomain.getPageNum() > 0 ? pageDomain.getPageNum() : 1;
        int pageSize = pageDomain.getPageSize() != null && pageDomain.getPageSize() > 0 ? pageDomain.getPageSize() : 10;
        List<T> rows = PageUtils.manualPage(allRows, pageNum, pageSize);
        PageUtils.PageState state = PageUtils.getCurrentPageState();
        long total = state != null ? state.getTotal() : (allRows == null ? 0 : allRows.size());
        int respPageNum = state != null ? state.getPageNum() : pageNum;
        int respPageSize = state != null ? state.getPageSize() : pageSize;
        PageUtils.clearManualPage();
        return TableDataInfo.success(rows, total, respPageNum, respPageSize);
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
        // 验证用户是否为商家总账号类型
        if (!isBuyerMainAccount(request.getUserId())) {
            return error("只能为商家总账号类型的用户预扣费");
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
     * 检查用户是否为商家总账号类型
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
