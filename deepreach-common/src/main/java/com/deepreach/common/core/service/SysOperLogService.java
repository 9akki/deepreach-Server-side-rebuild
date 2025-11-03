package com.deepreach.common.core.service;

import com.deepreach.common.core.domain.entity.SysOperLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 *
 * 提供操作日志的业务逻辑处理，包括：
 * 1. 操作日志的记录和查询
 * 2. 日志统计和分析
 * 3. 日志清理和归档
 * 4. 异常处理和监控
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
public interface SysOperLogService {

    /**
     * 记录操作日志
     *
     * 异步保存操作日志到数据库
     * 包含操作信息、用户信息、请求信息等
     *
     * @param operLog 操作日志对象
     * @return 是否保存成功
     */
    boolean insertOperLog(SysOperLog operLog);

    /**
     * 批量记录操作日志
     *
     * 批量保存多条操作日志
     * 用于批量操作场景
     *
     * @param operLogs 操作日志列表
     * @return 成功保存的数量
     */
    int insertOperLogBatch(List<SysOperLog> operLogs);

    /**
     * 根据操作日志ID查询日志信息
     *
     * @param operId 操作日志ID
     * @return 操作日志对象，如果不存在则返回null
     */
    SysOperLog selectOperLogById(Long operId);

    /**
     * 查询操作日志列表（分页）
     *
     * 支持多条件查询和分页
     *
     * @param operLog 查询条件对象
     * @return 操作日志列表
     */
    List<SysOperLog> selectOperLogList(SysOperLog operLog);

    /**
     * 根据操作者查询操作日志
     *
     * @param operName 操作者用户名
     * @return 操作日志列表
     */
    List<SysOperLog> selectOperLogByOperName(String operName);

    /**
     * 根据时间范围查询操作日志
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 操作日志列表
     */
    List<SysOperLog> selectOperLogByTimeRange(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 统计操作日志总数
     *
     * @param operLog 查询条件对象
     * @return 操作日志总数
     */
    int countOperLog(SysOperLog operLog);

    /**
     * 删除操作日志
     *
     * @param operId 操作日志ID
     * @return 是否删除成功
     */
    boolean deleteOperLogById(Long operId);

    /**
     * 批量删除操作日志
     *
     * @param operIds 操作日志ID列表
     * @return 成功删除的数量
     */
    int deleteOperLogByIds(List<Long> operIds);

    /**
     * 清理历史日志
     *
     * 清理指定时间之前的日志记录
     *
     * @param endTime 结束时间
     * @return 成功清理的数量
     */
    int cleanOperLogByTime(LocalDateTime endTime);

    /**
     * 清理指定天数之前的日志
     *
     * @param days 保留天数
     * @return 成功清理的数量
     */
    int cleanOperLogByDays(Integer days);

    /**
     * 获取最近的操作日志
     *
     * @param limit 查询数量限制
     * @return 操作日志列表
     */
    List<SysOperLog> getRecentOperLog(Integer limit);

    /**
     * 获取失败的操作日志
     *
     * @param limit 查询数量限制
     * @return 失败的操作日志列表
     */
    List<SysOperLog> getFailedOperLog(Integer limit);

    /**
     * 获取操作统计信息
     *
     * 统计各业务类型的操作数量
     *
     * @param beginTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 统计信息
     */
    Map<String, Object> getOperStatistics(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 获取用户操作活跃度统计
     *
     * @param beginTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 用户活跃度统计
     */
    List<Map<String, Object>> getUserActivityStatistics(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 获取系统监控信息
     *
     * 包括日志总数、错误数量、存储大小等
     *
     * @return 系统监控信息
     */
    Map<String, Object> getSystemMonitorInfo();

    /**
     * 异步记录操作日志
     *
     * 使用线程池异步保存日志，提高系统性能
     *
     * @param operLog 操作日志对象
     */
    void insertOperLogAsync(SysOperLog operLog);

    /**
     * 检查日志功能是否正常
     *
     * @return 日志功能状态
     */
    boolean isLogServiceHealthy();

    /**
     * 获取日志保留策略配置
     *
     * @return 保留策略信息
     */
    Map<String, Object> getLogRetentionPolicy();
}