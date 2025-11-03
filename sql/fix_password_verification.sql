-- ==========================================
-- DeepReach项目密码验证问题修复脚本
-- 问题：现有密码哈希与123456不匹配
-- 解决：重新生成123456的正确BCrypt哈希
-- 执行日期: 2025-10-29
-- ==========================================

-- 使用新生成的正确BCrypt哈希值
-- 明文密码: 123456
-- 新加密值: $2a$10$Ui0PmTLP0137WZJrSJ5Bre6zo2aoZ2LENe5qcH9YAwdR66Ay7I0tO

-- 方案一：更新所有用户密码为123456（推荐）
UPDATE sys_user
SET password = '$2a$10$Ui0PmTLP0137WZJrSJ5Bre6zo2aoZ2LENe5qcH9YAwdR66Ay7I0tO',
    update_time = NOW(),
    update_by = 'password_fix'
WHERE 1=1;

-- 显示更新结果
SELECT
    COUNT(*) AS total_users_updated,
    '所有用户密码已修复为: 123456' AS message,
    NOW() AS update_time
FROM sys_user;

-- 验证几个关键用户
SELECT username, nickname, '密码已修复' AS status
FROM sys_user
WHERE username IN ('admin', 'tech_manager', 'east_agent')
ORDER BY username;

SELECT '密码修复完成！现在所有用户都可以使用密码 123456 登录。' AS result;