package com.deepreach.common.core.service.impl;

import com.deepreach.common.core.domain.entity.SysOperLog;
import com.deepreach.common.core.mapper.SysOperLogMapper;
import com.deepreach.common.core.service.SysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 操作日志服务实现类
 *
 * 实现操作日志的业务逻辑处理，包括：
 * 1. 日志记录和查询
 * 2. 统计分析功能
 * 3. 日志清理和归档
 * 4. 异步处理优化
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Slf4j
@Service
public class SysOperLogServiceImpl implements SysOperLogService {

    @Autowired
    private SysOperLogMapper operLogMapper;

    /**
     * 异步处理线程池
     */
    private final Executor asyncExecutor = Executors.newFixedThreadPool(5);

    @Override
    public boolean insertOperLog(SysOperLog operLog) {
        try {
            sanitizeOperLogForStorage(operLog);
            int result = operLogMapper.insertOperLog(operLog);
            return result > 0;
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
            return false;
        }
    }

    @Override
    public int insertOperLogBatch(List<SysOperLog> operLogs) {
        try {
            if (operLogs == null || operLogs.isEmpty()) {
                return 0;
            }

            int successCount = 0;
            for (SysOperLog operLog : operLogs) {
                if (insertOperLog(operLog)) {
                    successCount++;
                }
            }
            return successCount;
        } catch (Exception e) {
            log.error("批量保存操作日志失败", e);
            return 0;
        }
    }

    @Override
    public SysOperLog selectOperLogById(Long operId) {
        try {
            return operLogMapper.selectOperLogById(operId);
        } catch (Exception e) {
            log.error("查询操作日志失败：operId={}", operId, e);
            return null;
        }
    }

    @Override
    public List<SysOperLog> selectOperLogList(SysOperLog operLog) {
        try {
            return operLogMapper.selectOperLogList(operLog);
        } catch (Exception e) {
            log.error("查询操作日志列表失败", e);
            return List.of();
        }
    }

    @Override
    public List<SysOperLog> selectOperLogByOperName(String operName) {
        try {
            return operLogMapper.selectOperLogByOperName(operName);
        } catch (Exception e) {
            log.error("根据操作者查询操作日志失败：operName={}", operName, e);
            return List.of();
        }
    }

    @Override
    public List<SysOperLog> selectOperLogByTimeRange(LocalDateTime beginTime, LocalDateTime endTime) {
        try {
            return operLogMapper.selectOperLogByTimeRange(beginTime, endTime);
        } catch (Exception e) {
            log.error("根据时间范围查询操作日志失败：beginTime={}, endTime={}", beginTime, endTime, e);
            return List.of();
        }
    }

    @Override
    public int countOperLog(SysOperLog operLog) {
        try {
            return operLogMapper.countOperLog(operLog);
        } catch (Exception e) {
            log.error("统计操作日志数量失败", e);
            return 0;
        }
    }

    @Override
    public boolean deleteOperLogById(Long operId) {
        try {
            int result = operLogMapper.deleteOperLogById(operId);
            return result > 0;
        } catch (Exception e) {
            log.error("删除操作日志失败：operId={}", operId, e);
            return false;
        }
    }

    @Override
    public int deleteOperLogByIds(List<Long> operIds) {
        try {
            if (operIds == null || operIds.isEmpty()) {
                return 0;
            }
            return operLogMapper.deleteOperLogByIds(operIds);
        } catch (Exception e) {
            log.error("批量删除操作日志失败", e);
            return 0;
        }
    }

    @Override
    public int cleanOperLogByTime(LocalDateTime endTime) {
        try {
            int result = operLogMapper.cleanOperLogByTime(endTime);
            log.info("清理历史操作日志完成，删除数量：{}", result);
            return result;
        } catch (Exception e) {
            log.error("清理历史操作日志失败：endTime={}", endTime, e);
            return 0;
        }
    }

    @Override
    public int cleanOperLogByDays(Integer days) {
        try {
            int result = operLogMapper.cleanOperLogByDays(days);
            log.info("清理{}天前的操作日志完成，删除数量：{}", days, result);
            return result;
        } catch (Exception e) {
            log.error("清理指定天数前的操作日志失败：days={}", days, e);
            return 0;
        }
    }

    @Override
    public List<SysOperLog> getRecentOperLog(Integer limit) {
        try {
            return operLogMapper.selectRecentOperLog(limit);
        } catch (Exception e) {
            log.error("获取最近操作日志失败：limit={}", limit, e);
            return List.of();
        }
    }

    @Override
    public List<SysOperLog> getFailedOperLog(Integer limit) {
        try {
            return operLogMapper.selectFailedOperLog(limit);
        } catch (Exception e) {
            log.error("获取失败操作日志失败：limit={}", limit, e);
            return List.of();
        }
    }

    @Override
    public Map<String, Object> getOperStatistics(LocalDateTime beginTime, LocalDateTime endTime) {
        try {
            List<Map<String, Object>> businessTypeStats = operLogMapper.countOperLogByBusinessType(beginTime, endTime);

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("businessTypeStats", businessTypeStats);
            statistics.put("totalCount", countOperLog(new SysOperLog()));
            statistics.put("failedCount", getFailedOperLog(1000).size());

            return statistics;
        } catch (Exception e) {
            log.error("获取操作统计信息失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public List<Map<String, Object>> getUserActivityStatistics(LocalDateTime beginTime, LocalDateTime endTime) {
        try {
            return operLogMapper.countOperLogByOperName(beginTime, endTime);
        } catch (Exception e) {
            log.error("获取用户活跃度统计失败", e);
            return List.of();
        }
    }

    @Override
    public Map<String, Object> getSystemMonitorInfo() {
        try {
            Map<String, Object> monitorInfo = new HashMap<>();

            // 检查表是否存在
            boolean tableExists = operLogMapper.checkTableExists();
            monitorInfo.put("tableExists", tableExists);

            if (tableExists) {
                // 获取记录总数
                long recordCount = operLogMapper.getTableRecordCount();
                monitorInfo.put("recordCount", recordCount);

                // 获取存储大小
                Long tableSize = operLogMapper.getTableSize();
                monitorInfo.put("tableSize", tableSize);

                // 获取今日日志数量
                LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
                int todayCount = operLogMapper.countOperLogByTimeRange(todayStart, todayEnd);
                monitorInfo.put("todayCount", todayCount);

                // 获取失败日志数量
                int failedCount = getFailedOperLog(10000).size();
                monitorInfo.put("failedCount", failedCount);
            }

            return monitorInfo;
        } catch (Exception e) {
            log.error("获取系统监控信息失败", e);
            return new HashMap<>();
        }
    }

    @Override
    @Async
    public void insertOperLogAsync(SysOperLog operLog) {
        CompletableFuture.runAsync(() -> {
            try {
                insertOperLog(operLog);
            } catch (Exception e) {
                log.error("异步保存操作日志失败", e);
            }
        }, asyncExecutor);
    }

    @Override
    public boolean isLogServiceHealthy() {
        try {
            // 检查表是否存在
            if (!operLogMapper.checkTableExists()) {
                return false;
            }

            // 尝试查询一条记录
            operLogMapper.selectRecentOperLog(1);
            return true;
        } catch (Exception e) {
            log.error("检查日志服务健康状态失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getLogRetentionPolicy() {
        Map<String, Object> policy = new HashMap<>();

        // 日志保留天数配置
        policy.put("retentionDays", 90);

        // 自动清理配置
        policy.put("autoCleanEnabled", true);

        // 清理执行时间（凌晨2点）
        policy.put("cleanSchedule", "0 0 2 * * ?");

        // 最大日志数量
        policy.put("maxLogCount", 1000000L);

        // 压缩策略
        policy.put("compressionEnabled", false);

        return policy;
    }

    /**
     * 避免日志内容超出数据库字段限制导致插入失败.
     *
     * sys_oper_log 表中 oper_param/json_result/error_msg 配置为 VARCHAR(2000)，
     * 这里在写库前做一次兜底截断，确保长请求不会抛出 DataIntegrityViolationException。
     *
     * @param operLog 待持久化的操作日志
     */
    private void sanitizeOperLogForStorage(SysOperLog operLog) {
        if (operLog == null) {
            return;
        }
        operLog.setOperParam(truncateIfNeeded("operParam", operLog.getOperParam(), 2000));
        operLog.setJsonResult(truncateIfNeeded("jsonResult", operLog.getJsonResult(), 2000));
        operLog.setErrorMsg(truncateIfNeeded("errorMsg", operLog.getErrorMsg(), 2000));
    }

    private String truncateIfNeeded(String fieldName, String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        int suffixReserve = Math.min(3, maxLength);
        String truncated = value.substring(0, maxLength - suffixReserve) + "...".substring(0, suffixReserve);
        log.warn("操作日志字段 {} 超过 {} 字符，已截断保存", fieldName, maxLength);
        return truncated;
    }
}
