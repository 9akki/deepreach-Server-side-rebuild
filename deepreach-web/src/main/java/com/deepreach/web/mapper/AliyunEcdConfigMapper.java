package com.deepreach.web.mapper;

import com.deepreach.common.core.mapper.BaseMapper;
import com.deepreach.web.entity.AliyunEcdConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 阿里云ECD配置Mapper接口
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-11-01
 */
@Mapper
public interface AliyunEcdConfigMapper extends BaseMapper<AliyunEcdConfig> {

    /**
     * 根据配置键查询配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    @Select("SELECT config_value FROM t_aliyun_ecd_config WHERE config_key = #{configKey} AND status = 1")
    String selectConfigValueByKey(@Param("configKey") String configKey);

    /**
     * 查询所有启用的配置
     *
     * @return 配置列表
     */
    @Select("SELECT id, config_key as configKey, config_value as configValue, description, status, create_time, update_time FROM t_aliyun_ecd_config WHERE status = 1 ORDER BY config_key")
    List<AliyunEcdConfig> selectAllEnabledConfigs();
}