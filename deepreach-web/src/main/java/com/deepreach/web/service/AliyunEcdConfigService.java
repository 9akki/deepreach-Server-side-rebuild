package com.deepreach.web.service;

import com.deepreach.common.core.service.BaseService;
import com.deepreach.web.entity.AliyunEcdConfig;

import java.util.List;
import java.util.Map;

/**
 * 阿里云ECD配置服务接口
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-11-01
 */
public interface AliyunEcdConfigService extends BaseService<AliyunEcdConfig> {

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValueByKey(String configKey);

    /**
     * 获取所有启用的配置
     *
     * @return 配置列表
     */
    List<AliyunEcdConfig> getAllEnabledConfigs();

    /**
     * 获取所有启用的配置并转换为Map
     *
     * @return 配置Map
     */
    Map<String, String> getAllEnabledConfigsAsMap();
}