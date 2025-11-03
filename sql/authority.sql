-- =====================================================
-- DeepReach 认证系统数据库表结构
-- 创建时间: 2025-10-23
-- 说明: 支持后台用户和客户端用户的权限管理系统
-- =====================================================

-- 1. 用户表 (sys_user)
-- 存储所有用户的基本信息，用户类型由部门类型决定
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `user_id`        BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '用户ID',
    `username`       VARCHAR(30)     NOT NULL                   COMMENT '用户账号',
    `password`       VARCHAR(100)    NOT NULL                   COMMENT '密码',
    `nickname`       VARCHAR(30)     DEFAULT NULL                COMMENT '用户昵称',
    `real_name`      VARCHAR(30)     DEFAULT NULL                COMMENT '真实姓名',
    `email`          VARCHAR(50)     DEFAULT NULL                COMMENT '用户邮箱',
    `phone`          VARCHAR(11)     DEFAULT NULL                COMMENT '手机号码',
    `gender`         CHAR(1)         DEFAULT '0'                 COMMENT '用户性别（1男 2女 0未知）',
    `avatar`         VARCHAR(100)    DEFAULT NULL                COMMENT '头像地址',
    `user_type`      TINYINT(1)      DEFAULT 1                  COMMENT '用户类型（1后台用户 2客户端用户）',
    `status`         CHAR(1)         DEFAULT '0'                 COMMENT '帐号状态（0正常 1停用）',
    `dept_id`        BIGINT(20)      NOT NULL                   COMMENT '部门ID（用户类型由部门类型决定）',
    `parent_user_id` BIGINT(20)      DEFAULT NULL                COMMENT '父用户ID（仅买家子账户有效）',
    `login_ip`       VARCHAR(128)    DEFAULT NULL                COMMENT '最后登录IP',
    `login_date`     DATETIME        DEFAULT NULL                COMMENT '最后登录时间',
    `create_by`      VARCHAR(64)     DEFAULT ''                  COMMENT '创建者',
    `create_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_by`      VARCHAR(64)     DEFAULT ''                  COMMENT '更新者',
    `update_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`         VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_user_type` (`user_type`),
    KEY `idx_status` (`status`),
    KEY `idx_parent_user_id` (`parent_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 2. 角色表 (sys_role)
-- 定义系统中的各种角色，与部门类型关联
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `role_id`       BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '角色ID',
    `role_name`     VARCHAR(30)     NOT NULL                   COMMENT '角色名称',
    `role_key`      VARCHAR(100)    NOT NULL                   COMMENT '角色权限字符串',
    `role_sort`     INT(4)          NOT NULL                   COMMENT '显示顺序',
    `dept_type`     CHAR(1)         NOT NULL                   COMMENT '适用部门类型（1系统 2代理 3买家总账户 4买家子账户）',
    `data_scope`    CHAR(1)         DEFAULT '1'                COMMENT '数据范围（1：全部数据权限 2：自定义数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：本人数据权限）',
    `menu_check_strictly` TINYINT(1) DEFAULT 1                COMMENT '菜单树选择项是否关联显示',
    `dept_check_strictly` TINYINT(1) DEFAULT 1                COMMENT '部门树选择项是否关联显示',
    `status`        CHAR(1)         NOT NULL                   COMMENT '角色状态（0正常 1停用）',
    `del_flag`      CHAR(1)         DEFAULT '0'                COMMENT '删除标志（0代表存在 2代表删除）',
    `create_by`     VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_by`     VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
    `update_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`        VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
    PRIMARY KEY (`role_id`),
    UNIQUE KEY `uk_role_key` (`role_key`),
    KEY `idx_dept_type` (`dept_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- 3. 菜单权限表 (sys_menu)
-- 定义系统菜单和权限点，用于后台管理系统的权限控制
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `menu_id`       BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '菜单ID',
    `menu_name`     VARCHAR(50)     NOT NULL                   COMMENT '菜单名称',
    `parent_id`     BIGINT(20)      DEFAULT 0                  COMMENT '父菜单ID',
    `order_num`     INT(4)          DEFAULT 0                  COMMENT '显示顺序',
    `path`          VARCHAR(200)    DEFAULT ''                 COMMENT '路由地址',
    `component`     VARCHAR(255)    DEFAULT NULL                COMMENT '组件路径',
    `query`         VARCHAR(255)    DEFAULT NULL                COMMENT '路由参数',
    `is_frame`      INT(1)          DEFAULT 1                  COMMENT '是否为外链（0是 1否）',
    `is_cache`      INT(1)          DEFAULT 0                  COMMENT '是否缓存（0缓存 1不缓存）',
    `menu_type`     CHAR(1)         DEFAULT ''                 COMMENT '菜单类型（M目录 C菜单 F按钮）',
    `visible`       INT(1)          DEFAULT 0                  COMMENT '菜单状态（0显示 1隐藏）',
    `status`        INT(1)          DEFAULT 0                  COMMENT '菜单状态（0正常 1停用）',
    `perms`         VARCHAR(100)    DEFAULT NULL                COMMENT '权限标识',
    `icon`          VARCHAR(100)    DEFAULT '#'                COMMENT '菜单图标',
    `create_by`     VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_by`     VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
    `update_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`        VARCHAR(500)    DEFAULT ''                 COMMENT '备注',
    PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 4. 部门表 (sys_dept)
-- 组织架构表，支持系统、代理、买家总账户、买家子账户四种类型
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
    `dept_id`       BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '部门ID',
    `parent_id`     BIGINT(20)      DEFAULT 0                  COMMENT '父部门ID',
    `ancestors`     VARCHAR(50)     DEFAULT ''                 COMMENT '祖级列表',
    `dept_name`     VARCHAR(30)     NOT NULL                   COMMENT '部门名称',
    `order_num`     INT(4)          DEFAULT 0                  COMMENT '显示顺序',
    `dept_type`     CHAR(1)         NOT NULL DEFAULT '1'       COMMENT '部门类型（1系统 2代理 3买家总账户 4买家子账户）',
    `level`         INT(2)          DEFAULT 0                  COMMENT '代理层级（仅代理部门有效：1一级代理 2二级代理 3三级代理）',
    `leader`        VARCHAR(20)     DEFAULT NULL                COMMENT '负责人',
    `leader_user_id` BIGINT(20)     DEFAULT NULL                COMMENT '负责人用户ID',
    `phone`         VARCHAR(11)     DEFAULT NULL                COMMENT '联系电话',
    `email`         VARCHAR(50)     DEFAULT NULL                COMMENT '邮箱',
    `status`        CHAR(1)         DEFAULT '0'                 COMMENT '部门状态（0正常 1停用）',
    `del_flag`      CHAR(1)         DEFAULT '0'                 COMMENT '删除标志（0代表存在 2代表删除）',
    `create_by`     VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_by`     VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
    `update_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`        VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
    PRIMARY KEY (`dept_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_dept_type` (`dept_type`),
    KEY `idx_level` (`level`),
    KEY `idx_status` (`status`),
    KEY `idx_leader_user_id` (`leader_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 5. 用户角色关联表 (sys_user_role)
-- 建立用户和角色的多对多关系
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `user_id`   BIGINT(20)  NOT NULL   COMMENT '用户ID',
    `role_id`   BIGINT(20)  NOT NULL   COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- 6. 角色菜单关联表 (sys_role_menu)
-- 建立角色和菜单的多对多关系
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `role_id`   BIGINT(20)  NOT NULL   COMMENT '角色ID',
    `menu_id`   BIGINT(20)  NOT NULL   COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和菜单关联表';

-- 7. 操作日志表 (sys_oper_log)
-- 记录用户操作日志，用于安全审计
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
    `oper_id`        BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '日志主键',
    `title`          VARCHAR(50)     DEFAULT ''                 COMMENT '模块标题',
    `business_type`  INT(2)          DEFAULT 0                  COMMENT '业务类型（0其它 1新增 2修改 3删除）',
    `method`         VARCHAR(100)    DEFAULT ''                 COMMENT '方法名称',
    `request_method` VARCHAR(10)     DEFAULT ''                 COMMENT '请求方式',
    `operator_type`  INT(1)          DEFAULT 0                  COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
    `oper_name`      VARCHAR(50)     DEFAULT ''                 COMMENT '操作人员',
    `dept_name`      VARCHAR(50)     DEFAULT ''                 COMMENT '部门名称',
    `oper_url`       VARCHAR(255)    DEFAULT ''                 COMMENT '请求URL',
    `oper_ip`        VARCHAR(128)    DEFAULT ''                 COMMENT '主机地址',
    `oper_location`  VARCHAR(255)    DEFAULT ''                 COMMENT '操作地点',
    `oper_param`     VARCHAR(2000)   DEFAULT ''                 COMMENT '请求参数',
    `json_result`    VARCHAR(2000)   DEFAULT ''                 COMMENT '返回参数',
    `status`         INT(1)          DEFAULT 0                  COMMENT '操作状态（0正常 1异常）',
    `error_msg`      VARCHAR(2000)   DEFAULT ''                 COMMENT '错误消息',
    `oper_time`      DATETIME        DEFAULT NULL                COMMENT '操作时间',
    PRIMARY KEY (`oper_id`),
    KEY `idx_oper_time` (`oper_time`),
    KEY `idx_oper_name` (`oper_name`),
    KEY `idx_business_type` (`business_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 8. 初始化部门数据
-- 系统部门
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `order_num`, `leader`, `phone`, `email`, `status`, `create_by`, `remark`) VALUES
(100, 0, '0', 'DeepReach科技总部', '1', 0, 0, 'DeepReach', '15888888888', 'admin@deepreach.com', '0', 'admin', '系统总部'),
(101, 100, '0,100', '技术部', '1', 0, 1, '技术总监', '15888888881', 'tech@deepreach.com', '0', 'admin', '技术研发部门'),
(102, 100, '0,100', '运营部', '1', 0, 2, '运营总监', '15888888882', 'ops@deepreach.com', '0', 'admin', '产品运营部门');

-- 一级代理部门
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `order_num`, `leader`, `phone`, `email`, `status`, `create_by`, `remark`) VALUES
(200, 100, '0,100', '一级代理A', '2', 1, 10, '代理A负责人', '15888888891', 'agent_a@deepreach.com', '0', 'admin', '一级代理部门'),
(201, 100, '0,100', '一级代理B', '2', 1, 20, '代理B负责人', '15888888892', 'agent_b@deepreach.com', '0', 'admin', '一级代理部门');

-- 二级代理部门
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `order_num`, `leader`, `phone`, `email`, `status`, `create_by`, `remark`) VALUES
(300, 200, '0,100,200', '二级代理A1', '2', 2, 10, '代理A1负责人', '15888888901', 'agent_a1@deepreach.com', '0', 'admin', '二级代理部门'),
(301, 200, '0,100,200', '二级代理A2', '2', 2, 20, '代理A2负责人', '15888888902', 'agent_a2@deepreach.com', '0', 'admin', '二级代理部门'),
(302, 201, '0,100,201', '二级代理B1', '2', 2, 10, '代理B1负责人', '15888888903', 'agent_b1@deepreach.com', '0', 'admin', '二级代理部门');

-- 三级代理部门
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `order_num`, `leader`, `phone`, `email`, `status`, `create_by`, `remark`) VALUES
(400, 300, '0,100,200,300', '三级代理A1-1', '2', 3, 10, '代理A1-1负责人', '15888888911', 'agent_a1_1@deepreach.com', '0', 'admin', '三级代理部门'),
(401, 300, '0,100,200,300', '三级代理A1-2', '2', 3, 20, '代理A1-2负责人', '15888888912', 'agent_a1_2@deepreach.com', '0', 'admin', '三级代理部门');

-- 买家总账户部门
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `order_num`, `leader`, `phone`, `email`, `status`, `create_by`, `remark`) VALUES
(500, 400, '0,100,200,300,400', '买家总账户A1-1-1', '3', 0, 10, '买家A1-1-1负责人', '15888888921', 'buyer_a1_1_1@deepreach.com', '0', 'admin', '买家总账户'),
(501, 400, '0,100,200,300,400', '买家总账户A1-1-2', '3', 0, 20, '买家A1-1-2负责人', '15888888922', 'buyer_a1_1_2@deepreach.com', '0', 'admin', '买家总账户'),
(502, 200, '0,100,200', '买家总账户A-1', '3', 0, 30, '买家A-1负责人', '15888888923', 'buyer_a_1@deepreach.com', '0', 'admin', '买家总账户'),
(503, 100, '0,100', '买家总账户总部直管1', '3', 0, 40, '买家总部1负责人', '15888888924', 'buyer_hq_1@deepreach.com', '0', 'admin', '买家总账户');

-- 买家子账户部门
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `dept_type`, `level`, `order_num`, `leader`, `phone`, `email`, `status`, `create_by`, `remark`) VALUES
(600, 500, '0,100,200,300,400,500', '买家子账户A1-1-1-1', '4', 0, 10, '子账户负责人1', '15888888931', 'sub_a1_1_1_1@deepreach.com', '0', 'admin', '买家子账户'),
(601, 500, '0,100,200,300,400,500', '买家子账户A1-1-1-2', '4', 0, 20, '子账户负责人2', '15888888932', 'sub_a1_1_1_2@deepreach.com', '0', 'admin', '买家子账户');

-- 9. 初始化角色数据
-- 系统角色
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `dept_type`, `data_scope`, `status`, `create_by`, `remark`) VALUES
(1, '超级管理员', 'admin', 1, '1', '1', '0', 'admin', '系统超级管理员'),
(2, '系统管理员', 'system_admin', 2, '1', '1', '0', 'admin', '系统管理员'),
(3, '技术管理员', 'tech_admin', 3, '1', '3', '0', 'admin', '技术部管理员'),
(4, '运营管理员', 'ops_admin', 4, '1', '4', '0', 'admin', '运营部管理员');

-- 代理角色
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `dept_type`, `data_scope`, `status`, `create_by`, `remark`) VALUES
(10, '一级代理', 'agent_level_1', 10, '2', '4', '0', 'admin', '一级代理角色'),
(11, '二级代理', 'agent_level_2', 11, '2', '4', '0', 'admin', '二级代理角色'),
(12, '三级代理', 'agent_level_3', 12, '2', '4', '0', 'admin', '三级代理角色');

-- 买家角色
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `dept_type`, `data_scope`, `status`, `create_by`, `remark`) VALUES
(20, '买家总账号', 'buyer_main', 20, '3', '5', '0', 'admin', '买家总账号角色'),
(21, '买家子账号', 'buyer_sub', 21, '4', '5', '0', 'admin', '买家子账号角色');

-- 10. 初始化菜单数据
-- 一级菜单
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`) VALUES
(1, '系统管理', 0, 1, 'system', NULL, 'M', '0', '0', NULL, 'system', '系统管理目录'),
(2, '系统监控', 0, 2, 'monitor', NULL, 'M', '0', '0', NULL, 'monitor', '系统监控目录'),
(3, '系统工具', 0, 3, 'tool', NULL, 'M', '0', '0', NULL, 'tool', '系统工具目录');

-- 二级菜单
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`) VALUES
(100, '用户管理', 1, 1, 'user', 'system/user/index', 'C', '0', '0', 'system:user:list', 'user', '用户管理菜单'),
(101, '角色管理', 1, 2, 'role', 'system/role/index', 'C', '0', '0', 'system:role:list', 'peoples', '角色管理菜单'),
(102, '菜单管理', 1, 3, 'menu', 'system/menu/index', 'C', '0', '0', 'system:menu:list', 'tree-table', '菜单管理菜单'),
(103, '部门管理', 1, 4, 'dept', 'system/dept/index', 'C', '0', '0', 'system:dept:list', 'tree', '部门管理菜单'),
(104, '在线用户', 2, 1, 'online', 'monitor/online/index', 'C', '0', '0', 'monitor:online:list', 'online', '在线用户菜单'),
(105, '操作日志', 2, 2, 'operlog', 'monitor/operlog/index', 'C', '0', '0', 'monitor:operlog:list', 'form', '操作日志菜单');

-- 三级菜单按钮权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`) VALUES
(1000, '用户查询', 100, 1, '#', '', 'F', '0', '0', 'system:user:query', '#', ''),
(1001, '用户新增', 100, 2, '#', '', 'F', '0', '0', 'system:user:add', '#', ''),
(1002, '用户修改', 100, 3, '#', '', 'F', '0', '0', 'system:user:edit', '#', ''),
(1003, '用户删除', 100, 4, '#', '', 'F', '0', '0', 'system:user:remove', '#', ''),
(1004, '用户导出', 100, 5, '#', '', 'F', '0', '0', 'system:user:export', '#', ''),
(1005, '角色查询', 101, 1, '#', '', 'F', '0', '0', 'system:role:query', '#', ''),
(1006, '角色新增', 101, 2, '#', '', 'F', '0', '0', 'system:role:add', '#', ''),
(1007, '角色修改', 101, 3, '#', '', 'F', '0', '0', 'system:role:edit', '#', ''),
(1008, '角色删除', 101, 4, '#', '', 'F', '0', '0', 'system:role:remove', '#', ''),
(1009, '菜单查询', 102, 1, '#', '', 'F', '0', '0', 'system:menu:query', '#', ''),
(1010, '菜单新增', 102, 2, '#', '', 'F', '0', '0', 'system:menu:add', '#', ''),
(1011, '菜单修改', 102, 3, '#', '', 'F', '0', '0', 'system:menu:edit', '#', ''),
(1012, '菜单删除', 102, 4, '#', '', 'F', '0', '0', 'system:menu:remove', '#', '');

-- 11. 初始化管理员用户 (密码: admin123)
INSERT INTO `sys_user` (`user_id`, `username`, `password`, `nickname`, `real_name`, `email`, `phone`, `gender`, `user_type`, `status`, `dept_id`, `create_by`, `remark`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSOfvVWbfXCOOVyFGNN6cGC7fkkM7f1ZlqfJX9K9zOj6', '超级管理员', '超级管理员', 'admin@deepreach.com', '15888888888', '1', 1, '0', 100, 'admin', '系统超级管理员');

-- 初始化各类型示例用户
-- 系统用户
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `real_name`, `email`, `phone`, `gender`, `user_type`, `status`, `dept_id`, `create_by`, `remark`) VALUES
('tech_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOVyFGNN6cGC7fkkM7f1ZlqfJX9K9zOj6', '技术管理员', '技术部管理员', 'tech@deepreach.com', '15888888881', '1', 1, '0', 101, 'admin', '技术部管理员'),
('ops_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOVyFGNN6cGC7fkkM7f1ZlqfJX9K9zOj6', '运营管理员', '运营部管理员', 'ops@deepreach.com', '15888888882', '2', 1, '0', 102, 'admin', '运营部管理员');

-- 代理用户
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `real_name`, `email`, `phone`, `gender`, `user_type`, `status`, `dept_id`, `create_by`, `remark`) VALUES
('agent_a', '$2a$10$7JB720yubVSOfvVWbfXCOOVyFGNN6cGC7fkkM7f1ZlqfJX9K9zOj6', '一级代理A', '代理A负责人', 'agent_a@deepreach.com', '15888888891', '1', 1, '0', 200, 'admin', '一级代理A负责人'),
('agent_a1', '$2a$10$7JB720yubVSOfvVWbfXCOOVyFGNN6cGC7fkkM7f1ZlqfJX9K9zOj6', '二级代理A1', '代理A1负责人', 'agent_a1@deepreach.com', '15888888901', '1', 1, '0', 300, 'agent_a', '二级代理A1负责人'),
('agent_a1_1', '$2a$10$7JB720yubVSOfvVWbfXCOOVyFGNN6cGC7fkkM7f1ZlqfJX9K9zOj6', '三级代理A1-1', '代理A1-1负责人', 'agent_a1_1@deepreach.com', '15888888911', '1', 1, '0', 400, 'agent_a1', '三级代理A1-1负责人');

-- 买家用户
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `real_name`, `email`, `phone`, `gender`, `user_type`, `status`, `dept_id`, `create_by`, `remark`) VALUES
('buyer_a1_1_1', '$2a$10$7JB720yubVSOfvVWbfXCOOVyFGNN6cGC7fkkM7f1ZlqfJX9K9zOj6', '买家总账户A1-1-1', '买家A1-1-1负责人', 'buyer_a1_1_1@deepreach.com', '15888888921', '1', 1, '0', 500, 'agent_a1_1', '买家总账户A1-1-1负责人'),
('buyer_sub_a1_1_1_1', '$2a$10$7JB720yubVSOfvVWbfXCOOVyFGNN6cGC7fkkM7f1ZlqfJX9K9zOj6', '买家子账户A1-1-1-1', '子账户负责人1', 'sub_a1_1_1_1@deepreach.com', '15888888931', '2', 2, '0', 600, 'buyer_a1_1_1', '买家子账户A1-1-1-1负责人'),
('buyer_sub_a1_1_1_2', '$2a$10$7JB720yubVSOfvVWbfXCOOVyFGNN6cGC7fkkM7f1ZlqfJX9K9zOj6', '买家子账户A1-1-1-2', '子账户负责人2', 'sub_a1_1_1_2@deepreach.com', '15888888932', '2', 2, '0', 601, 'buyer_a1_1_1', '买家子账户A1-1-1-2负责人');

-- 12. 初始化用户角色关联
-- 所有用户角色关联（包括管理员）
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.user_id, r.role_id FROM `sys_user` u, `sys_role` r
WHERE (u.username = 'admin' AND r.role_key = 'admin')
   OR (u.username = 'tech_admin' AND r.role_key = 'tech_admin')
   OR (u.username = 'ops_admin' AND r.role_key = 'ops_admin')
   OR (u.username = 'agent_a' AND r.role_key = 'agent_level_1')
   OR (u.username = 'agent_a1' AND r.role_key = 'agent_level_2')
   OR (u.username = 'agent_a1_1' AND r.role_key = 'agent_level_3')
   OR (u.username = 'buyer_a1_1_1' AND r.role_key = 'buyer_main')
   OR (u.username IN ('buyer_sub_a1_1_1_1', 'buyer_sub_a1_1_1_2') AND r.role_key = 'buyer_sub');

-- 13. 初始化角色菜单关联 (超级管理员拥有所有菜单权限)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE `menu_id` IN (
    1, 2, 3, 100, 101, 102, 103, 104, 105, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012
);

-- =====================================================
-- 创建索引和约束
-- =====================================================

-- 用户表索引优化
CREATE INDEX `idx_user_username` ON `sys_user` (`username`);
CREATE INDEX `idx_user_email` ON `sys_user` (`email`);
CREATE INDEX `idx_user_phone` ON `sys_user` (`phone`);
CREATE INDEX `idx_user_type_status` ON `sys_user` (`user_type`, `status`);

-- 操作日志表索引优化
CREATE INDEX `idx_oper_log_oper_time` ON `sys_oper_log` (`oper_time`);
CREATE INDEX `idx_oper_log_oper_name` ON `sys_oper_log` (`oper_name`);
CREATE INDEX `idx_oper_log_business_type` ON `sys_oper_log` (`business_type`);
CREATE INDEX `idx_oper_log_status` ON `sys_oper_log` (`status`);

-- =====================================================
-- 创建视图
-- =====================================================

-- 用户详细信息视图 (包含角色和部门信息)
CREATE OR REPLACE VIEW `v_user_detail` AS
SELECT
    u.user_id,
    u.username,
    u.nickname,
    u.real_name,
    u.email,
    u.phone,
    u.gender,
    u.avatar,
    u.user_type,
    u.status,
    u.dept_id,
    d.dept_name,
    d.dept_type,
    d.level as dept_level,
    CASE
        WHEN d.dept_type = '1' THEN '系统部门'
        WHEN d.dept_type = '2' THEN '代理部门'
        WHEN d.dept_type = '3' THEN '买家总账户'
        WHEN d.dept_type = '4' THEN '买家子账户'
        ELSE '未知类型'
    END as dept_type_name,
    CASE
        WHEN d.dept_type = '2' THEN CONCAT(d.level, '级代理')
        ELSE ''
    END as agent_level,
    u.parent_user_id,
    u.login_ip,
    u.login_date,
    u.create_time,
    u.update_time,
    GROUP_CONCAT(r.role_id) as role_ids,
    GROUP_CONCAT(r.role_name) as role_names,
    GROUP_CONCAT(r.role_key) as role_keys
FROM sys_user u
LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
LEFT JOIN sys_role r ON ur.role_id = r.role_id AND r.status = '0' AND r.del_flag = '0'
LEFT JOIN sys_dept d ON u.dept_id = d.dept_id AND d.del_flag = '0'
GROUP BY u.user_id;

-- 用户权限视图
CREATE OR REPLACE VIEW `v_user_permissions` AS
SELECT
    u.user_id,
    u.username,
    u.user_type,
    m.perms
FROM sys_user u
LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
LEFT JOIN sys_role_menu rm ON ur.role_id = rm.role_id
LEFT JOIN sys_menu m ON rm.menu_id = m.menu_id AND m.status = 0
WHERE u.status = '0' AND m.perms IS NOT NULL AND m.perms != '';

-- 部门树结构视图
CREATE OR REPLACE VIEW `v_dept_tree` AS
SELECT
    dept_id,
    parent_id,
    ancestors,
    dept_name,
    dept_type,
    level,
    order_num,
    leader,
    leader_user_id,
    phone,
    email,
    status,
    CASE
        WHEN dept_type = '1' THEN 'system'
        WHEN dept_type = '2' THEN 'agent'
        WHEN dept_type = '3' THEN 'buyer_main'
        WHEN dept_type = '4' THEN 'buyer_sub'
        ELSE 'unknown'
    END as dept_type_code,
    CASE
        WHEN dept_type = '1' THEN '系统部门'
        WHEN dept_type = '2' THEN CONCAT(level, '级代理')
        WHEN dept_type = '3' THEN '买家总账户'
        WHEN dept_type = '4' THEN '买家子账户'
        ELSE '未知类型'
    END as dept_type_display,
    (SELECT COUNT(*) FROM sys_dept WHERE parent_id = d.dept_id AND status = '0' AND del_flag = '0') as child_count,
    CASE
        WHEN parent_id = 0 THEN 'root'
        WHEN (SELECT COUNT(*) FROM sys_dept WHERE parent_id = d.dept_id AND status = '0' AND del_flag = '0') > 0 THEN 'branch'
        ELSE 'leaf'
    END as node_type,
    (SELECT COUNT(*) FROM sys_user WHERE dept_id = d.dept_id AND status = '0') as user_count
FROM sys_dept d
WHERE d.status = '0' AND d.del_flag = '0'
ORDER BY d.dept_type, d.level, d.order_num;

-- 用户部门详细信息视图
CREATE OR REPLACE VIEW `v_user_dept_detail` AS
SELECT
    u.user_id,
    u.username,
    u.nickname,
    u.real_name,
    u.email,
    u.phone,
    u.user_type,
    u.status,
    d.dept_id,
    d.dept_name,
    d.dept_type,
    d.level as dept_level,
    d.ancestors,
    d.parent_id,
    d.leader as dept_leader,
    d.leader_user_id as dept_leader_user_id,
    d.phone as dept_phone,
    d.email as dept_email,
    u.parent_user_id,
    CASE
        WHEN u.dept_id IS NULL THEN '未分配部门'
        WHEN d.dept_id IS NULL THEN '部门不存在'
        ELSE d.dept_name
    END as dept_display_name,
    CASE
        WHEN d.dept_type = '1' THEN '系统部门'
        WHEN d.dept_type = '2' THEN CONCAT(d.level, '级代理')
        WHEN d.dept_type = '3' THEN '买家总账户'
        WHEN d.dept_type = '4' THEN '买家子账户'
        ELSE '未知类型'
    END as dept_type_display,
    r.role_id,
    r.role_name,
    r.role_key,
    r.data_scope
FROM sys_user u
LEFT JOIN sys_dept d ON u.dept_id = d.dept_id AND d.status = '0' AND d.del_flag = '0'
LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
LEFT JOIN sys_role r ON ur.role_id = r.role_id AND r.status = '0' AND r.del_flag = '0'
WHERE u.status = '0';

-- =====================================================
-- 注释：登录日志记录
-- =====================================================
--
-- 由于MySQL权限限制，登录日志记录功能改为在应用代码层面实现
--
-- 在 SysUserServiceImpl.recordLoginInfo() 方法中：
-- 1. 更新用户最后登录信息（IP和时间）
-- 2. 手动记录登录操作日志到 sys_oper_log 表
--
-- 这样可以避免触发器权限问题，同时提供更灵活的日志记录功能
--

-- =====================================================
-- 部门权限管理（Java实现）
-- =====================================================
--
-- 原来的数据库存储函数已改为Java实现，避免MySQL权限问题：
--
-- 1. fn_get_child_dept_ids() -> DeptUtils.getChildDeptIds()
--    - 功能：递归获取指定部门的所有子部门ID列表
--    - 优势：Java代码更易调试和维护
--
-- 2. fn_get_child_dept_ids_recursive() -> DeptUtils.getChildDeptIdsRecursive()
--    - 功能：递归查询子部门的核心逻辑
--    - 优势：支持复杂业务逻辑处理
--
-- 3. fn_check_dept_data_permission() -> DeptUtils.checkDeptDataPermission()
--    - 功能：检查用户是否有部门数据权限
--    - 优势：支持更灵活的权限判断逻辑
--
-- Java实现的优势：
-- - 无数据库权限限制
-- - 更好的调试和测试能力
-- - 支持复杂的业务逻辑
-- - 易于扩展和维护
-- - 支持缓存优化
--

-- =====================================================
-- 部门权限使用示例
-- =====================================================

/*
-- 查询用户有权限访问的部门列表
SELECT DISTINCT dept_id, dept_name
FROM v_dept_tree
WHERE FIND_IN_SET(dept_id, fn_get_child_dept_ids(103)) > 0;

-- 查询当前用户部门下的所有用户
SELECT user_id, username, dept_name
FROM v_user_dept_detail
WHERE FIND_IN_SET(dept_id, fn_get_child_dept_ids(103)) > 0;

-- 检查用户是否有权限访问某个部门的数据
SELECT fn_check_dept_data_permission(1, 104) as has_permission;
*/

-- =====================================================
-- 数据完整性说明
-- =====================================================

/*
=====================================================
系统架构设计说明（基于部门类型的简化设计）
=====================================================

1. 核心设计理念：
   - 部门决定用户类型：用户类型完全由所在部门的类型决定
   - 层级关系简化：代理层级通过部门parent_id和level字段体现
   - 去除复杂的业务字段：只关注组织架构关系

2. 部门类型说明：
   - dept_type = 1: 系统部门（技术部、运营部等）
   - dept_type = 2: 代理部门（支持1-3级代理层级）
   - dept_type = 3: 买家总账户（可创建子账户）
   - dept_type = 4: 买家子账户（归属于买家总账户）

3. 代理层级说明：
   - level = 1: 一级代理
   - level = 2: 二级代理
   - level = 3: 三级代理
   - level = 0: 非代理部门（系统、买家部门）

4. 用户类型说明：
   - user_type = 1: 后台管理用户（系统部门、代理部门、买家总账户）
   - user_type = 2: 客户端用户（买家子账户）
   - 用户类型由部门类型自动决定，无需单独设置

5. 创建流程说明：
   - 第一步：创建部门（选择对应的部门类型）
   - 第二步：在部门内创建用户（用户自动继承部门类型）
   - 第三步：为用户分配角色（角色必须与部门类型匹配）

6. 权限控制说明：
   - 角色与部门类型绑定：每种角色只能用于特定类型的部门
   - 数据权限：支持全部数据、自定义数据、本部门数据、本部门及以下数据、本人数据

7. 数据权限控制说明：
   - data_scope = 1: 全部数据权限
   - data_scope = 2: 自定义数据权限
   - data_scope = 3: 本部门数据权限
   - data_scope = 4: 本部门及以下数据权限
   - data_scope = 5: 本人数据权限

8. 初始数据说明：
   - 管理员账号：admin / admin123
   - 部门：DeepReach科技总部（系统部门）
   - 角色：超级管理员
   - 示例数据：包含完整的代理层级和买家账户示例

9. 设计优势：
    - 逻辑简单：部门类型决定用户类型，无需复杂的映射关系
    - 层级清晰：通过parent_id天然形成树形结构
    - 权限直观：角色与部门类型绑定，权限控制一目了然
    - 易于扩展：新增类型和层级都比较简单
    - 便于维护：去除了复杂的业务字段
*/