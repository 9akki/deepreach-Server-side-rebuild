package com.deepreach.web.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代理连接测试DTO
 *
 * 用于接收代理连接测试的请求参数
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "代理连接测试请求")
public class ProxyTestDTO {

    @Schema(description = "代理类型（0 HTTP | 1 SOCKS5）", example = "0", required = true)
    private Integer proxyType;

    @Schema(description = "代理主机地址", example = "192.168.1.100", required = true)
    @NotBlank(message = "代理主机地址不能为空")
    private String proxyHost;

    @Schema(description = "代理端口", example = "8080", required = true)
    @NotBlank(message = "代理端口不能为空")
    private String proxyPort;

    @Schema(description = "代理用户名（可选）", example = "proxy_user")
    private String proxyUsername;

    @Schema(description = "代理密码（可选）", example = "proxy_pass")
    private String proxyPassword;

    @Schema(description = "测试超时时间（毫秒）", example = "5000")
    private Integer timeout = 5000;

    // ==================== 业务验证方法 ====================

    /**
     * 验证代理主机地址格式
     *
     * @return true如果格式正确，false否则
     */
    public boolean isValidHost() {
        if (proxyHost == null || proxyHost.trim().isEmpty()) {
            return false;
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
     * 验证代理端口
     *
     * @return true如果端口在有效范围内，false否则
     */
    public boolean isValidPort() {
        if (proxyPort == null) {
            return false;
        }
        try {
            int port = Integer.parseInt(proxyPort);
            return port >= 1 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 验证代理类型
     *
     * @return true如果类型有效，false否则
     */
    public boolean isValidProxyType() {
        return Integer.valueOf(0).equals(proxyType) || Integer.valueOf(1).equals(proxyType);
    }

    /**
     * 验证超时时间
     *
     * @return true如果超时时间有效，false否则
     */
    public boolean isValidTimeout() {
        return timeout != null && timeout > 0 && timeout <= 30000; // 最大30秒
    }

    /**
     * 验证认证信息完整性
     *
     * @return true如果认证信息完整或都不需要，false如果只有其中一个
     */
    public boolean isValidAuthentication() {
        boolean hasUsername = proxyUsername != null && !proxyUsername.trim().isEmpty();
        boolean hasPassword = proxyPassword != null && !proxyPassword.trim().isEmpty();

        // 要么都为空（无需认证），要么都有（需要认证）
        return hasUsername == hasPassword;
    }

    /**
     * 验证所有参数
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

        if (!isValidTimeout()) {
            errors.add("测试超时时间无效，必须在1-30000毫秒之间");
        }

        if (!isValidAuthentication()) {
            errors.add("代理用户名和密码必须同时提供或同时为空");
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
        proxy.setProxyType(this.proxyType);
        proxy.setProxyHost(this.proxyHost);
        proxy.setProxyPort(this.proxyPort);
        proxy.setProxyUsername(this.proxyUsername);
        proxy.setProxyPassword(this.proxyPassword);
        return proxy;
    }

    /**
     * 获取连接超时时间
     *
     * @return 连接超时时间（毫秒）
     */
    public int getConnectionTimeout() {
        return timeout != null ? timeout : 5000;
    }
}