package com.deepreach.common.core.domain.entity;

import com.deepreach.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实例实体类
 *
 * 管理系统中的各类实例，包括：
 * 1. 营销实例：用于主动营销推广的实例
 * 2. 侦查实例：用于市场调研和数据分析的实例
 * 3. 实例生命周期管理
 * 4. 实例计费统计
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Instance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     *
     * 实例的主键标识，系统内部使用
     * 自增长主键，数据库自动生成
     */
    private Long instanceId;

    /**
     * 实例名称
     *
     * 实例的显示名称，用于用户识别
     * 长度限制：最多50个字符
     * 用户自定义，可以包含中英文和数字
     */
    private String instanceName;

    /**
     * 用户ID
     *
     * 关联sys_user表的外键
     * 实例的所有者用户ID
     * 只有商家总账号和员工可以创建实例
     */
    private Long userId;

    /**
     * 平台类型
     *
     * 实例运行的平台类型：
     * 1 - Facebook：Facebook平台的实例
     * 2 - Google：Google平台的实例
     * 3 - TikTok：TikTok平台的实例
     *
     * 不同平台的实例有不同的配置和计费标准
     */
    private Integer platform;

    /**
     * 实例类型
     *
     * 实例的功能类型：
     * 1 - 营销实例：用于主动营销推广，需要预扣费
     * 2 - 侦查实例：用于市场调研，由营销实例解锁获得
     */
    private Integer type;

    /**
     * 实例状态
     *
     * 实例的当前运行状态：
     * 0 - 待启动：实例已创建但未启动
     * 1 - 运行中：实例正在正常运行
     * 2 - 已停止：实例已停止运行
     * 3 - 异常：实例运行出现异常
     * 4 - 已删除：实例已删除（逻辑删除）
     */
    private Integer status;

    /**
     * 实例配置
     *
     * 实例的配置信息，JSON格式存储
     * 包含实例的运行参数、连接信息等
     * 不同平台的配置结构可能不同
     */
    private String config;

    /**
     * 累计计费天数
     *
     * 实例累计的计费天数
     * 用于计费统计和成本分析
     * 每日计费任务会更新此字段
     */
    private Integer totalBilledDays;

    /**
     * 累计计费金额
     *
     * 实例累计的计费金额（DR积分）
     * 用于计费统计和成本分析
     * 精度支持小数点后2位
     */
    private BigDecimal totalBilledAmount;

    // ==================== 业务判断方法 ====================

    /**
     * 判断是否为营销实例
     *
     * @return true如果是营销实例，false否则
     */
    public boolean isMarketingInstance() {
        return Integer.valueOf(1).equals(this.type);
    }

    /**
     * 判断是否为侦查实例
     *
     * @return true如果是侦查实例，false否则
     */
    public boolean isProspectingInstance() {
        return Integer.valueOf(2).equals(this.type);
    }

    /**
     * 判断实例是否待启动
     *
     * @return true如果实例待启动，false否则
     */
    public boolean isPending() {
        return Integer.valueOf(0).equals(this.status);
    }

    /**
     * 判断实例是否运行中
     *
     * @return true如果实例运行中，false否则
     */
    public boolean isRunning() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 判断实例是否已停止
     *
     * @return true如果实例已停止，false否则
     */
    public boolean isStopped() {
        return Integer.valueOf(2).equals(this.status);
    }

    /**
     * 判断实例是否异常
     *
     * @return true如果实例异常，false否则
     */
    public boolean isAbnormal() {
        return Integer.valueOf(3).equals(this.status);
    }

    /**
     * 判断实例是否已删除
     *
     * @return true如果实例已删除，false否则
     */
    public boolean isDeleted() {
        return Integer.valueOf(4).equals(this.status);
    }

    /**
     * 判断实例是否活跃
     *
     * @return true如果实例处于活跃状态（待启动或运行中），false否则
     */
    public boolean isActive() {
        return isPending() || isRunning();
    }

    /**
     * 判断实例是否可以启动
     *
     * @return true如果实例可以启动，false否则
     */
    public boolean canStart() {
        return isPending() || isStopped();
    }

    /**
     * 判断实例是否可以停止
     *
     * @return true如果实例可以停止，false否则
     */
    public boolean canStop() {
        return isRunning();
    }

    /**
     * 判断实例是否可以重启
     *
     * @return true如果实例可以重启，false否则
     */
    public boolean canRestart() {
        return isStopped() || isAbnormal();
    }

    /**
     * 判断实例是否可以删除
     *
     * @return true如果实例可以删除，false否则
     */
    public boolean canDelete() {
        return !isRunning();
    }

    /**
     * 获取平台类型显示文本
     *
     * @return 平台类型的中文显示名称
     */
    public String getPlatformDisplay() {
        if (this.platform == null) {
            return "未知平台";
        }
        switch (this.platform) {
            case 1:
                return "Facebook";
            case 2:
                return "Google";
            case 3:
                return "TikTok";
            default:
                return "未知平台";
        }
    }

    /**
     * 获取实例类型显示文本
     *
     * @return 实例类型的中文显示名称
     */
    public String getTypeDisplay() {
        if (this.type == null) {
            return "未知类型";
        }
        switch (this.type) {
            case 1:
                return "营销实例";
            case 2:
                return "侦查实例";
            default:
                return "未知类型";
        }
    }

    /**
     * 获取状态显示文本
     *
     * @return 状态的中文显示名称
     */
    public String getStatusDisplay() {
        if (this.status == null) {
            return "未知状态";
        }
        switch (this.status) {
            case 0:
                return "待启动";
            case 1:
                return "运行中";
            case 2:
                return "已停止";
            case 3:
                return "异常";
            case 4:
                return "已删除";
            default:
                return "未知状态";
        }
    }

    /**
     * 获取状态图标
     *
     * @return 状态对应的图标名称
     */
    public String getStatusIcon() {
        if (this.status == null) {
            return "question";
        }
        switch (this.status) {
            case 0:
                return "clock";
            case 1:
                return "play-circle";
            case 2:
                return "pause-circle";
            case 3:
                return "exclamation-circle";
            case 4:
                return "delete";
            default:
                return "question";
        }
    }

    /**
     * 获取状态颜色
     *
     * @return 状态对应的颜色类名
     */
    public String getStatusColor() {
        if (this.status == null) {
            return "secondary";
        }
        switch (this.status) {
            case 0:
                return "warning";
            case 1:
                return "success";
            case 2:
                return "info";
            case 3:
                return "danger";
            case 4:
                return "secondary";
            default:
                return "secondary";
        }
    }

    /**
     * 计算日均成本
     *
     * @return 日均成本（DR积分/天）
     */
    public BigDecimal getDailyAverageCost() {
        if (this.totalBilledDays == null || this.totalBilledDays == 0 ||
            this.totalBilledAmount == null) {
            return BigDecimal.ZERO;
        }
        return this.totalBilledAmount.divide(new BigDecimal(this.totalBilledDays), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 计算运行天数
     *
     * @return 实例的运行天数
     */
    public long getRunningDays() {
        if (this.getCreateTime() == null) {
            return 0;
        }
        return java.time.Duration.between(this.getCreateTime().toLocalDate().atStartOfDay(),
                                         LocalDateTime.now().toLocalDate().atStartOfDay()).toDays();
    }

    /**
     * 判断是否为新创建的实例
     *
     * @return true如果创建时间小于24小时，false否则
     */
    public boolean isNewInstance() {
        if (this.getCreateTime() == null) {
            return false;
        }
        return java.time.Duration.between(this.getCreateTime(), LocalDateTime.now()).toHours() < 24;
    }

    /**
     * 判断是否为长期运行的实例
     *
     * @return true如果运行天数大于30天，false否则
     */
    public boolean isLongRunningInstance() {
        return getRunningDays() > 30;
    }

    /**
     * 判断是否为高成本实例
     *
     * @return true如果累计费用超过1000 DR，false否则
     */
    public boolean isHighCostInstance() {
        return this.totalBilledAmount != null && this.totalBilledAmount.compareTo(new BigDecimal("1000")) > 0;
    }

    /**
     * 判断是否需要续费提醒
     *
     * @param warningDays 预警天数
     * @return true如果需要提醒，false否则
     */
    public boolean needsRenewalReminder(int warningDays) {
        if (this.totalBilledDays == null || this.totalBilledAmount == null) {
            return false;
        }
        return getRunningDays() > (this.totalBilledDays - warningDays);
    }

    /**
     * 获取成本等级
     *
     * @return 成本等级描述
     */
    public String getCostLevel() {
        if (this.totalBilledAmount == null) {
            return "无消费";
        }

        if (this.totalBilledAmount.compareTo(new BigDecimal("100")) < 0) {
            return "低成本";
        } else if (this.totalBilledAmount.compareTo(new BigDecimal("500")) < 0) {
            return "中等成本";
        } else if (this.totalBilledAmount.compareTo(new BigDecimal("1000")) < 0) {
            return "高成本";
        } else {
            return "超高成本";
        }
    }

    /**
     * 获取实例效率评分
     *
     * @return 效率评分（0-100分）
     */
    public int getEfficiencyScore() {
        if (this.totalBilledDays == null || this.totalBilledDays == 0) {
            return 0;
        }

        long runningDays = getRunningDays();
        if (runningDays == 0) {
            return 100; // 新创建的实例效率最高
        }

        // 效率 = 实际运行天数 / 计费天数 * 100
        double efficiency = (double) runningDays / this.totalBilledDays * 100;
        return Math.min(100, (int) Math.round(efficiency));
    }

    /**
     * 获取实例健康状态
     *
     * @return 健康状态描述
     */
    public String getHealthStatus() {
        if (isAbnormal()) {
            return "异常";
        }

        if (isRunning()) {
            int efficiency = getEfficiencyScore();
            if (efficiency >= 90) {
                return "健康";
            } else if (efficiency >= 70) {
                return "良好";
            } else {
                return "亚健康";
            }
        }

        return "停止";
    }

    /**
     * 获取预估明日费用
     *
     * @param dailyPrice 每日价格
     * @return 预估明日费用
     */
    public BigDecimal getEstimatedTomorrowCost(BigDecimal dailyPrice) {
        if (!isRunning() || dailyPrice == null) {
            return BigDecimal.ZERO;
        }

        // 计算明日剩余时间的比例
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();

        // 计算距离午夜还有多少分钟
        long minutesToMidnight = java.time.Duration.between(now, midnight).toMinutes();
        if (minutesToMidnight <= 0) {
            return BigDecimal.ZERO;
        }

        // 按比例计算费用
        return dailyPrice.multiply(new BigDecimal(minutesToMidnight))
                       .divide(new BigDecimal(1440), 2, BigDecimal.ROUND_HALF_UP); // 1440 = 24 * 60
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建营销实例
     *
     * @param instanceName 实例名称
     * @param userId 用户ID
     * @param platform 平台类型
     * @param config 实例配置
     * @param createBy 创建人
     * @return 营销实例对象
     */
    public static Instance createMarketing(String instanceName, Long userId, Integer platform,
                                         String config, String createBy) {
        Instance instance = new Instance();
        instance.setInstanceName(instanceName);
        instance.setUserId(userId);
        instance.setPlatform(platform);
        instance.setType(1);
        instance.setStatus(0);
        instance.setConfig(config);
        instance.setTotalBilledDays(0);
        instance.setTotalBilledAmount(BigDecimal.ZERO);
        instance.setCreateBy(createBy);
        return instance;
    }

    /**
     * 创建侦查实例
     *
     * @param instanceName 实例名称
     * @param userId 用户ID
     * @param platform 平台类型
     * @param config 实例配置
     * @param createBy 创建人
     * @return 侦查实例对象
     */
    public static Instance createProspecting(String instanceName, Long userId, Integer platform,
                                           String config, String createBy) {
        Instance instance = new Instance();
        instance.setInstanceName(instanceName);
        instance.setUserId(userId);
        instance.setPlatform(platform);
        instance.setType(2);
        instance.setStatus(0);
        instance.setConfig(config);
        instance.setTotalBilledDays(0);
        instance.setTotalBilledAmount(BigDecimal.ZERO);
        instance.setCreateBy(createBy);
        return instance;
    }

    /**
     * 从营销实例创建侦查实例
     *
     * @param marketingInstance 营销实例
     * @param prospectingInstanceName 侦查实例名称
     * @param createBy 创建人
     * @return 侦查实例对象
     */
    public static Instance createProspectingFromMarketing(Instance marketingInstance,
                                                        String prospectingInstanceName,
                                                        String createBy) {
        Instance prospectingInstance = new Instance();
        prospectingInstance.setInstanceName(prospectingInstanceName);
        prospectingInstance.setUserId(marketingInstance.getUserId());
        prospectingInstance.setPlatform(marketingInstance.getPlatform());
        prospectingInstance.setType(2);
        prospectingInstance.setStatus(0);
        prospectingInstance.setConfig(marketingInstance.getConfig());
        prospectingInstance.setTotalBilledDays(0);
        prospectingInstance.setTotalBilledAmount(BigDecimal.ZERO);
        prospectingInstance.setCreateBy(createBy);
        return prospectingInstance;
    }

    // ==================== 验证方法 ====================

    /**
     * 验证实例数据的合法性
     *
     * @return 验证结果，包含错误信息（如果有）
     */
    public String validate() {
        StringBuilder errors = new StringBuilder();

        // 验证实例名称
        if (this.instanceName == null || this.instanceName.trim().isEmpty()) {
            errors.append("实例名称不能为空；");
        } else if (this.instanceName.length() > 50) {
            errors.append("实例名称不能超过50个字符；");
        }

        // 验证用户ID
        if (this.userId == null || this.userId <= 0) {
            errors.append("用户ID必须为正整数；");
        }

        // 验证平台类型
        if (this.platform == null || this.platform < 1 || this.platform > 3) {
            errors.append("平台类型必须是1-3之间的整数；");
        }

        // 验证实例类型
        if (this.type == null || this.type < 1 || this.type > 2) {
            errors.append("实例类型必须是1或2；");
        }

        // 验证状态
        if (this.status == null || this.status < 0 || this.status > 4) {
            errors.append("状态必须是0-4之间的整数；");
        }

        // 验证计费天数
        if (this.totalBilledDays != null && this.totalBilledDays < 0) {
            errors.append("累计计费天数不能为负数；");
        }

        // 验证计费金额
        if (this.totalBilledAmount != null && this.totalBilledAmount.compareTo(BigDecimal.ZERO) < 0) {
            errors.append("累计计费金额不能为负数；");
        }

        return errors.length() > 0 ? errors.toString() : null;
    }

    /**
     * 验证实例是否可以执行指定操作
     *
     * @param operation 操作类型（start, stop, restart, delete）
     * @return 验证结果和原因
     */
    public String canExecuteOperation(String operation) {
        if (operation == null) {
            return "操作类型不能为空";
        }

        switch (operation.toLowerCase()) {
            case "start":
                if (!canStart()) {
                    return "当前状态下不能启动实例";
                }
                break;
            case "stop":
                if (!canStop()) {
                    return "当前状态下不能停止实例";
                }
                break;
            case "restart":
                if (!canRestart()) {
                    return "当前状态下不能重启实例";
                }
                break;
            case "delete":
                if (!canDelete()) {
                    return "运行中的实例不能删除，请先停止";
                }
                break;
            default:
                return "不支持的操作类型";
        }

        return null; // 可以执行操作
    }

    /**
     * 检查实例是否需要计费
     *
     * @return true如果需要计费，false否则
     */
    public boolean needsBilling() {
        // 只有运行中的营销实例需要计费
        return isRunning() && isMarketingInstance();
    }

    /**
     * 获取下次计费时间
     *
     * @return 下次计费时间
     */
    public LocalDateTime getNextBillingTime() {
        if (!needsBilling()) {
            return null;
        }

        // 返回明日凌晨
        return LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay();
    }

    /**
     * 更新计费信息
     *
     * @param dailyPrice 每日价格
     */
    public void updateBillingInfo(BigDecimal dailyPrice) {
        if (dailyPrice != null && dailyPrice.compareTo(BigDecimal.ZERO) > 0) {
            this.totalBilledDays = (this.totalBilledDays != null ? this.totalBilledDays : 0) + 1;
            this.totalBilledAmount = (this.totalBilledAmount != null ? this.totalBilledAmount : BigDecimal.ZERO)
                                   .add(dailyPrice);
        }
    }

    /**
     * 重置计费信息
     *
     * 清空累计的计费天数和金额
     */
    public void resetBillingInfo() {
        this.totalBilledDays = 0;
        this.totalBilledAmount = BigDecimal.ZERO;
    }
}