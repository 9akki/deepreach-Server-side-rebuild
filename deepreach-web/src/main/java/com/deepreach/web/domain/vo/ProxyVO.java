package com.deepreach.web.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代理配置视图对象
 *
 * 用于返回给前端的代理配置信息
 * 包含代理的基本信息、状态信息和使用统计
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "代理配置视图对象")
public class ProxyVO {

    @Schema(description = "代理ID")
    private Long proxyId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "代理类型（0 HTTP | 1 SOCKS5）")
    private Integer proxyType;

    @Schema(description = "代理类型显示文本")
    private String proxyTypeDisplay;

    @Schema(description = "代理主机地址")
    private String proxyHost;

    @Schema(description = "代理端口")
    private String proxyPort;

    @Schema(description = "代理用户名")
    private String proxyUsername;

    @Schema(description = "是否需要认证")
    private Boolean requiresAuthentication;

    @Schema(description = "代理地址（host:port）")
    private String proxyAddress;

    @Schema(description = "代理URL")
    private String proxyUrl;

    @Schema(description = "状态（0正常 | 1弃用）")
    private String status;

    @Schema(description = "状态显示文本")
    private String statusDisplay;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private Long createBy;

    @Schema(description = "创建人姓名")
    private String createByName;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人")
    private Long updateBy;

    @Schema(description = "更新人姓名")
    private String updateByName;

    @Schema(description = "最后测试时间")
    private LocalDateTime lastTestTime;

    @Schema(description = "测试结果（true成功 | false失败）")
    private Boolean lastTestResult;

    @Schema(description = "响应时间（毫秒）")
    private Long responseTime;

    @Schema(description = "使用次数")
    private Long usageCount;

    @Schema(description = "成功率（百分比）")
    private Double successRate;

    @Schema(description = "备注")
    private String remark;

    // ==================== 业务状态方法 ====================

    /**
     * 判断是否为HTTP代理
     */
    public boolean isHttpProxy() {
        return Integer.valueOf(0).equals(this.proxyType);
    }

    /**
     * 判断是否为SOCKS5代理
     */
    public boolean isSocks5Proxy() {
        return Integer.valueOf(1).equals(this.proxyType);
    }

    /**
     * 判断代理状态是否正常
     */
    public boolean isNormal() {
        return "0".equals(this.status);
    }

    /**
     * 判断代理是否被弃用
     */
    public boolean isDisabled() {
        return "1".equals(this.status);
    }

    /**
     * 判断是否需要认证
     */
    public boolean requiresAuth() {
        return this.proxyUsername != null && !this.proxyUsername.trim().isEmpty();
    }

    /**
     * 判断最后测试是否成功
     */
    public boolean isLastTestSuccess() {
        return Boolean.TRUE.equals(this.lastTestResult);
    }

    /**
     * 获取性能等级
     *
     * @return 性能等级（优秀/良好/一般/差）
     */
    public String getPerformanceLevel() {
        if (this.responseTime == null) {
            return "未测试";
        }

        if (this.responseTime <= 200) {
            return "优秀";
        } else if (this.responseTime <= 500) {
            return "良好";
        } else if (this.responseTime <= 1000) {
            return "一般";
        } else {
            return "差";
        }
    }

    /**
     * 获取可用性等级
     *
     * @return 可用性等级（高/中/低）
     */
    public String getAvailabilityLevel() {
        if (this.successRate == null) {
            return "未统计";
        }

        if (this.successRate >= 95) {
            return "高";
        } else if (this.successRate >= 80) {
            return "中";
        } else {
            return "低";
        }
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 从Proxy实体创建ProxyVO
     */
    public static ProxyVO from(com.deepreach.web.entity.Proxy proxy) {
        if (proxy == null) {
            return null;
        }

        return ProxyVO.builder()
                .proxyId(proxy.getProxyId())
                .userId(proxy.getUserId())
                .proxyType(proxy.getProxyType())
                .proxyTypeDisplay(proxy.getProxyTypeDisplay())
                .proxyHost(proxy.getProxyHost())
                .proxyPort(proxy.getProxyPort())
                .proxyUsername(proxy.getProxyUsername())
                .requiresAuthentication(proxy.requiresAuthentication())
                .proxyAddress(proxy.getProxyAddress())
                .proxyUrl(proxy.getProxyUrl())
                .status(proxy.getStatus())
                .statusDisplay(proxy.getStatusDisplay())
                .createTime(proxy.getCreateTime())
                .createBy(Long.valueOf(proxy.getCreateBy()))
                .updateTime(proxy.getUpdateTime())
                .updateBy(Long.valueOf(proxy.getUpdateBy()))
                .remark(proxy.getRemark())
                .build();
    }

    /**
     * 创建带测试结果的ProxyVO
     */
    public static ProxyVO from(com.deepreach.web.entity.Proxy proxy,
                               java.util.Map<String, Object> testResult) {
        ProxyVO vo = from(proxy);
        if (vo != null && testResult != null) {
            vo.setLastTestTime(LocalDateTime.now());
            vo.setLastTestResult((Boolean) testResult.get("success"));
            vo.setResponseTime((Long) testResult.get("responseTime"));
        }
        return vo;
    }
}