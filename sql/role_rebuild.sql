-- =====================================================
-- 角色表重建和角色用户绑定SQL
-- 基于部门类型的简化角色体系
-- =====================================================

-- =====================================================
-- 1. 备份现有数据（可选）
-- =====================================================

-- 创建备份表（如果需要备份数据）
CREATE TABLE IF NOT EXISTS sys_role_backup_20251028 AS SELECT * FROM sys_role;
CREATE TABLE IF NOT EXISTS sys_user_role_backup_20251028 AS SELECT * FROM sys_user_role;

-- =====================================================
-- 2. 清空现有角色相关表
-- =====================================================

-- 删除角色菜单绑定
DELETE FROM sys_role_menu;

-- 删除用户角色绑定
DELETE FROM sys_user_role;

-- 清空角色表
DELETE FROM sys_role;

-- 重置角色ID自增（如果使用自增）
-- ALTER TABLE sys_role AUTO_INCREMENT = 1;

-- =====================================================
-- 3. 创建新的角色数据
-- =====================================================

-- 系统角色（部门类型 = 1）
INSERT INTO sys_role (
    role_id, role_name, role_key, role_sort, data_scope,
    menu_check_strictly, dept_check_strictly, status, del_flag,
    create_by, create_time, update_by, update_time, remark, dept_type
) VALUES
-- 超级管理员
(1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', NOW(), 'admin', NOW(), '超级管理员，拥有所有权限', '1'),

-- 系统管理员
(2, '系统管理员', 'system_admin', 2, '1', 1, 1, '0', '0', 'admin', NOW(), 'admin', NOW(), '系统管理员，除定时任务外的所有系统管理权限', '1'),

-- 技术管理员
(3, '技术管理员', 'tech_admin', 3, '1', 1, 1, '0', '0', 'admin', NOW(), 'admin', NOW(), '技术部管理员，负责系统和技术相关功能', '1'),

-- 运营管理员
(4, '运营管理员', 'ops_admin', 4, '1', 1, 1, '0', '0', 'admin', NOW(), 'admin', NOW(), '运营部管理员，负责业务和运营功能', '1'),

-- 代理角色（部门类型 = 2）
-- 代理（统一角色，层级在代码中控制）
(10, '代理', 'agent', 10, '4', 1, 1, '0', '0', 'admin', NOW(), 'admin', NOW(), '代理角色，具体的层级权限在代码中控制', '2'),

-- 买家角色（部门类型 = 3, 4）
-- 买家总账号
(20, '买家总账号', 'buyer_main', 20, '4', 1, 1, '0', '0', 'admin', NOW(), 'admin', NOW(), '买家总账号角色，可以创建和管理买家子账户', '3'),

-- 买家子账号
(21, '买家子账号', 'buyer_sub', 21, '5', 1, 1, '0', '0', 'admin', NOW(), 'admin', NOW(), '买家子账号角色，只能管理个人信息', '4');

-- =====================================================
-- 4. 重建用户角色绑定
-- =====================================================

-- 超级管理员（用户ID = 1）绑定超级管理员角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT 1, 1
WHERE EXISTS (SELECT 1 FROM sys_user WHERE user_id = 1 AND status = '0');

-- 如果需要为其他系统用户绑定角色，可以在这里添加
-- 例如：为技术部用户绑定技术管理员角色
/*
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, 3  -- 技术管理员角色ID
FROM sys_user u
JOIN sys_dept d ON u.dept_id = d.dept_id
WHERE d.dept_type = '1'  -- 系统部门
  AND d.dept_name LIKE '%技术%'  -- 技术部门
  AND u.status = '0'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur
    WHERE ur.user_id = u.user_id
  );
*/

-- 为代理部门用户绑定统一的代理角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, 10  -- 代理角色ID
FROM sys_user u
JOIN sys_dept d ON u.dept_id = d.dept_id
WHERE d.dept_type = '2'  -- 代理部门
  AND u.status = '0'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur
    WHERE ur.user_id = u.user_id
  );

-- 为买家总账户用户绑定角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, 20  -- 买家总账号角色ID
FROM sys_user u
JOIN sys_dept d ON u.dept_id = d.dept_id
WHERE d.dept_type = '3'  -- 买家总账户部门
  AND u.status = '0'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur
    WHERE ur.user_id = u.user_id
  );

-- 为买家子账户用户绑定角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, 21  -- 买家子账号角色ID
FROM sys_user u
JOIN sys_dept d ON u.dept_id = d.dept_id
WHERE d.dept_type = '4'  -- 买家子账户部门
  AND u.status = '0'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur
    WHERE ur.user_id = u.user_id
  );

-- =====================================================
-- 5. 验证角色创建结果
-- =====================================================

-- 查看所有角色
SELECT
    role_id, role_name, role_key, role_sort, data_scope, dept_type,
    status, del_flag, create_time, remark
FROM sys_role
ORDER BY role_sort, role_id;

-- 查看角色绑定统计
SELECT
    r.role_name, r.role_key, r.dept_type, r.data_scope,
    COUNT(ur.user_id) as user_count,
    GROUP_CONCAT(ur.user_id) as user_ids
FROM sys_role r
LEFT JOIN sys_user_role ur ON r.role_id = ur.role_id
WHERE r.status = '0' AND r.del_flag = '0'
GROUP BY r.role_id, r.role_name, r.role_key, r.dept_type, r.data_scope
ORDER BY r.role_sort, r.role_id;

-- 查看各部门的角色分配情况
SELECT
    d.dept_name, d.dept_type, d.level,
    COUNT(DISTINCT u.user_id) as dept_user_count,
    COUNT(DISTINCT ur.role_id) as role_count,
    GROUP_CONCAT(DISTINCT r.role_key) as assigned_roles
FROM sys_dept d
LEFT JOIN sys_user u ON d.dept_id = u.dept_id AND u.status = '0'
LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
LEFT JOIN sys_role r ON ur.role_id = r.role_id
WHERE d.del_flag = '0'
GROUP BY d.dept_id, d.dept_name, d.dept_type, d.level
ORDER BY d.dept_type, d.level, d.dept_id;

-- =====================================================
-- 6. 手动调整角色绑定（如果需要）
-- =====================================================

/*
-- 如果需要为特定用户手动分配角色，可以使用以下语句：

-- 为特定用户分配角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (用户ID, 角色ID);

-- 移除用户的特定角色
DELETE FROM sys_user_role WHERE user_id = 用户ID AND role_id = 角色ID;

-- 重新分配用户的所有角色
DELETE FROM sys_user_role WHERE user_id = 用户ID;
INSERT INTO sys_user_role (user_id, role_id) VALUES (用户ID, 角色ID1), (用户ID, 角色ID2);

-- 例如：为用户ID=100的用户分配二级代理角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (100, 11);
*/

-- =====================================================
-- 7. 脚本执行完成提示
-- =====================================================

SELECT '角色表重建完成！' as message;
SELECT '请检查角色绑定情况，并根据需要进行手动调整。' as notice;
SELECT '接下来请执行 role_menu_binding.sql 文件来绑定菜单权限。' as next_step;