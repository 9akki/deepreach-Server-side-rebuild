package com.deepreach.web.mapper;

import com.deepreach.web.entity.DrBillingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DR账单记录Mapper接口
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Mapper
public interface DrBillingRecordMapper {

    /**
     * 根据ID查询账单记录
     *
     * @param billId 账单ID
     * @return 账单记录
     */
    DrBillingRecord selectById(@Param("billId") Long billId);

    /**
     * 分页查询账单记录列表
     *
     * @param record 查询条件
     * @return 账单记录列表
     */
    List<DrBillingRecord> selectRecordPage(DrBillingRecord record);

    /**
     * 插入账单记录
     *
     * @param record 账单记录
     * @return 影响行数
     */
    int insert(DrBillingRecord record);

    /**
     * 更新账单记录
     *
     * @param record 账单记录
     * @return 影响行数
     */
    int update(DrBillingRecord record);

    /**
     * 根据用户ID查询账单记录
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 账单记录列表
     */
    List<DrBillingRecord> selectByUserId(@Param("userId") Long userId,
                                         @Param("limit") Integer limit);

    /**
     * 根据用户ID和业务类型查询账单记录
     *
     * @param userId 用户ID
     * @param businessType 业务类型
     * @param limit 限制数量
     * @return 账单记录列表
     */
    List<DrBillingRecord> selectByUserIdAndBusinessType(@Param("userId") Long userId,
                                                        @Param("businessType") String businessType,
                                                        @Param("limit") Integer limit);

    /**
     * 根据用户ID和账单类型查询账单记录
     *
     * @param userId 用户ID
     * @param billType 账单类型
     * @param limit 限制数量
     * @return 账单记录列表
     */
    List<DrBillingRecord> selectByUserIdAndBillType(@Param("userId") Long userId,
                                                    @Param("billType") Integer billType,
                                                    @Param("limit") Integer limit);

    /**
     * 根据时间范围查询用户账单记录
     *
     * @param userId 用户ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 账单记录列表
     */
    List<DrBillingRecord> selectByUserIdAndDateRange(@Param("userId") Long userId,
                                                    @Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);

    /**
     * 获取用户账单统计信息
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    Map<String, Object> selectUserBillStatistics(@Param("userId") Long userId,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);

    /**
     * 统计账单总数
     *
     * @return 总数
     */
    Long countTotal();

    /**
     * 根据账单类型统计数量
     *
     * @param billType 账单类型
     * @return 数量
     */
    Long countByBillType(@Param("billType") Integer billType);

    /**
     * 根据业务类型统计数量
     *
     * @param businessType 业务类型
     * @return 数量
     */
    Long countByBusinessType(@Param("businessType") String businessType);

    /**
     * 统计用户总消费金额
     *
     * @param userId 用户ID
     * @return 总消费金额
     */
    BigDecimal sumTotalConsume(@Param("userId") Long userId);

    /**
     * 统计用户总充值金额
     *
     * @param userId 用户ID
     * @return 总充值金额
     */
    BigDecimal sumTotalRecharge(@Param("userId") Long userId);

    /**
     * 统计指定时间范围内的消费金额
     *
     * @param userId 用户ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 消费金额
     */
    BigDecimal sumConsumeByDateRange(@Param("userId") Long userId,
                                     @Param("startDate") String startDate,
                                     @Param("endDate") String endDate);

    /**
     * 获取最近的成功账单记录
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 账单记录列表
     */
    List<DrBillingRecord> selectRecentSuccessRecords(@Param("userId") Long userId,
                                                    @Param("limit") Integer limit);

    /**
     * 根据业务ID查询账单记录
     *
     * @param businessId 业务ID
     * @return 账单记录列表
     */
    List<DrBillingRecord> selectByBusinessId(@Param("businessId") Long businessId);

    /**
     * 删除账单记录
     *
     * @param billId 账单ID
     * @return 影响行数
     */
    int deleteById(@Param("billId") Long billId);

    /**
     * 批量删除账单记录
     *
     * @param billIds 账单ID列表
     * @return 影响行数
     */
    int batchDelete(@Param("billIds") List<Long> billIds);

    List<DrBillingRecord> selectTransactionsByUser(@Param("userId") Long userId,
                                                   @Param("startTime") String startTime,
                                                   @Param("endTime") String endTime,
                                                   @Param("minAmount") BigDecimal minAmount,
                                                   @Param("maxAmount") BigDecimal maxAmount,
                                                   @Param("billType") Integer billType,
                                                   @Param("businessType") String businessType);
}
