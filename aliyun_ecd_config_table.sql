-- 阿里云ECD配置参数表
CREATE TABLE IF NOT EXISTS t_aliyun_ecd_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value VARCHAR(500) NOT NULL COMMENT '配置值',
    description VARCHAR(200) COMMENT '配置描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阿里云ECD配置参数表';

-- 插入默认配置数据
INSERT INTO t_aliyun_ecd_config (config_key, config_value, description, status) VALUES
('access.key.id', 'LTAI5tKDpAnhZr52ttZfyTp7', '阿里云AccessKeyId', 1),
('access.key.secret', 'yPLYk8hkTCfCW0mZdbqyQ1SzAMEmAr', '阿里云AccessKeySecret', 1),
('endpoint', 'https://ecd.us-west-1.aliyuncs.com', '阿里云ECD服务端点', 1),
('api.version', '2020-10-02', 'API版本', 1),
('action', 'GetLoginToken', 'API操作', 1),
('region.id', 'us-west-1', '区域ID', 1),
('client.id', '123456789000', '客户端ID', 1),
('office.site.id', 'us-west-1+dir-5588339126', '办公站点ID', 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), description = VALUES(description);