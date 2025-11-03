package com.deepreach.web.domain.dto;

import lombok.Data;

/**
 * 云电脑数据
 *
 * @author DeepReach Team
 */
@Data
public class CloudComputerData {

    /**
     * 状态码
     */
    private Integer status;

    /**
     * 消息
     */
    private String message;

    /**
     * 登录令牌
     */
    private String loginToken;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 电脑ID
     */
    private String computerId;

    /**
     * 登录区域ID
     */
    private String loginRegionId;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 错误标识
     */
    private Boolean error;

    /**
     * 成功标识
     */
    private Boolean success;

    public static CloudComputerData success(String loginToken, String clientId, String computerId, String loginRegionId) {
        CloudComputerData data = new CloudComputerData();
        data.setStatus(0);
        data.setMessage("获取云电脑参数成功");
        data.setLoginToken(loginToken);
        data.setClientId(clientId);
        data.setComputerId(computerId);
        data.setLoginRegionId(loginRegionId);
        data.setTimestamp(System.currentTimeMillis());
        data.setError(false);
        data.setSuccess(true);
        return data;
    }

    public static CloudComputerData error(String message) {
        CloudComputerData data = new CloudComputerData();
        data.setStatus(1);
        data.setMessage(message);
        data.setError(true);
        data.setSuccess(false);
        data.setTimestamp(System.currentTimeMillis());
        return data;
    }
}