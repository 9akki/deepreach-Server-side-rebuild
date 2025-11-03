package com.deepreach.web.entity;

import com.deepreach.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 代理池实体类 proxy
 *
 * 用于管理HTTP和SOCKS5代理配置：
 * 1. 代理服务器的基本信息（地址、端口、认证）
 * 2. 代理类型和状态管理
 * 3. 用户关联和权限控制
 * 4. 创建和更新审计信息
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Proxy extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 代理ID
     *
     * 代理配置的主键标识，系统内部使用
     * 自增长主键，数据库自动生成
     */
    private Long proxyId;

    /**
     * 用户ID
     *
     * 代理配置所属用户的ID，关联sys_user表
     * 用于权限控制和数据隔离
     * 用户只能管理自己创建的代理配置
     */
    private Long userId;

    /**
     * 代理类型
     *
     * 支持的代理协议类型：
     * 0 - HTTP代理：支持HTTP和HTTPS协议
     * 1 - SOCKS5代理：支持TCP和UDP协议
     *
     * 不同类型的代理有不同的配置和使用场景
     */
    private Integer proxyType;

    /**
     * 代理主机地址
     *
     * 代理服务器的IP地址或域名
     * IPv4地址：如 192.168.1.100
     * 域名：如 proxy.example.com
     * 长度限制：最多20个字符
     */
    private String proxyHost;

    /**
     * 代理端口
     *
     * 代理服务器监听的端口号
     * HTTP代理常用端口：8080, 3128, 8888
     * SOCKS5代理常用端口：1080, 1081
     * 长度限制：最多6个字符
     */
    private String proxyPort;

    /**
     * 代理用户名
     *
     * 代理服务器的认证用户名（可选）
     * 当代理服务器需要用户名密码认证时使用
     * 如果代理服务器无需认证，此字段可为空
     * 长度限制：最多20个字符
     */
    private String proxyUsername;

    /**
     * 代理密码
     *
     * 代理服务器的认证密码（可选）
     * 注意：此字段需要加密存储，明文密码不能直接存储在数据库中
     * 建议使用AES加密算法，密钥通过配置文件管理
     * 长度限制：最多20个字符（加密后）
     */
    private String proxyPassword;

    /**
     * 状态
     *
     * 代理配置的启用状态：
     * 0 - 正常：代理配置可用，可以被系统使用
     * 1 - 弃用：代理配置不可用，暂停使用
     *
     * 状态变更会立即影响代理的使用
     */
    private String status;

    /**
     * 创建时间
     *
     * 代理配置创建的时间
     * 由系统自动记录，创建后不可修改
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // createBy 和 updateBy 字段已在 BaseEntity 中定义为 String 类型

    /**
     * 更新时间
     *
     * 代理配置最后更新的时间
     * 由系统自动记录，每次更新后自动修改
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // updateBy 字段已在 BaseEntity 中定义为 String 类型

    /**
     * 备注
     *
     * 代理配置的备注信息
     * 可用于记录代理的用途、来源等信息
     */
    private String remark;

    // ==================== 业务判断方法 ====================

    /**
     * 判断代理是否为HTTP类型
     *
     * @return true如果是HTTP代理，false否则
     */
    public boolean isHttpProxy() {
        return Integer.valueOf(0).equals(this.proxyType);
    }

    /**
     * 判断代理是否为SOCKS5类型
     *
     * @return true如果是SOCKS5代理，false否则
     */
    public boolean isSocks5Proxy() {
        return Integer.valueOf(1).equals(this.proxyType);
    }

    /**
     * 判断代理状态是否正常
     *
     * @return true如果代理状态正常，false否则
     */
    public boolean isNormal() {
        return "0".equals(this.status);
    }

    /**
     * 判断代理是否被弃用
     *
     * @return true如果代理被弃用，false否则
     */
    public boolean isDisabled() {
        return "1".equals(this.status);
    }

    /**
     * 判断代理是否需要认证
     *
     * @return true如果代理需要用户名密码认证，false否则
     */
    public boolean requiresAuthentication() {
        return this.proxyUsername != null && !this.proxyUsername.trim().isEmpty();
    }

    /**
     * 获取代理类型显示文本
     *
     * @return 代理类型显示文本：HTTP/SOCKS5
     */
    public String getProxyTypeDisplay() {
        if (Integer.valueOf(0).equals(this.proxyType)) {
            return "HTTP";
        } else if (Integer.valueOf(1).equals(this.proxyType)) {
            return "SOCKS5";
        } else {
            return "未知类型";
        }
    }

    /**
     * 获取状态显示文本
     *
     * @return 状态显示文本：正常/弃用
     */
    public String getStatusDisplay() {
        if ("0".equals(this.status)) {
            return "正常";
        } else if ("1".equals(this.status)) {
            return "弃用";
        } else {
            return "未知";
        }
    }

    /**
     * 获取完整的代理地址
     *
     * @return 代理地址，格式：host:port
     */
    public String getProxyAddress() {
        if (this.proxyHost == null || this.proxyPort == null) {
            return "";
        }
        return this.proxyHost.trim() + ":" + this.proxyPort.trim();
    }

    /**
     * 获取代理的连接URL（HTTP类型）
     *
     * 仅适用于HTTP代理
     *
     * @return HTTP代理URL，格式：http://[username:password@]host:port
     */
    public String getHttpProxyUrl() {
        if (!isHttpProxy()) {
            return "";
        }

        StringBuilder url = new StringBuilder("http://");

        if (requiresAuthentication()) {
            url.append(this.proxyUsername).append(":").append("***@");
        }

        url.append(getProxyAddress());
        return url.toString();
    }

    /**
     * 获取代理的连接URL（SOCKS5类型）
     *
     * 仅适用于SOCKS5代理
     *
     * @return SOCKS5代理URL，格式：socks5://[username:password@]host:port
     */
    public String getSocks5ProxyUrl() {
        if (!isSocks5Proxy()) {
            return "";
        }

        StringBuilder url = new StringBuilder("socks5://");

        if (requiresAuthentication()) {
            url.append(this.proxyUsername).append(":").append("***@");
        }

        url.append(getProxyAddress());
        return url.toString();
    }

    /**
     * 获取代理的连接URL（通用）
     *
     * 根据代理类型返回对应的URL格式
     *
     * @return 代理URL
     */
    public String getProxyUrl() {
        if (isHttpProxy()) {
            return getHttpProxyUrl();
        } else if (isSocks5Proxy()) {
            return getSocks5ProxyUrl();
        } else {
            return "";
        }
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建HTTP代理对象
     *
     * @param host 代理主机地址
     * @param port 代理端口
     * @param userId 用户ID
     * @return HTTP代理对象
     */
    public static Proxy createHttpProxy(String host, String port, Long userId) {
        Proxy proxy = new Proxy();
        proxy.setProxyType(0);
        proxy.setProxyHost(host);
        proxy.setProxyPort(port);
        proxy.setUserId(userId);
        proxy.setStatus("0"); // 默认正常状态
        return proxy;
    }

    /**
     * 创建带认证的HTTP代理对象
     *
     * @param host 代理主机地址
     * @param port 代理端口
     * @param username 用户名
     * @param password 密码（明文，需要后续加密）
     * @param userId 用户ID
     * @return 带认证的HTTP代理对象
     */
    public static Proxy createHttpProxy(String host, String port, String username, String password, Long userId) {
        Proxy proxy = createHttpProxy(host, port, userId);
        proxy.setProxyUsername(username);
        proxy.setProxyPassword(password);
        return proxy;
    }

    /**
     * 创建SOCKS5代理对象
     *
     * @param host 代理主机地址
     * @param port 代理端口
     * @param userId 用户ID
     * @return SOCKS5代理对象
     */
    public static Proxy createSocks5Proxy(String host, String port, Long userId) {
        Proxy proxy = new Proxy();
        proxy.setProxyType(1);
        proxy.setProxyHost(host);
        proxy.setProxyPort(port);
        proxy.setUserId(userId);
        proxy.setStatus("0"); // 默认正常状态
        return proxy;
    }

    /**
     * 创建带认证的SOCKS5代理对象
     *
     * @param host 代理主机地址
     * @param port 代理端口
     * @param username 用户名
     * @param password 密码（明文，需要后续加密）
     * @param userId 用户ID
     * @return 带认证的SOCKS5代理对象
     */
    public static Proxy createSocks5Proxy(String host, String port, String username, String password, Long userId) {
        Proxy proxy = createSocks5Proxy(host, port, userId);
        proxy.setProxyUsername(username);
        proxy.setProxyPassword(password);
        return proxy;
    }

    // ==================== 验证方法 ====================

    /**
     * 验证代理配置是否有效
     *
     * @return true如果配置有效，false否则
     */
    public boolean isValid() {
        if (this.proxyHost == null || this.proxyHost.trim().isEmpty()) {
            return false;
        }
        if (this.proxyPort == null || this.proxyPort.trim().isEmpty()) {
            return false;
        }
        if (this.proxyType == null || (this.proxyType != 0 && this.proxyType != 1)) {
            return false;
        }
        if (this.userId == null || this.userId <= 0) {
            return false;
        }
        return true;
    }

    /**
     * 验证代理端口是否有效
     *
     * @return true如果端口在有效范围内，false否则
     */
    public boolean isValidPort() {
        if (this.proxyPort == null) {
            return false;
        }
        try {
            int port = Integer.parseInt(this.proxyPort);
            return port >= 1 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 验证代理主机地址是否有效
     *
     * @return true如果是有效的IP地址或域名，false否则
     */
    public boolean isValidHost() {
        if (this.proxyHost == null || this.proxyHost.trim().isEmpty()) {
            return false;
        }
        String host = this.proxyHost.trim();

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
}