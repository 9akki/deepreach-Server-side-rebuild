package com.deepreach.web.mapper;

import com.deepreach.common.core.domain.entity.UserDrBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 用户DR积分余额Mapper接口
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Mapper
public interface UserDrBalanceMapper {

    /**
     * 根据用户ID查询余额信息
     *
     * @param userId 用户ID
     * @return 余额信息
     */
    UserDrBalance selectByUserId(@Param("userId") Long userId);

    /**
     * 分页查询余额列表
     *
     * @param balance 查询条件
     * @return 余额列表
     */
    List<UserDrBalance> selectBalancePage(UserDrBalance balance);

    /**
     * 插入余额记录
     *
     * @param balance 余额信息
     * @return 影响行数
     */
    int insert(UserDrBalance balance);

    /**
     * 更新余额信息
     *
     * @param balance 余额信息
     * @return 影响行数
     */
    int update(UserDrBalance balance);

    /**
     * 乐观锁更新余额（用于充值、消费等操作）
     *
     * @param userId 用户ID
     * @param drBalance DR积分余额
     * @param preDeductedBalance 预扣费余额
     * @param totalRecharge 累计充值金额
     * @param totalConsume 累计消费金额
     * @param totalRefund 累计退款金额
     * @param frozenAmount 冻结金额
     * @param version 版本号
     * @return 影响行数
     */
    int updateBalanceWithVersion(@Param("userId") Long userId,
                                @Param("drBalance") BigDecimal drBalance,
                                @Param("preDeductedBalance") BigDecimal preDeductedBalance,
                                @Param("totalRecharge") BigDecimal totalRecharge,
                                @Param("totalConsume") BigDecimal totalConsume,
                                @Param("totalRefund") BigDecimal totalRefund,
                                @Param("frozenAmount") BigDecimal frozenAmount,
                                @Param("version") Integer version);

    /**
     * 更新用户余额状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @param updateBy 更新者
     * @return 影响行数
     */
    int updateStatus(@Param("userId") Long userId,
                     @Param("status") String status,
                     @Param("updateBy") String updateBy);

    /**
     * 根据状态查询余额列表
     *
     * @param status 状态
     * @return 余额列表
     */
    List<UserDrBalance> selectByStatus(@Param("status") String status);

    /**
     * 统计余额总数
     *
     * @return 总数
     */
    Long countTotal();

    /**
     * 统计正常状态的余额总数
     *
     * @return 总数
     */
    Long countNormal();

    /**
     * 统计冻结状态的余额总数
     *
     * @return 总数
     */
    Long countFrozen();

    /**
     * 查询余额为0的用户数量
     *
     * @return 数量
     */
    Long countZeroBalance();

    /**
     * 查询余额大于指定金额的用户数量
     *
     * @param amount 金额
     * @return 数量
     */
    Long countBalanceGreaterThan(@Param("amount") BigDecimal amount);

    /**
     * 获取余额统计信息
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> selectBalanceStatistics(@Param("userId") Long userId);

    /**
     * 批量更新余额状态
     *
     * @param userIds 用户ID列表
     * @param status 状态
     * @param updateBy 更新者
     * @return 影响行数
     */
    int batchUpdateStatus(@Param("userIds") List<Long> userIds,
                          @Param("status") String status,
                          @Param("updateBy") String updateBy);

    /**
     * 删除余额记录
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Long userId);

    int deductForToken(UserDrBalance userBalance);

    /**
     * 消耗一次AI人设免费创建次数
     *
     * @param userId 商家总账号ID
     * @return 影响行数
     */
    int consumeAiCharacterFreeTimes(@Param("userId") Long userId);
}
