-- ==========================================
-- 测试用户数据 - 密码统一为 123456 (BCrypt加密)
-- BCrypt加密后的密码: $2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO
-- ==========================================

-- 系统用户 (dept_type = 1) - 密码: 123456
INSERT INTO sys_user (user_id, username, password, nickname, real_name, email, phone, gender, avatar, user_type, status, dept_id, parent_user_id, login_ip, login_time, create_by, create_time, remark) VALUES
(2, 'tech_manager', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '技术总监', '张三', 'tech@deepreach.com', '13800138001', '1', '', 0, '0', 2, NULL, '127.0.0.1', NOW(), 'system', NOW(), '技术研发部负责人'),
(3, 'product_manager', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '产品总监', '李四', 'product@deepreach.com', '13800138002', '2', '', 0, '0', 3, NULL, '127.0.0.1', NOW(), 'system', NOW(), '产品运营部负责人'),
(4, 'market_manager', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '市场总监', '王五', 'market@deepreach.com', '13800138003', '1', '', 0, '0', 4, NULL, '127.0.0.1', NOW(), 'system', NOW(), '市场营销部负责人'),
(5, 'tech_developer1', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '前端开发', '赵六', 'dev1@deepreach.com', '13800138011', '1', '', 0, '0', 2, NULL, '127.0.0.1', NOW(), 'system', NOW(), '前端开发工程师'),
(6, 'tech_developer2', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '后端开发', '钱七', 'dev2@deepreach.com', '13800138012', '1', '', 0, '0', 2, NULL, '127.0.0.1', NOW(), 'system', NOW(), '后端开发工程师');

-- 代理用户 (dept_type = 2) - 密码: 123456
INSERT INTO sys_user (user_id, username, password, nickname, real_name, email, phone, gender, avatar, user_type, status, dept_id, parent_user_id, login_ip, login_time, create_by, create_time, remark) VALUES
(11, 'east_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '华东代理', '周代理', 'east@agent.com', '13900139001', '1', '', 0, '0', 11, NULL, '127.0.0.1', NOW(), 'system', NOW(), '华东地区一级代理'),
(12, 'north_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '华北代理', '吴代理', 'north@agent.com', '13900139002', '1', '', 0, '0', 12, NULL, '127.0.0.1', NOW(), 'system', NOW(), '华北地区一级代理'),
(13, 'south_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '华南代理', '郑代理', 'south@agent.com', '13900139003', '1', '', 0, '0', 13, NULL, '127.0.0.1', NOW(), 'system', NOW(), '华南地区一级代理'),
(21, 'shanghai_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '上海代理', '上海经理', 'shanghai@agent.com', '13900139101', '1', '', 0, '0', 21, NULL, '127.0.0.1', NOW(), 'system', NOW(), '上海二级代理'),
(22, 'jiangsu_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '江苏代理', '江苏经理', 'jiangsu@agent.com', '13900139102', '1', '', 0, '0', 22, NULL, '127.0.0.1', NOW(), 'system', NOW(), '江苏二级代理'),
(23, 'beijing_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '北京代理', '北京经理', 'beijing@agent.com', '13900139201', '1', '', 0, '0', 23, NULL, '127.0.0.1', NOW(), 'system', NOW(), '北京二级代理'),
(31, 'suzhou_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '苏州代理', '苏州经理', 'suzhou@agent.com', '13900140101', '1', '', 0, '0', 31, NULL, '127.0.0.1', NOW(), 'system', NOW(), '苏州三级代理'),
(32, 'hangzhou_agent', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '杭州代理', '杭州经理', 'hangzhou@agent.com', '13900140102', '1', '', 0, '0', 32, NULL, '127.0.0.1', NOW(), 'system', NOW(), '杭州三级代理');

-- 买家总账户用户 (dept_type = 3) - 密码: 123456
INSERT INTO sys_user (user_id, username, password, nickname, real_name, email, phone, gender, avatar, user_type, status, dept_id, parent_user_id, login_ip, login_time, create_by, create_time, remark) VALUES
(101, 'alibaba_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '阿里巴巴', '马云', 'alibaba@buyer.com', '13700137001', '1', '', 0, '0', 101, NULL, '127.0.0.1', NOW(), 'system', NOW(), '阿里巴巴集团总账户'),
(102, 'tencent_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '腾讯科技', '马化腾', 'tencent@buyer.com', '13700137002', '1', '', 0, '0', 102, NULL, '127.0.0.1', NOW(), 'system', NOW(), '腾讯科技总账户'),
(103, 'baidu_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '百度科技', '李彦宏', 'baidu@buyer.com', '13700137003', '1', '', 0, '0', 103, NULL, '127.0.0.1', NOW(), 'system', NOW(), '百度科技总账户'),
(104, 'bytedance_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '字节跳动', '张一鸣', 'bytedance@buyer.com', '13700137004', '1', '', 0, '0', 104, NULL, '127.0.0.1', NOW(), 'system', NOW(), '字节跳动总账户'),
(105, 'jd_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '京东集团', '刘强东', 'jd@buyer.com', '13700137101', '1', '', 0, '0', 105, NULL, '127.0.0.1', NOW(), 'system', NOW(), '京东集团总账户'),
(106, 'meituan_admin', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '美团点评', '王兴', 'meituan@buyer.com', '13700137102', '1', '', 0, '0', 106, NULL, '127.0.0.1', NOW(), 'system', NOW(), '美团点评总账户');

-- 买家子账户用户 (dept_type = 4) - 密码: 123456
INSERT INTO sys_user (user_id, username, password, nickname, real_name, email, phone, gender, avatar, user_type, status, dept_id, parent_user_id, login_ip, login_time, create_by, create_time, remark) VALUES
(201, 'alibaba_tech', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '阿里技术', '阿里技术负责人', 'alibaba-tech@buyer.com', '13600136001', '1', '', 0, '0', 201, 101, '127.0.0.1', NOW(), 'system', NOW(), '阿里巴巴技术部子账户'),
(202, 'alibaba_market', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '阿里市场', '阿里市场负责人', 'alibaba-market@buyer.com', '13600136002', '2', '', 0, '0', 202, 101, '127.0.0.1', NOW(), 'system', NOW(), '阿里巴巴市场部子账户'),
(203, 'alibaba_ops', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '阿里运营', '阿里运营负责人', 'alibaba-ops@buyer.com', '13600136003', '2', '', 0, '0', 203, 101, '127.0.0.1', NOW(), 'system', NOW(), '阿里巴巴运营部子账户'),
(204, 'tencent_game', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '腾讯游戏', '腾讯游戏负责人', 'tencent-game@buyer.com', '13600136101', '1', '', 0, '0', 204, 102, '127.0.0.1', NOW(), 'system', NOW(), '腾讯游戏业务子账户'),
(205, 'tencent_cloud', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '腾讯云', '腾讯云负责人', 'tencent-cloud@buyer.com', '13600136102', '1', '', 0, '0', 205, 102, '127.0.0.1', NOW(), 'system', NOW(), '腾讯云服务子账户'),
(206, 'baidu_search', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '百度搜索', '百度搜索负责人', 'baidu-search@buyer.com', '13600136201', '1', '', 0, '0', 206, 103, '127.0.0.1', NOW(), 'system', NOW(), '百度搜索业务子账户'),
(207, 'douyin_ops', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '抖音运营', '抖音业务负责人', 'douyin@buyer.com', '13600136301', '2', '', 0, '0', 207, 104, '127.0.0.1', NOW(), 'system', NOW(), '抖音业务子账户'),
(208, 'jd_retail', '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO', '京东零售', '京东零售负责人', 'jd-retail@buyer.com', '13600136401', '1', '', 0, '0', 208, 105, '127.0.0.1', NOW(), 'system', NOW(), '京东零售子账户');

SELECT '所有测试用户创建完成，密码统一为: 123456' AS result;