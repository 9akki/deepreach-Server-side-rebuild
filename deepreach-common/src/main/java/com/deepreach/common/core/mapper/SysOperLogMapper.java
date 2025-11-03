package com.deepreach.common.core.mapper;

import com.deepreach.common.core.domain.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志Mapper接口
 *
 * 负责操作日志相关的数据库操作，包括：
 * 1. 操作日志的插入和查询
 * 2. 日志清理和归档
 * 3. 统计分析功能
 * 4. 条件查询和分页
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Mapper
public interface SysOperLogMapper {

    /**
     * 插入操作日志
     *
     * 保存新的操作日志记录到数据库
     *
     * @param operLog 操作日志对象
     * @return 成功插入的记录数，通常为1
     */
    int insertOperLog(SysOperLog operLog);

    /**
     * 根据操作日志ID查询日志信息
     *
     * @param operId 操作日志ID
     * @return 操作日志对象，如果不存在则返回null
     */
    SysOperLog selectOperLogById(@Param("operId") Long operId);

    /**
     * 查询操作日志列表（分页）
     *
     * 支持多条件查询，包括：
     * 1. 操作时间范围
     * 2. 操作者
     * 3. 操作类型
     * 4. 操作状态
     * 5. 模块名称
     *
     * @param operLog 查询条件对象
     * @return 操作日志列表，按操作时间倒序排列
     */
    List<SysOperLog> selectOperLogList(SysOperLog operLog);

    /**
     * 根据操作者查询操作日志
     *
     * 查询指定用户的操作日志记录
     *
     * @param operName 操作者用户名
     * @return 操作日志列表
     */
    List<SysOperLog> selectOperLogByOperName(@Param("operName") String operName);

    /**
     * 根据时间范围查询操作日志
     *
     * 查询指定时间范围内的操作日志
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 操作日志列表
     */
    List<SysOperLog> selectOperLogByTimeRange(@Param("beginTime") LocalDateTime beginTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 根据业务类型查询操作日志
     *
     * 查询指定业务类型的操作日志
     *
     * @param businessType 业务类型代码
     * @return 操作日志列表
     */
    List<SysOperLog> selectOperLogByBusinessType(@Param("businessType") Integer businessType);

    /**
     * 统计操作日志总数
     *
     * 用于后台统计功能，统计符合条件的日志数量
     *
     * @param operLog 查询条件对象
     * @return 操作日志总数
     */
    int countOperLog(SysOperLog operLog);

    /**
     * 统计指定时间范围内的操作日志数量
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 操作日志数量
     */
    int countOperLogByTimeRange(@Param("beginTime") LocalDateTime beginTime,
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各业务类型的操作数量
     *
     * 用于生成操作统计报表
     *
     * @param beginTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 业务类型统计结果
     */
    List<java.util.Map<String, Object>> countOperLogByBusinessType(@Param("beginTime") LocalDateTime beginTime,
                                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各操作者的操作数量
     *
     * 用于生成用户操作活跃度统计
     *
     * @param beginTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 操作者统计结果
     */
    List<java.util.Map<String, Object>> countOperLogByOperName(@Param("beginTime") LocalDateTime beginTime,
                                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 删除操作日志
     *
     * 根据操作日志ID删除日志记录
     * 用于日志管理功能
     *
     * @param operId 操作日志ID
     * @return 成功删除的记录数
     */
    int deleteOperLogById(@Param("operId") Long operId);

    /**
     * 批量删除操作日志
     *
     * 根据操作日志ID列表批量删除日志记录
     * 用于批量日志管理功能
     *
     * @param operIds 操作日志ID列表
     * @return 成功删除的记录数
     */
    int deleteOperLogByIds(@Param("operIds") List<Long> operIds);

    /**
     * 清理指定时间之前的操作日志
     *
     * 定期清理历史日志，保持数据库性能
     *
     * @param endTime 结束时间，删除此时间之前的所有日志
     * @return 成功删除的记录数
     */
    int cleanOperLogByTime(@Param("endTime") LocalDateTime endTime);

    /**
     * 清理指定天数之前的操作日志
     *
     * 定期清理历史日志的便捷方法
     *
     * @param days 保留天数，删除此天数之前的所有日志
     * @return 成功删除的记录数
     */
    int cleanOperLogByDays(@Param("days") Integer days);

    /**
     * 查询最近的操作日志
     *
     * 获取最近的N条操作日志，用于实时监控
     *
     * @param limit 查询数量限制
     * @return 操作日志列表
     */
    List<SysOperLog> selectRecentOperLog(@Param("limit") Integer limit);

    /**
     * 查询失败的操作日志
     *
     * 查询所有操作失败（status=1）的日志记录
     * 用于错误监控和分析
     *
     * @param limit 查询数量限制
     * @return 失败的操作日志列表
     */
    List<SysOperLog> selectFailedOperLog(@Param("limit") Integer limit);

    /**
     * 查询异常操作日志
     *
     * 查询包含错误信息的操作日志
     * 用于异常监控和分析
     *
     * @param limit 查询数量限制
     * @return 异常操作日志列表
     */
    List<SysOperLog> selectErrorOperLog(@Param("limit") Integer limit);

    /**
     * 检查日志表是否存在
     *
     * 用于系统初始化时的表存在性检查
     *
     * @return 表是否存在
     */
    boolean checkTableExists();

    /**
     * 获取日志表记录总数
     *
     * 用于监控日志表的规模和性能
     *
     * @return 日志表记录总数
     */
    long getTableRecordCount();

    /**
     * 获取日志表的存储大小
     *
     * 用于监控日志表的存储占用
     *
     * @return 存储大小（字节）
     */
    Long getTableSize();
}