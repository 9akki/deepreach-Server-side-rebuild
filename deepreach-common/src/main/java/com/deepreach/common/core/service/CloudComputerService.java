package com.deepreach.common.core.service;

import com.deepreach.common.core.dto.CloudComputerParameterDTO;

/**
 * 云电脑服务接口
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
public interface CloudComputerService {

    /**
     * 获取云电脑参数
     *
     * 根据用户ID判断是否分配云电脑，返回相应的参数信息
     *
     * @param userId 用户ID
     * @return 云电脑参数
     */
    CloudComputerParameterDTO getCloudComputerParameter(Long userId);
}