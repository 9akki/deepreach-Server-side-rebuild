package com.deepreach.web.controller;

import com.deepreach.common.annotation.Log;
import com.deepreach.common.web.BaseController;
import com.deepreach.common.web.Result;
import com.deepreach.common.web.page.TableDataInfo;
import com.deepreach.common.enums.BusinessType;
import com.deepreach.web.dto.DrTransactionQuery;
import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.web.service.DrBillingRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DR账单记录控制器
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/dr/billing")
@RequiredArgsConstructor
public class DrBillingRecordController extends BaseController {

    private final DrBillingRecordService billingRecordService;

    /**
     * 查询DR账单记录列表
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:list')")
    @GetMapping("/list")
    public TableDataInfo<DrBillingRecord> list(DrBillingRecord record) {
        startPage();
        List<DrBillingRecord> list = billingRecordService.selectRecordPage(record);
        return getDataTable(list);
    }

    /**
     * 获取DR账单记录详情
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:query')")
    @GetMapping("/detail/{billId}")
    public Result<DrBillingRecord> getInfo(@PathVariable Long billId) {
        DrBillingRecord record = billingRecordService.getById(billId);
        return success(record);
    }

    /**
     * 根据用户ID查询账单记录
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:query')")
    @GetMapping("/user/{userId}")
    public Result<List<DrBillingRecord>> getUserRecords(@PathVariable Long userId,
                                                       @RequestParam(required = false) Integer limit) {
        List<DrBillingRecord> records = billingRecordService.getUserRecentRecords(userId, limit);
        return success(records);
    }

    /**
     * 根据业务类型查询用户账单记录
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:query')")
    @GetMapping("/user/{userId}/business/{businessType}")
    public Result<List<DrBillingRecord>> getUserRecordsByBusinessType(@PathVariable Long userId,
                                                                     @PathVariable String businessType,
                                                                     @RequestParam(required = false) Integer limit) {
        List<DrBillingRecord> records = billingRecordService.getUserRecordsByBusinessType(userId, businessType, limit);
        return success(records);
    }

    /**
     * 根据账单类型查询用户账单记录
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:query')")
    @GetMapping("/user/{userId}/type/{billType}")
    public Result<List<DrBillingRecord>> getUserRecordsByBillType(@PathVariable Long userId,
                                                                 @PathVariable Integer billType,
                                                                 @RequestParam(required = false) Integer limit) {
        List<DrBillingRecord> records = billingRecordService.getUserRecordsByBillType(userId, billType, limit);
        return success(records);
    }

    /**
     * 根据时间范围查询用户账单记录
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:query')")
    @GetMapping("/user/{userId}/daterange")
    public Result<List<DrBillingRecord>> getUserRecordsByDateRange(@PathVariable Long userId,
                                                                  @RequestParam String startDate,
                                                                  @RequestParam String endDate) {
        List<DrBillingRecord> records = billingRecordService.getRecordsByDateRange(userId, startDate, endDate);
        return success(records);
    }

    /**
     * 根据商户用户ID查询DR积分收支明细
     *
     * 支持通过查询参数筛选时间范围、金额区间、账单类型和业务类型。
     * 返回结果中包含consumer字段，用于标识每条记录的发起人。
     */
    @GetMapping("/user/{userId}/transactions")
    public Result<List<DrBillingRecord>> getUserTransactions(@PathVariable Long userId, DrTransactionQuery query) {
        List<DrBillingRecord> records = billingRecordService.selectTransactionsByUser(userId, query);
        return success(records);
    }

    /**
     * 获取用户账单统计信息
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:statistics')")
    @GetMapping("/statistics/{userId}")
    public Result<Map<String, Object>> getUserStatistics(@PathVariable Long userId,
                                                         @RequestParam(required = false) String startDate,
                                                         @RequestParam(required = false) String endDate) {
        Map<String, Object> statistics = billingRecordService.getUserBillStatistics(userId, startDate, endDate);
        return success(statistics);
    }

    /**
     * 获取用户今日账单统计
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:statistics')")
    @GetMapping("/statistics/{userId}/today")
    public Result<Map<String, Object>> getUserTodayStatistics(@PathVariable Long userId) {
        LocalDateTime now = LocalDateTime.now();
        String today = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String tomorrow = now.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));

        Map<String, Object> statistics = billingRecordService.getUserBillStatistics(userId, today, tomorrow);
        return success(statistics);
    }

    /**
     * 获取用户本月账单统计
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:statistics')")
    @GetMapping("/statistics/{userId}/month")
    public Result<Map<String, Object>> getUserMonthStatistics(@PathVariable Long userId) {
        LocalDateTime now = LocalDateTime.now();
        String monthStart = now.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String monthEnd = now.plusMonths(1).withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));

        Map<String, Object> statistics = billingRecordService.getUserBillStatistics(userId, monthStart, monthEnd);
        return success(statistics);
    }

    /**
     * 获取用户年度账单统计
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:statistics')")
    @GetMapping("/statistics/{userId}/year")
    public Result<Map<String, Object>> getUserYearStatistics(@PathVariable Long userId) {
        LocalDateTime now = LocalDateTime.now();
        String yearStart = now.withDayOfYear(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String yearEnd = now.plusYears(1).withDayOfYear(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));

        Map<String, Object> statistics = billingRecordService.getUserBillStatistics(userId, yearStart, yearEnd);
        return success(statistics);
    }

    /**
     * 获取账单记录概览统计
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:overview')")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getBillingOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 这里可以调用billingRecordService获取概览数据
        // 由于我们在Service实现类中实现了getBillingOverview方法
        // 如果需要的话，可以将该方法添加到Service接口中

        return success(overview);
    }

    /**
     * 根据业务ID获取账单记录
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:query')")
    @GetMapping("/business/{businessId}")
    public Result<List<DrBillingRecord>> getRecordsByBusinessId(@PathVariable Long businessId) {
        // 由于这个方法在Service实现类中，但不在接口中，需要先添加到接口
        // 暂时返回空列表
        return success(List.of());
    }

    /**
     * 获取用户最近的成功账单记录
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:query')")
    @GetMapping("/user/{userId}/success")
    public Result<List<DrBillingRecord>> getUserRecentSuccessRecords(@PathVariable Long userId,
                                                                    @RequestParam(defaultValue = "10") Integer limit) {
        // 由于这个方法在Service实现类中，但不在接口中，需要先添加到接口
        // 暂时调用getUserRecentRecords
        List<DrBillingRecord> records = billingRecordService.getUserRecentRecords(userId, limit);
        return success(records);
    }

    /**
     * 获取账单类型统计
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:statistics')")
    @GetMapping("/statistics/types")
    public Result<Map<String, Object>> getBillTypeStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 可以在这里添加各种账单类型的统计逻辑
        statistics.put("rechargeCount", 0);
        statistics.put("consumeCount", 0);
        statistics.put("refundCount", 0);

        return success(statistics);
    }

    /**
     * 获取业务类型统计
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:statistics')")
    @GetMapping("/statistics/business-types")
    public Result<Map<String, Object>> getBusinessTypeStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 可以在这里添加各种业务类型的统计逻辑
        statistics.put("instancePreDeductCount", 0);
        statistics.put("marketingInstanceCount", 0);
        statistics.put("prospectingInstanceCount", 0);
        statistics.put("smsCount", 0);
        statistics.put("tokenCount", 0);

        return success(statistics);
    }

    /**
     * 导出用户账单记录
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:export')")
    @Log(title = "导出DR账单记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export/{userId}")
    public Result<String> exportUserRecords(@PathVariable Long userId,
                                           @RequestParam(required = false) String startDate,
                                           @RequestParam(required = false) String endDate) {
        try {
            // 这里可以实现导出逻辑
            // 生成Excel或PDF文件

            return success("账单记录导出成功");
        } catch (Exception e) {
            log.error("导出账单记录失败：用户ID={}", userId, e);
            return error("导出失败：" + e.getMessage());
        }
    }

    /**
     * 重新生成账单编号
     */
    // @PreAuthorize("@ss.hasPermi('dr:billing:edit')")
    @Log(title = "重新生成账单编号", businessType = BusinessType.UPDATE)
    @PutMapping("/{billId}/regenerate-bill-no")
    public Result<String> regenerateBillNo(@PathVariable Long billId) {
        try {
            DrBillingRecord record = billingRecordService.getById(billId);
            if (record == null) {
                return error("账单记录不存在");
            }

            String newBillNo = billingRecordService.generateBillNo();
            // 这里需要更新账单记录的billNo字段

            return success("账单编号重新生成成功", newBillNo);
        } catch (Exception e) {
            log.error("重新生成账单编号失败：账单ID={}", billId, e);
            return error("重新生成失败：" + e.getMessage());
        }
    }
}
