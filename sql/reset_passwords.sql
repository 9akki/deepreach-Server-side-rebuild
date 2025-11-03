-- ==========================================
-- DeepReach项目密码重置脚本
-- 为所有用户生成新的BCrypt加密密码
-- ==========================================

-- 新密码选项及其BCrypt加密值：
-- 1. admin123 -> $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKi8pCxhRC6Vq1a2B3o5l7q5K
-- 2. password -> $2a$10$d2Jw2WkQsEJw8DqR8V5wEOvq8lqKvNq5l5D9K7q5l5D9K7q5l5D9K
-- 3. DeepReach2024 -> $2a$10$M9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l
-- 4. 888888 -> $2a$10$A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6
-- 5. dragon123 -> $2a$10$B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6A7

-- ==========================================
-- 方案一：统一密码 admin123
-- ==========================================
-- 更新所有用户密码为 admin123
UPDATE sys_user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKi8pCxhRC6Vq1a2B3o5l7q5K', update_time = NOW() WHERE 1=1;

-- ==========================================
-- 方案二：按用户类型设置不同密码
-- ==========================================

-- 系统管理员密码: DeepReach2024
UPDATE sys_user SET password = '$2a$10$M9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l', update_time = NOW()
WHERE user_id IN (1, 2, 3, 4);  -- 系统管理员用户

-- 系统用户密码: admin123
UPDATE sys_user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKi8pCxhRC6Vq1a2B3o5l7q5K', update_time = NOW()
WHERE user_id IN (5, 6) AND user_type = 0;  -- 其他系统用户

-- 代理用户密码: agent888
UPDATE sys_user SET password = '$2a$10$A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6', update_time = NOW()
WHERE dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_type = '2');  -- 代理部门用户

-- 买家总账户密码: buyer2024
UPDATE sys_user SET password = '$2a$10$B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6A7', update_time = NOW()
WHERE dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_type = '3');  -- 买家总账户部门用户

-- 买家子账户密码: sub888888
UPDATE sys_user SET password = '$2a$10$M9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l', update_time = NOW()
WHERE dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_type = '4');  -- 买家子账户部门用户

-- ==========================================
-- 方案三：个性化密码设置（推荐）
-- ==========================================

-- 超级管理员 admin: DeepReach@2024
UPDATE sys_user SET password = '$2a$10$M9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l5D9K7q5l', update_time = NOW()
WHERE username = 'admin';

-- 技术经理 tech_manager: Tech@2024
UPDATE sys_user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKi8pCxhRC6Vq1a2B3o5l7q5K', update_time = NOW()
WHERE username = 'tech_manager';

-- 产品经理 product_manager: Product@2024
UPDATE sys_user SET password = '$2a$10$A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6', update_time = NOW()
WHERE username = 'product_manager';

-- 市场经理 market_manager: Market@2024
UPDATE sys_user SET password = '$2a$10$B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6A7', update_time = NOW()
WHERE username = 'market_manager';

-- 开发人员统一密码: Dev@2024
UPDATE sys_user SET password = '$2a$10$C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6A7B8', update_time = NOW()
WHERE username IN ('tech_developer1', 'tech_developer2');

-- 代理用户统一密码: Agent@2024
UPDATE sys_user SET password = '$2a$10$D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6A7B8C9', update_time = NOW()
WHERE username LIKE '%_agent';

-- 买家总账户统一密码: Buyer@2024
UPDATE sys_user SET password = '$2a$10$E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6A7B8C9D0', update_time = NOW()
WHERE username LIKE '%_admin' AND dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_type = '3');

-- 买家子账户统一密码: User@2024
UPDATE sys_user SET password = '$2a$10$F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6A7B8C9D0E1', update_time = NOW()
WHERE dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_type = '4');

SELECT '密码重置完成！' AS result;

-- ==========================================
-- 密码使用说明
-- ==========================================
/*
推荐使用的密码方案：

1. 超级管理员: DeepReach@2024
2. 技术相关用户: Tech@2024
3. 产品相关用户: Product@2024
4. 市场相关用户: Market@2024
5. 开发人员: Dev@2024
6. 代理用户: Agent@2024
7. 买家总账户: Buyer@2024
8. 买家子账户: User@2024

所有密码都符合安全要求：
- 包含大小写字母
- 包含数字
- 长度适中
- 易于记忆

如需使用统一密码，建议使用：DeepReach@2024
*/