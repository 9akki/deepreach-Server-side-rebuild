-- ==========================================
-- 组织架构测试数据创建脚本
-- 基于部门类型的用户体系
-- ==========================================

-- 首先确保表结构中有login_time字段
-- ALTER TABLE sys_user ADD COLUMN login_time datetime NULL COMMENT '最后登录时间';

-- 清理现有测试数据（可选）
-- DELETE FROM sys_user_role WHERE user_id > 1;
-- DELETE FROM sys_user WHERE user_id > 1;
-- DELETE FROM sys_dept WHERE dept_id > 1;

-- ==========================================
-- 1. 部门数据创建
-- ==========================================

-- 系统部门 (dept_type = 1)
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, leader_user_id, phone, email, status, del_flag, dept_type, level, create_by, create_time) VALUES
(1, 0, '0', 'DeepReach科技', 1, '系统管理员', 1, '13800138000', 'admin@deepreach.com', '0', '0', '1', 0, 'system', NOW()),  -- 根部门
(2, 1, '0,1', '技术研发部', 1, '技术总监', 2, '13800138001', 'tech@deepreach.com', '0', '0', '1', 1, 'system', NOW()),
(3, 1, '0,1', '产品运营部', 2, '产品总监', 3, '13800138002', 'product@deepreach.com', '0', '0', '1', 1, 'system', NOW()),
(4, 1, '0,1', '市场营销部', 3, '市场总监', 4, '13800138003', 'market@deepreach.com', '0', '0', '1', 1, 'system', NOW());

-- 一级代理部门 (dept_type = 2, level = 1)
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, leader_user_id, phone, email, status, del_flag, dept_type, level, create_by, create_time) VALUES
(11, 1, '0,1', '华东代理', 1, '华东代理经理', 11, '13900139001', 'east@agent.com', '0', '0', '2', 1, 'system', NOW()),
(12, 1, '0,1', '华北代理', 2, '华北代理经理', 12, '13900139002', 'north@agent.com', '0', '0', '2', 1, 'system', NOW()),
(13, 1, '0,1', '华南代理', 3, '华南代理经理', 13, '13900139003', 'south@agent.com', '0', '0', '2', 1, 'system', NOW());

-- 二级代理部门 (dept_type = 2, level = 2)
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, leader_user_id, phone, email, status, del_flag, dept_type, level, create_by, create_time) VALUES
(21, 11, '0,1,11', '上海代理', 1, '上海代理经理', 21, '13900139101', 'shanghai@agent.com', '0', '0', '2', 2, 'system', NOW()),
(22, 11, '0,1,11', '江苏代理', 2, '江苏代理经理', 22, '13900139102', 'jiangsu@agent.com', '0', '0', '2', 2, 'system', NOW()),
(23, 12, '0,1,12', '北京代理', 1, '北京代理经理', 23, '13900139201', 'beijing@agent.com', '0', '0', '2', 2, 'system', NOW()),
(24, 13, '0,1,13', '广东代理', 1, '广东代理经理', 24, '13900139301', 'guangdong@agent.com', '0', '0', '2', 2, 'system', NOW());

-- 三级代理部门 (dept_type = 2, level = 3)
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, leader_user_id, phone, email, status, del_flag, dept_type, level, create_by, create_time) VALUES
(31, 21, '0,1,11,21', '苏州代理', 1, '苏州代理经理', 31, '13900140101', 'suzhou@agent.com', '0', '0', '2', 3, 'system', NOW()),
(32, 21, '0,1,11,21', '杭州代理', 2, '杭州代理经理', 32, '13900140102', 'hangzhou@agent.com', '0', '0', '2', 3, 'system', NOW()),
(33, 23, '0,1,12,23', '天津代理', 1, '天津代理经理', 33, '13900140201', 'tianjin@agent.com', '0', '0', '2', 3, 'system', NOW());

-- 买家总账户部门 (dept_type = 3)
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, leader_user_id, phone, email, status, del_flag, dept_type, level, create_by, create_time) VALUES
(101, 11, '0,1,11', '阿里巴巴集团', 1, '阿里巴巴采购总监', 101, '13700137001', 'alibaba@buyer.com', '0', '0', '3', 0, 'system', NOW()),
(102, 11, '0,1,11', '腾讯科技', 2, '腾讯采购总监', 102, '13700137002', 'tencent@buyer.com', '0', '0', '3', 0, 'system', NOW()),
(103, 12, '0,1,12', '百度科技', 1, '百度采购总监', 103, '13700137003', 'baidu@buyer.com', '0', '0', '3', 0, 'system', NOW()),
(104, 13, '0,1,13', '字节跳动', 1, '字节跳动采购总监', 104, '13700137004', 'bytedance@buyer.com', '0', '0', '3', 0, 'system', NOW()),
(105, 21, '0,1,11,21', '京东集团', 1, '京东采购总监', 105, '13700137101', 'jd@buyer.com', '0', '0', '3', 0, 'system', NOW()),
(106, 21, '0,1,11,21', '美团点评', 2, '美团采购总监', 106, '13700137102', 'meituan@buyer.com', '0', '0', '3', 0, 'system', NOW());

-- 买家子账户部门 (dept_type = 4)
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, leader_user_id, phone, email, status, del_flag, dept_type, level, create_by, create_time) VALUES
(201, 101, '0,1,11,101', '阿里巴巴-技术部', 1, '阿里巴巴技术负责人', 201, '13600136001', 'alibaba-tech@buyer.com', '0', '0', '4', 0, 'system', NOW()),
(202, 101, '0,1,11,101', '阿里巴巴-市场部', 2, '阿里巴巴市场负责人', 202, '13600136002', 'alibaba-market@buyer.com', '0', '0', '4', 0, 'system', NOW()),
(203, 101, '0,1,11,101', '阿里巴巴-运营部', 3, '阿里巴巴运营负责人', 203, '13600136003', 'alibaba-ops@buyer.com', '0', '0', '4', 0, 'system', NOW()),
(204, 102, '0,1,11,102', '腾讯-游戏业务', 1, '腾讯游戏负责人', 204, '13600136101', 'tencent-game@buyer.com', '0', '0', '4', 0, 'system', NOW()),
(205, 102, '0,1,11,102', '腾讯-云服务', 2, '腾讯云服务负责人', 205, '13600136102', 'tencent-cloud@buyer.com', '0', '0', '4', 0, 'system', NOW()),
(206, 103, '0,1,12,103', '百度-搜索业务', 1, '百度搜索负责人', 206, '13600136201', 'baidu-search@buyer.com', '0', '0', '4', 0, 'system', NOW()),
(207, 104, '0,1,13,104', '字节跳动-抖音', 1, '抖音业务负责人', 207, '13600136301', 'douyin@buyer.com', '0', '0', '4', 0, 'system', NOW()),
(208, 105, '0,1,11,21,105', '京东-零售', 1, '京东零售负责人', 208, '13600136401', 'jd-retail@buyer.com', '0', '0', '4', 0, 'system', NOW());

-- ==========================================
-- 2. 角色数据创建
-- ==========================================

-- 系统角色
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark) VALUES
(1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'system', NOW(), '超级管理员拥有所有权限'),
(2, '系统管理员', 'system_admin', 2, '1', 1, 1, '0', '0', 'system', NOW(), '系统管理员角色'),
(3, '技术管理员', 'tech_admin', 3, '1', 1, 1, '0', '0', 'system', NOW(), '技术部门管理员'),
(4, '代理管理员', 'agent', 4, '2', 1, 1, '0', '0', 'system', NOW(), '代理统一角色'),
(5, '买家总账户', 'buyer_main', 5, '3', 1, 1, '0', '0', 'system', NOW(), '买家总账户角色'),
(6, '买家子账户', 'buyer_sub', 6, '5', 1, 1, '0', '0', 'system', NOW(), '买家子账户角色'),
(7, '普通用户', 'user', 7, '5', 1, 1, '0', '0', 'system', NOW(), '普通用户角色');

-- ==========================================
-- 3. 用户数据创建
-- 密码统一为: 123456 (BCrypt加密后的值: $2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO)
-- ==========================================

-- 系统用户 (dept_type = 1)
INSERT INTO sys_user (user_id, username, password, nickname, real_name, email, phone, gender, avatar, user_type, status, dept_id, parent_user_id, login_ip, login_time, create_by, create_time, remark) VALUES
(1, 'admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '超级管理员', '系统管理员', 'admin@deepreach.com', '13800138000', '1', '', 1, '0', 1, NULL, '127.0.0.1', NOW(), 'system', NOW(), '系统超级管理员'),
(2, 'tech_manager', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '技术总监', '张三', 'tech@deepreach.com', '13800138001', '1', '', 1, '0', 2, NULL, '127.0.0.1', NOW(), 'system', NOW(), '技术研发部负责人'),
(3, 'product_manager', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '产品总监', '李四', 'product@deepreach.com', '13800138002', '2', '', 1, '0', 3, NULL, '127.0.0.1', NOW(), 'system', NOW(), '产品运营部负责人'),
(4, 'market_manager', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '市场总监', '王五', 'market@deepreach.com', '13800138003', '1', '', 1, '0', 4, NULL, '127.0.0.1', NOW(), 'system', NOW(), '市场营销部负责人'),
(5, 'tech_developer1', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '前端开发', '赵六', 'dev1@deepreach.com', '13800138011', '1', '', 1, '0', 2, NULL, '127.0.0.1', NOW(), 'system', NOW(), '前端开发工程师'),
(6, 'tech_developer2', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '后端开发', '钱七', 'dev2@deepreach.com', '13800138012', '1', '', 1, '0', 2, NULL, '127.0.0.1', NOW(), 'system', NOW(), '后端开发工程师');

-- 代理用户 (dept_type = 2)
INSERT INTO sys_user (user_id, username, password, nickname, real_name, email, phone, gender, avatar, user_type, status, dept_id, parent_user_id, login_ip, login_time, create_by, create_time, remark) VALUES
(11, 'east_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '华东代理', '周代理', 'east@agent.com', '13900139001', '1', '', 1, '0', 11, NULL, '127.0.0.1', NOW(), 'system', NOW(), '华东地区一级代理'),
(12, 'north_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '华北代理', '吴代理', 'north@agent.com', '13900139002', '1', '', 1, '0', 12, NULL, '127.0.0.1', NOW(), 'system', NOW(), '华北地区一级代理'),
(13, 'south_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '华南代理', '郑代理', 'south@agent.com', '13900139003', '1', '', 1, '0', 13, NULL, '127.0.0.1', NOW(), 'system', NOW(), '华南地区一级代理'),
(21, 'shanghai_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '上海代理', '上海经理', 'shanghai@agent.com', '13900139101', '1', '', 1, '0', 21, NULL, '127.0.0.1', NOW(), 'system', NOW(), '上海二级代理'),
(22, 'jiangsu_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '江苏代理', '江苏经理', 'jiangsu@agent.com', '13900139102', '1', '', 1, '0', 22, NULL, '127.0.0.1', NOW(), 'system', NOW(), '江苏二级代理'),
(23, 'beijing_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '北京代理', '北京经理', 'beijing@agent.com', '13900139201', '1', '', 1, '0', 23, NULL, '127.0.0.1', NOW(), 'system', NOW(), '北京二级代理'),
(31, 'suzhou_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '苏州代理', '苏州经理', 'suzhou@agent.com', '13900140101', '1', '', 1, '0', 31, NULL, '127.0.0.1', NOW(), 'system', NOW(), '苏州三级代理'),
(32, 'hangzhou_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '杭州代理', '杭州经理', 'hangzhou@agent.com', '13900140102', '1', '', 1, '0', 32, NULL, '127.0.0.1', NOW(), 'system', NOW(), '杭州三级代理');

-- 买家总账户用户 (dept_type = 3)
INSERT INTO sys_user (user_id, username, password, nickname, real_name, email, phone, gender, avatar, user_type, status, dept_id, parent_user_id, login_ip, login_time, create_by, create_time, remark) VALUES
(101, 'alibaba_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '阿里巴巴', '马云', 'alibaba@buyer.com', '13700137001', '1', '', 1, '0', 101, NULL, '127.0.0.1', NOW(), 'system', NOW(), '阿里巴巴集团总账户'),
(102, 'tencent_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '腾讯科技', '马化腾', 'tencent@buyer.com', '13700137002', '1', '', 1, '0', 102, NULL, '127.0.0.1', NOW(), 'system', NOW(), '腾讯科技总账户'),
(103, 'baidu_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '百度科技', '李彦宏', 'baidu@buyer.com', '13700137003', '1', '', 1, '0', 103, NULL, '127.0.0.1', NOW(), 'system', NOW(), '百度科技总账户'),
(104, 'bytedance_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '字节跳动', '张一鸣', 'bytedance@buyer.com', '13700137004', '1', '', 1, '0', 104, NULL, '127.0.0.1', NOW(), 'system', NOW(), '字节跳动总账户'),
(105, 'jd_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '京东集团', '刘强东', 'jd@buyer.com', '13700137101', '1', '', 1, '0', 105, NULL, '127.0.0.1', NOW(), 'system', NOW(), '京东集团总账户'),
(106, 'meituan_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '美团点评', '王兴', 'meituan@buyer.com', '13700137102', '1', '', 1, '0', 106, NULL, '127.0.0.1', NOW(), 'system', NOW(), '美团点评总账户');

-- 买家子账户用户 (dept_type = 4)
INSERT INTO sys_user (user_id, username, password, nickname, real_name, email, phone, gender, avatar, user_type, status, dept_id, parent_user_id, login_ip, login_time, create_by, create_time, remark) VALUES
(201, 'alibaba_tech', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '阿里技术', '阿里技术负责人', 'alibaba-tech@buyer.com', '13600136001', '1', '', 2, '0', 201, 101, '127.0.0.1', NOW(), 'system', NOW(), '阿里巴巴技术部子账户'),
(202, 'alibaba_market', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '阿里市场', '阿里市场负责人', 'alibaba-market@buyer.com', '13600136002', '2', '', 2, '0', 202, 101, '127.0.0.1', NOW(), 'system', NOW(), '阿里巴巴市场部子账户'),
(203, 'alibaba_ops', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '阿里运营', '阿里运营负责人', 'alibaba-ops@buyer.com', '13600136003', '2', '', 2, '0', 203, 101, '127.0.0.1', NOW(), 'system', NOW(), '阿里巴巴运营部子账户'),
(204, 'tencent_game', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '腾讯游戏', '腾讯游戏负责人', 'tencent-game@buyer.com', '13600136101', '1', '', 2, '0', 204, 102, '127.0.0.1', NOW(), 'system', NOW(), '腾讯游戏业务子账户'),
(205, 'tencent_cloud', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '腾讯云', '腾讯云负责人', 'tencent-cloud@buyer.com', '13600136102', '1', '', 2, '0', 205, 102, '127.0.0.1', NOW(), 'system', NOW(), '腾讯云服务子账户'),
(206, 'baidu_search', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '百度搜索', '百度搜索负责人', 'baidu-search@buyer.com', '13600136201', '1', '', 2, '0', 206, 103, '127.0.0.1', NOW(), 'system', NOW(), '百度搜索业务子账户'),
(207, 'douyin_ops', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '抖音运营', '抖音业务负责人', 'douyin@buyer.com', '13600136301', '2', '', 2, '0', 207, 104, '127.0.0.1', NOW(), 'system', NOW(), '抖音业务子账户'),
(208, 'jd_retail', '$2a.10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '京东零售', '京东零售负责人', 'jd-retail@buyer.com', '13600136401', '1', '', 2, '0', 208, 105, '127.0.0.1', NOW(), 'system', NOW(), '京东零售子账户');

-- ==========================================
-- 4. 用户角色关联数据
-- ==========================================

-- 超级管理员
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 系统管理员
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2), (3, 3), (4, 2);

-- 系统部门普通用户
INSERT INTO sys_user_role (user_id, role_id) VALUES (5, 7), (6, 7);

-- 代理用户（统一使用agent角色）
INSERT INTO sys_user_role (user_id, role_id) VALUES
(11, 4), (12, 4), (13, 4), (21, 4), (22, 4), (23, 4), (24, 4),
(31, 4), (32, 4), (33, 4);

-- 买家总账户用户
INSERT INTO sys_user_role (user_id, role_id) VALUES
(101, 5), (102, 5), (103, 5), (104, 5), (105, 5), (106, 5);

-- 买家子账户用户
INSERT INTO sys_user_role (user_id, role_id) VALUES
(201, 6), (202, 6), (203, 6), (204, 6), (205, 6), (206, 6), (207, 6), (208, 6);

-- ==========================================
-- 5. 菜单权限数据（基础菜单）
-- ==========================================

-- 系统管理菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(1, '系统管理', 0, 1, 'system', NULL, 1, 0, 'M', '0', '0', NULL, 'system', 'admin', NOW(), '系统管理目录'),
(100, '用户管理', 1, 1, 'user', 'system/user/index', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', NOW(), '用户管理菜单'),
(101, '角色管理', 1, 2, 'role', 'system/role/index', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', NOW(), '角色管理菜单'),
(102, '部门管理', 1, 3, 'dept', 'system/dept/index', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', NOW(), '部门管理菜单');

-- 监控统计菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2, '监控统计', 0, 2, 'monitor', NULL, 1, 0, 'M', '0', '0', NULL, 'monitor', 'admin', NOW(), '监控统计目录'),
(200, '在线用户', 2, 1, 'online', 'monitor/online/index', 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 'admin', NOW(), '在线用户菜单');

-- 工具菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(3, '系统工具', 0, 3, 'tool', NULL, 1, 0, 'M', '0', '0', NULL, 'tool', 'admin', NOW(), '系统工具目录'),
(300, '表单构建', 3, 1, 'build', 'tool/build/index', 1, 0, 'C', '0', '0', 'tool:build:list', 'build', 'admin', NOW(), '表单构建菜单');

-- ==========================================
-- 6. 角色菜单权限关联
-- ==========================================

-- 超级管理员拥有所有权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu;

-- 系统管理员拥有系统管理权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 1), (2, 100), (2, 101), (2, 102);

-- 技术管理员拥有技术相关权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (3, 1), (3, 100), (3, 102);

-- 代理管理员拥有基础查看权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (4, 2), (4, 200);

-- 买家总账户拥有订单和统计查看权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (5, 2), (5, 200);

-- 买家子账户拥有基本操作权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (6, 3), (6, 300);

-- ==========================================
-- 7. 测试数据说明
-- ==========================================

/*
测试账户信息：

【系统管理员账户】
- 用户名: admin
- 密码: 123456
- 说明: 超级管理员，拥有所有权限

【系统部门测试账户】
- 用户名: tech_manager
- 密码: 123456
- 说明: 技术总监，属于技术研发部

- 用户名: product_manager
- 密码: 123456
- 说明: 产品总监，属于产品运营部

【代理测试账户】
- 用户名: east_agent
- 密码: 123456
- 说明: 华东地区一级代理

- 用户名: shanghai_agent
- 密码: 123456
- 说明: 上海地区二级代理

- 用户名: suzhou_agent
- 密码: 123456
- 说明: 苏州地区三级代理

【买家总账户测试】
- 用户名: alibaba_admin
- 密码: 123456
- 说明: 阿里巴巴集团总账户，可以创建子账户

- 用户名: jd_admin
- 密码: 123456
- 说明: 京东集团总账户，可以创建子账户

【买家子账户测试】
- 用户名: alibaba_tech
- 密码: 123456
- 说明: 阿里巴巴技术部子账户

- 用户名: douyin_ops
- 密码: 123456
- 说明: 字节跳动抖音业务子账户

组织架构层级：
- 系统部门 → 技术研发部/产品运营部/市场营销部
- 一级代理 → 华东代理/华北代理/华南代理
- 二级代理 → 上海代理/江苏代理/北京代理/广东代理
- 三级代理 → 苏州代理/杭州代理/天津代理
- 买家总账户 → 阿里巴巴/腾讯/百度/字节跳动/京东/美团
- 买家子账户 → 各个买家的业务部门

*/

SELECT '测试数据创建完成！' AS result;