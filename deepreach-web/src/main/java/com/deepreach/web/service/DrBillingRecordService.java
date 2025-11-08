package com.deepreach.web.service;

import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.web.dto.DrTransactionQuery;

import java.util.List;
import java.util.Map;

/**
 * DR账单记录服务接口
 *
 * @author DeepReach Team
 * @version 1.0
 */
public interface DrBillingRecordService {

    /**
     * 根据ID获取账单记录
     *
     * @param billId 账单ID
     * @return 账单记录
     */
    DrBillingRecord getById(Long billId);

    /**
     * 分页查询账单记录列表
     *
     * @param record 查询条件
     * @return 账单记录列表
     */
    List<DrBillingRecord> selectRecordPage(DrBillingRecord record);

    /**
     * 条件查询充值账单列表（固定充值类型）
     *
     * @param record 查询条件
     * @return 充值账单列表
     */
    List<DrBillingRecord> selectRechargeOrders(DrBillingRecord record);

    /**
     * 创建账单记录
     *
     * @param record 账单记录
     * @return 创建的账单记录
     */
    DrBillingRecord createRecord(DrBillingRecord record);

    /**
     * 获取用户账单统计
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    Map<String, Object> getUserBillStatistics(Long userId, String startDate, String endDate);

    /**
     * 根据业务类型获取用户账单记录
     *
     * @param userId 用户ID
     * @param businessType 业务类型
     * @param limit 限制数量
     * @return 账单记录列表
     */
    List<DrBillingRecord> getUserRecordsByBusinessType(Long userId, String businessType, Integer limit);

    /**
     * 根据账单类型获取用户账单记录
     *
     * @param userId 用户ID
     * @param billType 账单类型
     * @param limit 限制数量
     * @return 账单记录列表
     */
    List<DrBillingRecord> getUserRecordsByBillType(Long userId, Integer billType, Integer limit);

    /**
     * 获取用户最近的账单记录
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 账单记录列表
     */
    List<DrBillingRecord> getUserRecentRecords(Long userId, Integer limit);

    /**
     * 生成账单编号
     *
     * @return 账单编号
     */
    String generateBillNo();

    /**
     * 根据时间范围获取账单记录
     *
     * @param userId 用户ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 账单记录列表
     */
    List<DrBillingRecord> getRecordsByDateRange(Long userId, String startDate, String endDate);

    List<DrBillingRecord> selectTransactionsByUser(Long userId, DrTransactionQuery query);
}
