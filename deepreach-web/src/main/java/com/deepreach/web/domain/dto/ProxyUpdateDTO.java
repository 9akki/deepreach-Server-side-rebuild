package com.deepreach.web.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新代理配置DTO
 *
 * 用于接收更新代理配置的请求参数
 * 所有字段都是可选的，只更新提供的字段
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新代理配置请求")
public class ProxyUpdateDTO {

    @Schema(description = "代理ID", example = "1", required = true)
    private Long proxyId;

    @Schema(description = "代理类型（0 HTTP | 1 SOCKS5）", example = "0")
    @Min(value = 0, message = "代理类型只能是0或1")
    @Max(value = 1, message = "代理类型只能是0或1")
    private Integer proxyType;

    @Schema(description = "代理主机地址", example = "192.168.1.100")
    private String proxyHost;

    @Schema(description = "代理端口", example = "8080")
    private String proxyPort;

    @Schema(description = "代理用户名（可选）", example = "proxy_user")
    private String proxyUsername;

    @Schema(description = "代理密码（可选）", example = "proxy_pass")
    private String proxyPassword;

    @Schema(description = "状态（0正常 | 1弃用）", example = "0")
    private String status;

    @Schema(description = "备注", example = "用于访问外部网络的代理")
    private String remark;

    // ==================== 业务验证方法 ====================

    /**
     * 检查是否有字段需要更新
     *
     * @return true如果有至少一个字段需要更新，false否则
     */
    public boolean hasUpdateFields() {
        return proxyType != null ||
               proxyHost != null ||
               proxyPort != null ||
               proxyUsername != null ||
               proxyPassword != null ||
               status != null ||
               remark != null;
    }

    /**
     * 验证代理主机地址格式（如果提供了地址）
     *
     * @return true如果格式正确或未提供，false否则
     */
    public boolean isValidHost() {
        if (proxyHost == null || proxyHost.trim().isEmpty()) {
            return true; // 未提供，不需要验证
        }
        String host = proxyHost.trim();

        // 简单的IPv4地址验证
        if (host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            String[] parts = host.split("\\.");
            for (String part : parts) {
                try {
                    int num = Integer.parseInt(part);
                    if (num < 0 || num > 255) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }

        // 简单的域名验证
        return host.matches("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?))*$");
    }

    /**
     * 验证代理端口（如果提供了端口）
     *
     * @return true如果端口在有效范围内或未提供，false否则
     */
    public boolean isValidPort() {
        if (proxyPort == null || proxyPort.trim().isEmpty()) {
            return true; // 未提供，不需要验证
        }
        try {
            int port = Integer.parseInt(proxyPort);
            return port >= 1 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 验证代理类型（如果提供了类型）
     *
     * @return true如果类型有效或未提供，false否则
     */
    public boolean isValidProxyType() {
        if (proxyType == null) {
            return true; // 未提供，不需要验证
        }
        return Integer.valueOf(0).equals(proxyType) || Integer.valueOf(1).equals(proxyType);
    }

    /**
     * 验证状态（如果提供了状态）
     *
     * @return true如果状态有效或未提供，false否则
     */
    public boolean isValidStatus() {
        if (status == null) {
            return true; // 未提供，不需要验证
        }
        return "0".equals(status) || "1".equals(status);
    }

    /**
     * 验证认证信息完整性（如果提供了认证信息）
     *
     * @return true如果认证信息完整、都不需要或都未更新，false如果只更新其中一个
     */
    public boolean isValidAuthentication() {
        boolean hasUsername = proxyUsername != null && !proxyUsername.trim().isEmpty();
        boolean hasPassword = proxyPassword != null && !proxyPassword.trim().isEmpty();

        // 如果只更新其中一个，这是不允许的
        if (hasUsername != hasPassword) {
            return false;
        }

        return true;
    }

    /**
     * 验证所有提供的参数
     *
     * @return 验证结果
     */
    public java.util.Map<String, Object> validate() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        java.util.List<String> errors = new java.util.ArrayList<>();

        if (!isValidProxyType()) {
            errors.add("代理类型无效，只能是0（HTTP）或1（SOCKS5）");
        }

        if (!isValidHost()) {
            errors.add("代理主机地址格式无效");
        }

        if (!isValidPort()) {
            errors.add("代理端口号无效，必须在1-65535之间");
        }

        if (!isValidStatus()) {
            errors.add("状态值无效，只能是0（正常）或1（弃用）");
        }

        if (!isValidAuthentication()) {
            errors.add("代理用户名和密码必须同时更新或同时为空");
        }

        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        return result;
    }

    // ==================== 转换方法 ====================

    /**
     * 转换为Proxy实体对象
     *
     * @return Proxy实体对象
     */
    public com.deepreach.web.entity.Proxy toProxy() {
        com.deepreach.web.entity.Proxy proxy = new com.deepreach.web.entity.Proxy();
        proxy.setProxyId(this.proxyId);
        proxy.setProxyType(this.proxyType);
        proxy.setProxyHost(this.proxyHost);
        proxy.setProxyPort(this.proxyPort);
        proxy.setProxyUsername(this.proxyUsername);
        proxy.setProxyPassword(this.proxyPassword);
        proxy.setStatus(this.status);
        proxy.setRemark(this.remark);
        return proxy;
    }

    /**
     * 将更新字段应用到现有的Proxy对象
     *
     * @param existingProxy 现有的Proxy对象
     * @return 更新后的Proxy对象
     */
    public com.deepreach.web.entity.Proxy applyTo(com.deepreach.web.entity.Proxy existingProxy) {
        if (existingProxy == null) {
            return toProxy();
        }

        com.deepreach.web.entity.Proxy proxy = new com.deepreach.web.entity.Proxy();
        proxy.setProxyId(existingProxy.getProxyId());
        proxy.setUserId(existingProxy.getUserId());
        proxy.setProxyType(proxyType != null ? proxyType : existingProxy.getProxyType());
        proxy.setProxyHost(proxyHost != null ? proxyHost : existingProxy.getProxyHost());
        proxy.setProxyPort(proxyPort != null ? proxyPort : existingProxy.getProxyPort());
        proxy.setProxyUsername(proxyUsername != null ? proxyUsername : existingProxy.getProxyUsername());
        proxy.setProxyPassword(proxyPassword != null ? proxyPassword : existingProxy.getProxyPassword());
        proxy.setStatus(status != null ? status : existingProxy.getStatus());
        proxy.setRemark(remark != null ? remark : existingProxy.getRemark());
        proxy.setCreateTime(existingProxy.getCreateTime());
        proxy.setCreateBy(existingProxy.getCreateBy());
        return proxy;
    }
}