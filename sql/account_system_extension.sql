-- =====================================================
-- 账户体系数据库表结构扩展脚本
-- 创建时间: 2025-10-27
-- 说明: 基于现有部门、用户、角色表扩展，支持多级代理和买家账户体系
-- 注意: 扣费逻辑在应用代码中实现，不使用数据库存储过程和触发器
-- =====================================================

-- =====================================================
-- 1. 部门表(sys_dept)扩展
-- =====================================================

-- 添加账户体系相关字段
ALTER TABLE `sys_dept` 
ADD COLUMN `dept_type` CHAR(1) DEFAULT '1' COMMENT '部门类型（1总部 2一级代理 3二级代理 4三级代理 5买家）' AFTER `del_flag`,
ADD COLUMN `agent_code` VARCHAR(50) DEFAULT NULL COMMENT '代理编码（唯一标识）' AFTER `dept_type`,
ADD COLUMN `parent_agent_code` VARCHAR(50) DEFAULT NULL COMMENT '上级代理编码' AFTER `agent_code`,
ADD COLUMN `level` INT(2) DEFAULT 1 COMMENT '部门层级（1总部 2一级代理 3二级代理 4三级代理 5买家）' AFTER `parent_agent_code`,
ADD COLUMN `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人' AFTER `level`,
ADD COLUMN `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话' AFTER `contact_person`,
ADD COLUMN `commission_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '佣金比例' AFTER `contact_phone`,
ADD COLUMN `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '账户余额' AFTER `commission_rate`,
ADD COLUMN `dr_points` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'DR积分余额' AFTER `balance`,
ADD COLUMN `instance_count` INT(10) DEFAULT 0 COMMENT '实例数量' AFTER `dr_points`,
ADD COLUMN `recharge_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '充值业绩' AFTER `instance_count`;

-- 添加索引
ALTER TABLE `sys_dept` 
ADD UNIQUE KEY `uk_agent_code` (`agent_code`),
ADD KEY `idx_dept_type` (`dept_type`),
ADD KEY `idx_parent_agent_code` (`parent_agent_code`),
ADD KEY `idx_level` (`level`);

-- =====================================================
-- 2. 用户表(sys_user)扩展
-- =====================================================

-- 添加账户体系相关字段
ALTER TABLE `sys_user` 
ADD COLUMN `agent_code` VARCHAR(50) DEFAULT NULL COMMENT '所属代理编码' AFTER `dept_id`,
ADD COLUMN `parent_user_id` BIGINT(20) DEFAULT NULL COMMENT '父用户ID（用于买家子账号）' AFTER `agent_code`,
ADD COLUMN `account_type` CHAR(1) DEFAULT '1' COMMENT '账号类型（1管理员 2代理 3买家总账号 4买家子账号）' AFTER `parent_user_id`,
ADD COLUMN `marketing_accounts` INT(5) DEFAULT 0 COMMENT '营销账号数量' AFTER `account_type`,
ADD COLUMN `prospecting_accounts` INT(5) DEFAULT 0 COMMENT '拓客账号数量' AFTER `marketing_accounts`,
ADD COLUMN `sms_accounts` INT(5) DEFAULT 0 COMMENT '短信账号数量' AFTER `prospecting_accounts`,
ADD COLUMN `character_consumption` BIGINT(20) DEFAULT 0 COMMENT '字符消耗数量' AFTER `sms_accounts`,
ADD COLUMN `unlock_prospecting_count` INT(5) DEFAULT 0 COMMENT '解锁拓客账号数量' AFTER `character_consumption`,
ADD COLUMN `virtual_balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '虚拟币余额' AFTER `unlock_prospecting_count`;

-- 添加索引
ALTER TABLE `sys_user` 
ADD KEY `idx_agent_code` (`agent_code`),
ADD KEY `idx_parent_user_id` (`parent_user_id`),
ADD KEY `idx_account_type` (`account_type`);

-- =====================================================
-- 3. 角色表(sys_role)扩展
-- =====================================================

-- 添加账户体系相关字段
ALTER TABLE `sys_role` 
ADD COLUMN `role_category` CHAR(1) DEFAULT '1' COMMENT '角色类别（1系统角色 2代理角色 3买家角色）' AFTER `del_flag`,
ADD COLUMN `max_create_level` INT(2) DEFAULT 0 COMMENT '最大创建层级（0不能创建 1可创建同级 2可创建下级）' AFTER `role_category`,
ADD COLUMN `max_child_accounts` INT(5) DEFAULT 0 COMMENT '最大子账号数量' AFTER `max_create_level`,
ADD COLUMN `can_view_performance` TINYINT(1) DEFAULT 0 COMMENT '是否可查看业绩' AFTER `max_child_accounts`,
ADD COLUMN `can_create_accounts` TINYINT(1) DEFAULT 0 COMMENT '是否可创建账号' AFTER `can_view_performance`,
ADD COLUMN `can_recharge` TINYINT(1) DEFAULT 0 COMMENT '是否可充值' AFTER `can_create_accounts`,
ADD COLUMN `can_view_billing` TINYINT(1) DEFAULT 0 COMMENT '是否可查看账单' AFTER `can_recharge`,
ADD COLUMN `can_config_price` TINYINT(1) DEFAULT 0 COMMENT '是否可配置价格' AFTER `can_view_billing`;

-- 添加索引
ALTER TABLE `sys_role` 
ADD KEY `idx_role_category` (`role_category`);

-- =====================================================
-- 4. 创建业务表
-- =====================================================

-- 4.1 账户配置表(sys_account_config)
DROP TABLE IF EXISTS `sys_account_config`;
CREATE TABLE `sys_account_config` (
    `config_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_type` CHAR(1) NOT NULL COMMENT '配置类型（1营销账号 2拓客账号 3短信 4字符）',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
    `unit` VARCHAR(20) NOT NULL COMMENT '单位',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`config_id`),
    KEY `idx_config_type` (`config_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户配置表';

-- 4.2 账单记录表(sys_billing_record)
DROP TABLE IF EXISTS `sys_billing_record`;
CREATE TABLE `sys_billing_record` (
    `bill_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '账单ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `agent_code` VARCHAR(50) DEFAULT NULL COMMENT '代理编码',
    `bill_type` CHAR(1) NOT NULL COMMENT '账单类型（1充值 2消费 3退款）',
    `billing_type` CHAR(1) NOT NULL COMMENT '结算类型（1秒结秒扣 2日结日扣）',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
    `balance_before` DECIMAL(10,2) NOT NULL COMMENT '操作前余额',
    `balance_after` DECIMAL(10,2) NOT NULL COMMENT '操作后余额',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`bill_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_agent_code` (`agent_code`),
    KEY `idx_bill_type` (`bill_type`),
    KEY `idx_billing_type` (`billing_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单记录表';

-- 4.3 账户使用统计表(sys_account_usage)
DROP TABLE IF EXISTS `sys_account_usage`;
CREATE TABLE `sys_account_usage` (
    `usage_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '使用ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `account_type` CHAR(1) NOT NULL COMMENT '账号类型（1营销 2拓客 3短信）',
    `usage_count` INT(10) DEFAULT 0 COMMENT '使用数量',
    `usage_date` DATE NOT NULL COMMENT '使用日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`usage_id`),
    UNIQUE KEY `uk_user_account_date` (`user_id`, `account_type`, `usage_date`),
    KEY `idx_usage_date` (`usage_date`),
    KEY `idx_account_type` (`account_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户使用统计表';

-- 4.4 代理关系表(sys_agent_relation)
DROP TABLE IF EXISTS `sys_agent_relation`;
CREATE TABLE `sys_agent_relation` (
    `relation_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '关系ID',
    `parent_agent_code` VARCHAR(50) NOT NULL COMMENT '上级代理编码',
    `child_agent_code` VARCHAR(50) NOT NULL COMMENT '下级代理编码',
    `level` INT(2) NOT NULL COMMENT '层级关系（1-2级 2-3级 3-4级）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`relation_id`),
    UNIQUE KEY `uk_parent_child` (`parent_agent_code`, `child_agent_code`),
    KEY `idx_parent_agent_code` (`parent_agent_code`),
    KEY `idx_child_agent_code` (`child_agent_code`),
    KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理关系表';

-- 4.5 实例表(sys_instance)
DROP TABLE IF EXISTS `sys_instance`;
CREATE TABLE `sys_instance` (
    `instance_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '实例ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '所属用户ID',
    `instance_type` CHAR(1) NOT NULL COMMENT '实例类型（1营销 2拓客 3短信）',
    `platform` VARCHAR(20) NOT NULL COMMENT '平台（whatsapp, facebook, instagram, tiktok, sms等）',
    `instance_name` VARCHAR(100) NOT NULL COMMENT '实例名称',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用 2删除）',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `last_billing_time` DATETIME DEFAULT NULL COMMENT '最后计费时间',
    `daily_price` DECIMAL(10,2) NOT NULL COMMENT '每日价格',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`instance_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_instance_type` (`instance_type`),
    KEY `idx_platform` (`platform`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实例表';

-- 4.6 充值记录表(sys_recharge_record)
DROP TABLE IF EXISTS `sys_recharge_record`;
CREATE TABLE `sys_recharge_record` (
    `recharge_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '充值ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `agent_code` VARCHAR(50) DEFAULT NULL COMMENT '代理编码',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '充值金额',
    `dr_points` DECIMAL(10,2) NOT NULL COMMENT 'DR积分数量',
    `payment_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式',
    `payment_status` CHAR(1) DEFAULT '0' COMMENT '支付状态（0待处理 1已成功 2已失败）',
    `telegram_contact` VARCHAR(100) DEFAULT NULL COMMENT 'Telegram联系方式',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `process_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `process_by` VARCHAR(64) DEFAULT NULL COMMENT '处理人',
    PRIMARY KEY (`recharge_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_agent_code` (`agent_code`),
    KEY `idx_payment_status` (`payment_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值记录表';

-- =====================================================
-- 5. 初始化数据
-- =====================================================

-- 5.1 初始化部门数据
-- 更新现有部门为总部
UPDATE `sys_dept` SET `dept_type` = '1', `level` = 1, `agent_code` = 'HEADQUARTERS' WHERE `dept_id` = 100;

-- 一级代理部门1
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (100, '0,100', '一级代理A', '2', 2, 'AGENT_A', 'HEADQUARTERS', 10, '0', 'admin');

-- 一级代理部门2
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (100, '0,100', '一级代理B', '2', 2, 'AGENT_B', 'HEADQUARTERS', 20, '0', 'admin');

-- 一级代理A下的二级代理部门1
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (101, '0,100,101', '二级代理A1', '2', 2, 'AGENT_A1', 'AGENT_A', 10, '0', 'admin');

-- 一级代理A下的二级代理部门2
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (101, '0,100,101', '二级代理A2', '2', 2, 'AGENT_A2', 'AGENT_A', 20, '0', 'admin');

-- 一级代理B下的二级代理部门1
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (102, '0,100,102', '二级代理B1', '2', 2, 'AGENT_B1', 'AGENT_B', 10, '0', 'admin');

-- 二级代理A1下的三级代理部门1
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (103, '0,100,101,103', '三级代理A1-1', '2', 2, 'AGENT_A1_1', 'AGENT_A1', 10, '0', 'admin');

-- 二级代理A1下的三级代理部门2
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (103, '0,100,101,103', '三级代理A1-2', '2', 2, 'AGENT_A1_2', 'AGENT_A1', 20, '0', 'admin');

-- 三级代理A1-1下的买家总账户1
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (105, '0,100,101,103,105', '买家总账户A1-1-1', '3', 3, 'BUYER_A1_1_1', 'AGENT_A1_1', 10, '0', 'admin');

-- 三级代理A1-1下的买家总账户2
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (105, '0,100,101,103,105', '买家总账户A1-1-2', '3', 3, 'BUYER_A1_1_2', 'AGENT_A1_1', 20, '0', 'admin');

-- 一级代理A直接管理的买家总账户
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (101, '0,100,101', '买家总账户A-1', '3', 3, 'BUYER_A_1', 'AGENT_A', 30, '0', 'admin');

-- 总部直接管理的买家总账户
INSERT INTO `sys_dept` (`parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `agent_code`, `parent_agent_code`, `order_num`, `status`, `create_by`)
VALUES (100, '0,100', '买家总账户总部1', '3', 3, 'BUYER_HQ_1', 'HEADQUARTERS', 30, '0', 'admin');

-- 5.2 初始化角色数据
-- 更新现有角色
UPDATE `sys_role` SET `role_category` = '1', `max_create_level` = 5, `can_view_performance` = 1, `can_create_accounts` = 1, `can_recharge` = 1, `can_view_billing` = 1, `can_config_price` = 1 WHERE `role_id` = 1;

-- 一级代理角色
INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `data_scope`, `role_category`, `max_create_level`, `can_view_performance`, `can_create_accounts`, `can_recharge`, `can_view_billing`, `status`, `create_by`, `remark`) 
VALUES ('一级代理', 'agent_level_1', 2, '4', '2', 3, 1, 1, 1, 1, '0', 'admin', '一级代理角色');

-- 二级代理角色
INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `data_scope`, `role_category`, `max_create_level`, `can_view_performance`, `can_create_accounts`, `can_recharge`, `can_view_billing`, `status`, `create_by`, `remark`) 
VALUES ('二级代理', 'agent_level_2', 3, '4', '2', 2, 1, 1, 1, 1, '0', 'admin', '二级代理角色');

-- 三级代理角色
INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `data_scope`, `role_category`, `max_create_level`, `can_view_performance`, `can_create_accounts`, `can_recharge`, `can_view_billing`, `status`, `create_by`, `remark`) 
VALUES ('三级代理', 'agent_level_3', 4, '4', '2', 1, 1, 1, 0, 1, '0', 'admin', '三级代理角色');

-- 买家总账号角色
INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `data_scope`, `role_category`, `max_create_level`, `max_child_accounts`, `can_view_performance`, `can_create_accounts`, `can_recharge`, `status`, `create_by`, `remark`) 
VALUES ('买家总账号', 'buyer_main', 5, '5', '3', 0, 50, 1, 1, 1, '0', 'admin', '买家总账号角色');

-- 买家子账号角色
INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `data_scope`, `role_category`, `max_create_level`, `can_view_performance`, `can_create_accounts`, `can_recharge`, `status`, `create_by`, `remark`) 
VALUES ('买家子账号', 'buyer_sub', 6, '5', '3', 0, 0, 0, 0, 0, '0', 'admin', '买家子账号角色');

-- 5.3 初始化账户配置数据
INSERT INTO `sys_account_config` (`config_type`, `price`, `unit`, `description`, `create_by`) VALUES
('1', 6.00, 'DR/天', '营销账号价格', 'admin'),
('2', 1.00, 'DR/天', '拓客账号价格', 'admin'),
('3', 0.05, 'DR/条', '短信价格', 'admin'),
('4', 10.00, 'DR/100万字符', '字符价格', 'admin');

-- 5.4 初始化代理关系数据
INSERT INTO `sys_agent_relation` (`parent_agent_code`, `child_agent_code`, `level`) VALUES
('HEADQUARTERS', 'AGENT_A', 1),
('HEADQUARTERS', 'AGENT_B', 1),
('AGENT_A', 'AGENT_A1', 1),
('AGENT_A', 'AGENT_A2', 1),
('AGENT_B', 'AGENT_B1', 1),
('AGENT_A1', 'AGENT_A1_1', 1),
('AGENT_A1', 'AGENT_A1_2', 1),
('AGENT_A1_1', 'BUYER_A1_1_1', 1),
('AGENT_A1_1', 'BUYER_A1_1_2', 1),
('AGENT_A', 'BUYER_A_1', 1),
('HEADQUARTERS', 'BUYER_HQ_1', 1);

-- =====================================================
-- 6. 创建视图
-- =====================================================

-- 6.1 账户详细信息视图
CREATE OR REPLACE VIEW `v_account_detail` AS
SELECT
    u.user_id,
    u.username,
    u.nickname,
    u.real_name,
    u.email,
    u.phone,
    u.user_type,
    u.status,
    u.account_type,
    u.agent_code,
    u.marketing_accounts,
    u.prospecting_accounts,
    u.sms_accounts,
    u.character_consumption,
    u.unlock_prospecting_count,
    u.virtual_balance,
    d.dept_id,
    d.dept_name,
    d.dept_type,
    d.level as dept_level,
    d.contact_person,
    d.contact_phone,
    d.commission_rate,
    d.balance as dept_balance,
    d.dr_points,
    d.instance_count,
    d.recharge_amount,
    r.role_id,
    r.role_name,
    r.role_key,
    r.role_category,
    r.max_create_level,
    r.max_child_accounts,
    r.can_view_performance,
    r.can_create_accounts,
    r.can_recharge,
    r.can_view_billing,
    r.can_config_price
FROM sys_user u
LEFT JOIN sys_dept d ON u.dept_id = d.dept_id AND d.del_flag = '0'
LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
LEFT JOIN sys_role r ON ur.role_id = r.role_id AND r.del_flag = '0'
WHERE u.status = '0';

-- 6.2 代理层级视图
CREATE OR REPLACE VIEW `v_agent_hierarchy` AS
SELECT
    d.dept_id,
    d.dept_name,
    d.dept_type,
    d.level,
    d.agent_code,
    d.parent_agent_code,
    d.contact_person,
    d.contact_phone,
    d.commission_rate,
    d.balance,
    d.dr_points,
    d.instance_count,
    d.recharge_amount,
    parent.dept_name as parent_dept_name,
    parent.agent_code as parent_agent_code_full,
    CASE 
        WHEN d.level = 1 THEN '总部'
        WHEN d.level = 2 THEN '一级代理'
        WHEN d.level = 3 THEN '二级代理'
        WHEN d.level = 4 THEN '三级代理'
        WHEN d.level = 5 THEN '买家'
        ELSE '未知'
    END as level_name
FROM sys_dept d
LEFT JOIN sys_dept parent ON d.parent_agent_code = parent.agent_code AND parent.del_flag = '0'
WHERE d.del_flag = '0'
ORDER BY d.level, d.order_num;

-- 6.3 账单统计视图
CREATE OR REPLACE VIEW `v_billing_statistics` AS
SELECT
    u.user_id,
    u.username,
    u.agent_code,
    d.dept_name,
    r.role_name,
    COUNT(b.bill_id) as bill_count,
    SUM(CASE WHEN b.bill_type = '1' THEN b.amount ELSE 0 END) as total_recharge,
    SUM(CASE WHEN b.bill_type = '2' THEN b.amount ELSE 0 END) as total_consume,
    SUM(CASE WHEN b.bill_type = '3' THEN b.amount ELSE 0 END) as total_refund,
    SUM(CASE WHEN b.billing_type = '1' THEN b.amount ELSE 0 END) as instant_billing,
    SUM(CASE WHEN b.billing_type = '2' THEN b.amount ELSE 0 END) as daily_billing,
    MAX(b.create_time) as last_bill_time
FROM sys_user u
LEFT JOIN sys_dept d ON u.dept_id = d.dept_id AND d.del_flag = '0'
LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
LEFT JOIN sys_role r ON ur.role_id = r.role_id AND r.del_flag = '0'
LEFT JOIN sys_billing_record b ON u.user_id = b.user_id
WHERE u.status = '0'
GROUP BY u.user_id, u.username, u.agent_code, d.dept_name, r.role_name;

-- 6.4 实例统计视图
CREATE OR REPLACE VIEW `v_instance_statistics` AS
SELECT
    u.user_id,
    u.username,
    u.agent_code,
    d.dept_name,
    r.role_name,
    COUNT(CASE WHEN i.instance_type = '1' THEN 1 END) as marketing_count,
    COUNT(CASE WHEN i.instance_type = '2' THEN 1 END) as prospecting_count,
    COUNT(CASE WHEN i.instance_type = '3' THEN 1 END) as sms_count,
    COUNT(CASE WHEN i.platform = 'whatsapp' THEN 1 END) as whatsapp_count,
    COUNT(CASE WHEN i.platform = 'facebook' THEN 1 END) as facebook_count,
    COUNT(CASE WHEN i.platform = 'instagram' THEN 1 END) as instagram_count,
    COUNT(CASE WHEN i.platform = 'tiktok' THEN 1 END) as tiktok_count,
    SUM(i.daily_price) as total_daily_cost,
    COUNT(i.instance_id) as total_instances
FROM sys_user u
LEFT JOIN sys_dept d ON u.dept_id = d.dept_id AND d.del_flag = '0'
LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
LEFT JOIN sys_role r ON ur.role_id = r.role_id AND r.del_flag = '0'
LEFT JOIN sys_instance i ON u.user_id = i.user_id AND i.status != '2'
WHERE u.status = '0'
GROUP BY u.user_id, u.username, u.agent_code, d.dept_name, r.role_name;

-- =====================================================
-- 7. 数据完整性约束
-- =====================================================

-- 7.1 添加外键约束
ALTER TABLE `sys_billing_record` 
ADD CONSTRAINT `fk_billing_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE CASCADE;

ALTER TABLE `sys_account_usage` 
ADD CONSTRAINT `fk_usage_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE CASCADE;

ALTER TABLE `sys_instance` 
ADD CONSTRAINT `fk_instance_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE CASCADE;

ALTER TABLE `sys_recharge_record` 
ADD CONSTRAINT `fk_recharge_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE CASCADE;

-- =====================================================
-- 8. 性能优化
-- =====================================================

-- 8.1 创建复合索引
ALTER TABLE `sys_billing_record` 
ADD INDEX `idx_user_bill_type` (`user_id`, `bill_type`),
ADD INDEX `idx_agent_create_time` (`agent_code`, `create_time`);

ALTER TABLE `sys_account_usage` 
ADD INDEX `idx_user_type_date` (`user_id`, `account_type`, `usage_date`);

ALTER TABLE `sys_instance` 
ADD INDEX `idx_user_type_status` (`user_id`, `instance_type`, `status`);

-- =====================================================
-- 9. 注释说明
-- =====================================================

/*
账户体系设计说明：

1. 部门层级：
   - 1级：总部（所有权后台）
   - 2级：一级代理
   - 3级：二级代理
   - 4级：三级代理
   - 5级：买家

2. 账号类型：
   - 1：管理员
   - 2：代理
   - 3：买家总账号
   - 4：买家子账号

3. 角色类别：
   - 1：系统角色
   - 2：代理角色
   - 3：买家角色

4. 配置类型：
   - 1：营销账号
   - 2：拓客账号
   - 3：短信
   - 4：字符

5. 账单类型：
   - 1：充值
   - 2：消费
   - 3：退款

6. 结算类型：
   - 1：秒结秒扣
   - 2：日结日扣

7. 数据权限：
   - 1：全部数据权限
   - 2：自定义数据权限
   - 3：本部门数据权限
   - 4：本部门及以下数据权限
   - 5：本人数据权限

使用说明：
1. 部门表扩展了代理相关字段，支持多级代理管理
2. 用户表扩展了账户相关字段，支持不同类型账号管理
3. 角色表扩展了权限控制字段，支持细粒度权限控制
4. 新增业务表支持账户配置、账单记录、使用统计等功能
5. 创建了视图简化复杂查询
6. 添加了索引和约束优化性能和数据完整性
7. 扣费逻辑在应用代码中实现，不使用数据库存储过程和触发器

创号逻辑：
1. 项目总后台创建代理后台账号再由代理后台创建商家总账号，或者项目总后台直接创建商家总账号
2. 商家后台总账号给员工创建员工子账号和密码
3. 员工子账号可创建营销账号和拓客账号：每充值100 DR可以开1个营销账号

扣费逻辑（在应用代码中实现）：
1. 秒建秒扣：创建实例成功，实时扣除DR积分；当天创建的实例，根据创建时间来决定首次创建扣多少
2. 延续扣费：每天过0点，所有现存的实例直接扣费（通过应用定时任务实现）
3. 短信账号的扣费逻辑：按实际使用量扣费

拓客账号解锁逻辑：
每创建一个营销账号，解锁每个应用平台最大创建10个拓客账号的功能
例如：创建1个whatsapp营销账号，拓客账号解锁10个facebook、10个instagram、10个tiktok、短信账户等

代码实现注意事项：
1. 所有扣费操作需要在应用代码中实现，包括余额检查、扣费、账单记录等
2. 每日扣费需要通过应用定时任务实现，建议在凌晨1点执行
3. 子账号扣费需要同步到父账号，确保数据一致性
4. 所有余额操作需要考虑并发安全，建议使用数据库事务或乐观锁
5. 实例状态变更需要记录操作日志，便于追踪
*/