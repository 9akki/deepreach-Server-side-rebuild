package com.deepreach.common.core.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 云电脑参数响应DTO
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
@Data
public class CloudComputerParameterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作状态（0-成功，1-失败）
     */
    private Integer status;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 登录令牌
     */
    private String loginToken;

    /**
     * 客户端唯一标识
     */
    private String clientId;

    /**
     * 云电脑唯一标识
     */
    private String computerId;

    /**
     * 登录区域标识
     */
    private String loginRegionId;

    /**
     * 构造成功响应
     */
    public static CloudComputerParameterDTO success(String loginToken, String clientId,
                                                   String computerId, String loginRegionId) {
        CloudComputerParameterDTO dto = new CloudComputerParameterDTO();
        dto.setStatus(0);
        dto.setMessage("获取云电脑参数成功");
        dto.setLoginToken(loginToken);
        dto.setClientId(clientId);
        dto.setComputerId(computerId);
        dto.setLoginRegionId(loginRegionId);
        return dto;
    }

    /**
     * 构造失败响应
     */
    public static CloudComputerParameterDTO fail(String message) {
        CloudComputerParameterDTO dto = new CloudComputerParameterDTO();
        dto.setStatus(1);
        dto.setMessage(message);
        return dto;
    }
}