-- ==========================================
-- DeepReach项目统一密码重置脚本
-- 所有用户密码统一设置为: 123456
-- BCrypt加密值: $2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO
-- 执行日期: 2025-10-29
-- ==========================================

-- 更新所有用户密码为 123456
UPDATE sys_user
SET password = '$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO',
    update_time = NOW(),
    update_by = 'system'
WHERE 1=1;

-- 显示更新结果
SELECT
    COUNT(*) AS total_users_updated,
    '所有用户密码已重置为: 123456' AS message,
    NOW() AS update_time
FROM sys_user;

-- 验证更新结果
SELECT username, nickname, '密码已重置为123456' AS status
FROM sys_user
ORDER BY user_id
LIMIT 10;

SELECT '密码重置完成！所有用户现在可以使用密码 123456 登录系统。' AS result;