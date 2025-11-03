CREATE TABLE `sys_dept` (
                            `dept_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '部门ID',
                            `parent_id` bigint(20) DEFAULT '0' COMMENT '父部门ID',
                            `ancestors` varchar(50) DEFAULT '' COMMENT '祖级列表',
                            `dept_name` varchar(30) DEFAULT '' COMMENT '部门名称',
                            `order_num` int(4) DEFAULT '0' COMMENT '显示顺序',
                            `leader` varchar(20) DEFAULT NULL COMMENT '负责人',
                            `leader_user_id` bigint(20) DEFAULT NULL COMMENT '负责人用户ID',
                            `phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
                            `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
                            `status` char(1) DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
                            `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`dept_id`),
                            KEY `idx_parent_id` (`parent_id`),
                            KEY `idx_status` (`status`),
                            KEY `idx_leader_user_id` (`leader_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='部门表'

CREATE TABLE `sys_role` (
                            `role_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                            `role_name` varchar(30) NOT NULL COMMENT '角色名称',
                            `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
                            `role_sort` int(4) NOT NULL COMMENT '显示顺序',
                            `data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定义数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
                            `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
                            `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
                            `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
                            `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`role_id`),
                            UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表'

CREATE TABLE `sys_user_role` (
                                 `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                 `role_id` bigint(20) NOT NULL COMMENT '角色ID',
                                 PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表'

CREATE TABLE `sys_user` (
                            `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `username` varchar(30) NOT NULL COMMENT '用户账号',
                            `password` varchar(100) NOT NULL COMMENT '密码',
                            `nickname` varchar(30) DEFAULT NULL COMMENT '用户昵称',
                            `real_name` varchar(30) DEFAULT NULL COMMENT '真实姓名',
                            `email` varchar(50) DEFAULT NULL COMMENT '用户邮箱',
                            `phone` varchar(11) DEFAULT NULL COMMENT '手机号码',
                            `gender` char(1) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
                            `avatar` varchar(100) DEFAULT NULL COMMENT '头像地址',
                            `user_type` tinyint(1) DEFAULT '1' COMMENT '用户类型（1后台用户 2客户端用户）',
                            `status` char(1) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
                            `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
                            `login_ip` varchar(128) DEFAULT NULL COMMENT '最后登录IP',
                            `login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`user_id`),
                            UNIQUE KEY `uk_username` (`username`),
                            UNIQUE KEY `uk_email` (`email`),
                            KEY `idx_dept_id` (`dept_id`),
                            KEY `idx_user_type` (`user_type`),
                            KEY `idx_status` (`status`),
                            KEY `idx_user_username` (`username`),
                            KEY `idx_user_email` (`email`),
                            KEY `idx_user_phone` (`phone`),
                            KEY `idx_user_type_status` (`user_type`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=107 DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表'

CREATE TABLE `instance` (
                            `instance_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '实例id',
                            `user_id` int(11) NOT NULL COMMENT '创建用户id',
                            `instance_name` varchar(20) NOT NULL COMMENT '实例名称',
                            `instance_type` char(1) NOT NULL COMMENT '实例类型（0 营销｜ 1 拓客）',
                            `platform_id` int(11) NOT NULL COMMENT '绑定平台id',
                            `character_id` int(11) DEFAULT NULL COMMENT '绑定的AI人设id',
                            `proxy_id` varchar(20) DEFAULT NULL COMMENT '代理',
                            `daily_price` decimal(2,0) DEFAULT NULL COMMENT '每日价格',
                            `last_billing_time` datetime DEFAULT NULL COMMENT '最后计费时间',
                            `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 ｜ 1弃用）',
                            `create_time` datetime NOT NULL COMMENT '创建时间',
                            `create_by` varchar(20) NOT NULL COMMENT '创建者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `update_by` varchar(20) DEFAULT NULL COMMENT '更新者',
                            PRIMARY KEY (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实例表'



