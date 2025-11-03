package com.deepreach.web.service.impl;

import com.deepreach.common.core.service.impl.BaseServiceImpl;
import com.deepreach.web.entity.AliyunEcdConfig;
import com.deepreach.web.mapper.AliyunEcdConfigMapper;
import com.deepreach.web.service.AliyunEcdConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云ECD配置服务实现
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-11-01
 */
@Slf4j
@Service
public class AliyunEcdConfigServiceImpl extends BaseServiceImpl<AliyunEcdConfigMapper, AliyunEcdConfig> implements AliyunEcdConfigService {

    @Autowired
    @Qualifier("cloudComputerJdbcTemplate")
    private JdbcTemplate cloudComputerJdbcTemplate;

    @Autowired
    private AliyunEcdConfigMapper aliyunEcdConfigMapper;

    @Override
    public String getConfigValueByKey(String configKey) {
        try {
            String configValue = aliyunEcdConfigMapper.selectConfigValueByKey(configKey);
            if (configValue == null) {
                log.warn("未找到配置键: {}", configKey);
            }
            return configValue;
        } catch (Exception e) {
            log.error("获取配置值失败，配置键: {}", configKey, e);
            return null;
        }
    }

    @Override
    public List<AliyunEcdConfig> getAllEnabledConfigs() {
        try {
            return aliyunEcdConfigMapper.selectAllEnabledConfigs();
        } catch (Exception e) {
            log.error("获取所有启用的配置失败", e);
            return null;
        }
    }

    @Override
    public Map<String, String> getAllEnabledConfigsAsMap() {
        Map<String, String> configMap = new HashMap<>();
        try {
            List<AliyunEcdConfig> configs = getAllEnabledConfigs();
            if (configs != null) {
                for (AliyunEcdConfig config : configs) {
                    configMap.put(config.getConfigKey(), config.getConfigValue());
                }
            }
        } catch (Exception e) {
            log.error("获取配置Map失败", e);
        }
        return configMap;
    }
}