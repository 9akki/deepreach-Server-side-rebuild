package com.deepreach.web.service.impl;

import com.deepreach.web.dto.DrTransactionQuery;
import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.web.mapper.DrBillingRecordMapper;
import com.deepreach.web.service.DrBillingRecordService;
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

/**
 * DR账单记录服务实现类
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrBillingRecordServiceImpl implements DrBillingRecordService {

    private final DrBillingRecordMapper billingRecordMapper;

    @Override
    public DrBillingRecord getById(Long billId) {
        if (billId == null) {
            return null;
        }
        return billingRecordMapper.selectById(billId);
    }

    @Override
    public List<DrBillingRecord> selectRecordPage(DrBillingRecord record) {
        return billingRecordMapper.selectRecordPage(record);
    }

    @Override
    public List<DrBillingRecord> selectRechargeOrders(DrBillingRecord record) {
        DrBillingRecord query = record != null ? record : new DrBillingRecord();
        query.setBillType(1);
        query.setBusinessType(DrBillingRecord.BUSINESS_TYPE_RECHARGE);
        List<DrBillingRecord> records = billingRecordMapper.selectRecordPage(query);
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DrBillingRecord createRecord(DrBillingRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("账单记录不能为空");
        }

        // 生成账单编号
        if (record.getBillNo() == null || record.getBillNo().isEmpty()) {
            record.setBillNo(generateBillNo());
        }

        // 设置创建时间
        if (record.getCreateTime() == null) {
            record.setCreateTime(LocalDateTime.now());
        }

        int result = billingRecordMapper.insert(record);
        if (result <= 0) {
            throw new RuntimeException("创建账单记录失败");
        }

        return record;
    }

    @Override
    public Map<String, Object> getUserBillStatistics(Long userId, String startDate, String endDate) {
        if (userId == null) {
            return new HashMap<>();
        }

        Map<String, Object> statistics = billingRecordMapper.selectUserBillStatistics(userId, startDate, endDate);
        if (statistics == null) {
            return new HashMap<>();
        }

        // 添加额外统计信息
        statistics.put("userId", userId);
        statistics.put("startDate", startDate);
        statistics.put("endDate", endDate);

        // 计算平均金额
        Long totalCount = (Long) statistics.get("totalCount");
        if (totalCount != null && totalCount > 0) {
            BigDecimal totalRecharge = (BigDecimal) statistics.get("totalRecharge");
            BigDecimal totalConsume = (BigDecimal) statistics.get("totalConsume");
            BigDecimal totalRefund = (BigDecimal) statistics.get("totalRefund");

            if (totalRecharge != null && totalRecharge.compareTo(BigDecimal.ZERO) > 0) {
                statistics.put("avgRecharge", totalRecharge.divide(new BigDecimal(totalCount), 2, BigDecimal.ROUND_HALF_UP));
            }
            if (totalConsume != null && totalConsume.compareTo(BigDecimal.ZERO) > 0) {
                statistics.put("avgConsume", totalConsume.divide(new BigDecimal(totalCount), 2, BigDecimal.ROUND_HALF_UP));
            }
            if (totalRefund != null && totalRefund.compareTo(BigDecimal.ZERO) > 0) {
                statistics.put("avgRefund", totalRefund.divide(new BigDecimal(totalCount), 2, BigDecimal.ROUND_HALF_UP));
            }
        }

        return statistics;
    }

    @Override
    public List<DrBillingRecord> getUserRecordsByBusinessType(Long userId, String businessType, Integer limit) {
        if (userId == null || businessType == null || businessType.isEmpty()) {
            return List.of();
        }
        return billingRecordMapper.selectByUserIdAndBusinessType(userId, businessType, limit);
    }

    @Override
    public List<DrBillingRecord> getUserRecordsByBillType(Long userId, Integer billType, Integer limit) {
        if (userId == null || billType == null) {
            return List.of();
        }
        return billingRecordMapper.selectByUserIdAndBillType(userId, billType, limit);
    }

    @Override
    public List<DrBillingRecord> getUserRecentRecords(Long userId, Integer limit) {
        if (userId == null) {
            return List.of();
        }
        return billingRecordMapper.selectByUserId(userId, limit);
    }

    @Override
    public String generateBillNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomNum = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        return "DR" + timestamp + randomNum;
    }

    @Override
    public List<DrBillingRecord> getRecordsByDateRange(Long userId, String startDate, String endDate) {
        if (userId == null || startDate == null || endDate == null) {
            return List.of();
        }
        return billingRecordMapper.selectByUserIdAndDateRange(userId, startDate, endDate);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * 获取账单记录分页数据
     */
    public List<DrBillingRecord> getBillingRecordsWithPagination(Long userId, Integer pageNum, Integer pageSize,
                                                                String billType, String businessType) {
        DrBillingRecord queryCondition = new DrBillingRecord();
        queryCondition.setUserId(userId);

        if (billType != null && !billType.isEmpty()) {
            try {
                queryCondition.setBillType(Integer.parseInt(billType));
            } catch (NumberFormatException e) {
                log.warn("Invalid bill type: {}", billType);
            }
        }

        if (businessType != null && !businessType.isEmpty()) {
            queryCondition.setBusinessType(businessType);
        }

        return billingRecordMapper.selectRecordPage(queryCondition);
    }

    /**
     * 获取用户今日统计
     */
    public Map<String, Object> getUserTodayStatistics(Long userId) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String tomorrow = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        return getUserBillStatistics(userId, today, tomorrow);
    }

    /**
     * 获取用户本月统计
     */
    public Map<String, Object> getUserMonthStatistics(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        String monthStart = now.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String monthEnd = now.plusMonths(1).withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        return getUserBillStatistics(userId, monthStart, monthEnd);
    }

    /**
     * 获取用户年度统计
     */
    public Map<String, Object> getUserYearStatistics(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        String yearStart = now.withDayOfYear(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String yearEnd = now.plusYears(1).withDayOfYear(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        return getUserBillStatistics(userId, yearStart, yearEnd);
    }

    /**
     * 批量创建账单记录
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateRecords(List<DrBillingRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (DrBillingRecord record : records) {
            try {
                createRecord(record);
                successCount++;
            } catch (Exception e) {
                log.error("创建账单记录失败: {}", record, e);
                throw e; // 事务回滚
            }
        }

        return successCount;
    }

    /**
     * 获取账单记录统计概览
     */
    public Map<String, Object> getBillingOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 总账单数
        Long totalCount = billingRecordMapper.countTotal();
        overview.put("totalCount", totalCount);

        // 各类型账单数
        overview.put("rechargeCount", billingRecordMapper.countByBillType(1));
        overview.put("consumeCount", billingRecordMapper.countByBillType(2));
        overview.put("refundCount", billingRecordMapper.countByBillType(3));

        // 各业务类型账单数
        overview.put("instancePreDeductCount", billingRecordMapper.countByBusinessType(DrBillingRecord.BUSINESS_TYPE_INSTANCE_PRE_DEDUCT));
        overview.put("marketingInstanceCount", billingRecordMapper.countByBusinessType(DrBillingRecord.BUSINESS_TYPE_INSTANCE_MARKETING));
        overview.put("prospectingInstanceCount", billingRecordMapper.countByBusinessType(DrBillingRecord.BUSINESS_TYPE_INSTANCE_PROSPECTING));
        overview.put("smsCount", billingRecordMapper.countByBusinessType(DrBillingRecord.BUSINESS_TYPE_SMS));
        overview.put("tokenCount", billingRecordMapper.countByBusinessType(DrBillingRecord.BUSINESS_TYPE_TOKEN));

        return overview;
    }

    /**
     * 根据业务ID获取账单记录
     */
    public List<DrBillingRecord> getRecordsByBusinessId(Long businessId) {
        if (businessId == null) {
            return List.of();
        }
        return billingRecordMapper.selectByBusinessId(businessId);
    }

    /**
     * 获取用户最近的成功账单记录
     */
    public List<DrBillingRecord> getUserRecentSuccessRecords(Long userId, Integer limit) {
        if (userId == null) {
            return List.of();
        }
        return billingRecordMapper.selectRecentSuccessRecords(userId, limit);
    }

    @Override
    public List<DrBillingRecord> selectTransactionsByUser(Long userId, DrTransactionQuery query) {
        if (userId == null) {
            return List.of();
        }
        DrTransactionQuery condition = query != null ? query : new DrTransactionQuery();
        return billingRecordMapper.selectTransactionsByUser(
                userId,
                condition.getStartTime(),
                condition.getEndTime(),
                condition.getMinAmount(),
                condition.getMaxAmount(),
                condition.getBillType(),
                condition.getBusinessType()
        );
    }
}
