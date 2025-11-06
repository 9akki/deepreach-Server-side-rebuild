package com.deepreach.common.core.domain.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * 请求参数：调整代理身份。
 */
@Data
public class AgentIdentityAdjustRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标身份标识：仅支持 agent_level_1、agent_level_2、agent_level_3。
     */
    private String targetIdentity;
}
