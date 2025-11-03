-- 创建云电脑数据库
CREATE DATABASE IF NOT EXISTS cloud_computer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cloud_computer;

-- 云用户表
CREATE TABLE IF NOT EXISTS t_cloud_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    end_user_id VARCHAR(100) NOT NULL UNIQUE COMMENT '终端用户ID',
    client_username VARCHAR(100) NOT NULL COMMENT '客户端用户名',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_end_user_id (end_user_id),
    INDEX idx_client_username (client_username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云用户表';

-- 电脑信息表
CREATE TABLE IF NOT EXISTS t_computer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    computer_id VARCHAR(100) NOT NULL UNIQUE COMMENT '电脑ID',
    office_siteId VARCHAR(200) NOT NULL COMMENT '办公站点ID',
    computer_name VARCHAR(200) COMMENT '电脑名称',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_computer_id (computer_id),
    INDEX idx_office_site_id (office_siteId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电脑信息表';

-- 用户电脑关联表
CREATE TABLE IF NOT EXISTS t_user_computer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    end_user_id VARCHAR(100) NOT NULL COMMENT '终端用户ID',
    computer_id VARCHAR(100) NOT NULL COMMENT '电脑ID',
    assign_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-解绑',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_computer (end_user_id, computer_id),
    INDEX idx_end_user_id (end_user_id),
    INDEX idx_computer_id (computer_id),
    FOREIGN KEY (end_user_id) REFERENCES t_cloud_user(end_user_id) ON DELETE CASCADE,
    FOREIGN KEY (computer_id) REFERENCES t_computer(computer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户电脑关联表';

-- 插入测试数据
INSERT INTO t_cloud_user (end_user_id, client_username, status) VALUES
('hq01', 'admin', 1),
('hq02', 'user1', 1),
('hq03', 'user2', 1)
ON DUPLICATE KEY UPDATE client_username = VALUES(client_username);

INSERT INTO t_computer (computer_id, office_siteId, computer_name, status) VALUES
('ecd-4i9t0zi25chd9hjw3', 'us-west-1+dir-5588339126', 'CloudComputer-01', 1),
('ecd-4i9t0zi25chd9hjw4', 'us-west-1+dir-5588339126', 'CloudComputer-02', 1)
ON DUPLICATE KEY UPDATE office_siteId = VALUES(office_siteId);

INSERT INTO t_user_computer (end_user_id, computer_id, status) VALUES
('hq01', 'ecd-4i9t0zi25chd9hjw3', 1),
('hq02', 'ecd-4i9t0zi25chd9hjw4', 1)
ON DUPLICATE KEY UPDATE status = VALUES(status);