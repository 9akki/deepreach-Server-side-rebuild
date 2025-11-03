-- =====================================================
-- 角色菜单权限绑定SQL
-- 基于部门类型的简化角色权限体系
-- =====================================================

-- 清空现有角色菜单绑定（重新初始化）
DELETE FROM sys_role_menu;

-- =====================================================
-- 1. 角色类型定义和初始化
-- =====================================================

-- 更新角色表，确保 dept_type 字段正确设置
UPDATE sys_role SET dept_type = '1' WHERE role_key IN ('admin', 'system_admin', 'tech_admin', 'ops_admin');
UPDATE sys_role SET dept_type = '2' WHERE role_key = 'agent';
UPDATE sys_role SET dept_type = '3' WHERE role_key = 'buyer_main';
UPDATE sys_role SET dept_type = '4' WHERE role_key = 'buyer_sub';

-- =====================================================
-- 2. 核心角色数据权限设置
-- =====================================================

-- 管理员角色：全部数据权限
UPDATE sys_role SET data_scope = '1' WHERE role_key = 'admin';

-- 系统管理员角色：全部数据权限
UPDATE sys_role SET data_scope = '1' WHERE role_key = 'system_admin';

-- 技术管理员角色：全部数据权限
UPDATE sys_role SET data_scope = '1' WHERE role_key = 'tech_admin';

-- 运营管理员角色：全部数据权限
UPDATE sys_role SET data_scope = '1' WHERE role_key = 'ops_admin';

-- 代理角色：本部门及子部门权限（可以看到下级代理和买家数据）
UPDATE sys_role SET data_scope = '4' WHERE role_key = 'agent';

-- 买家总账户角色：本部门及子部门权限（可以看到自己的子账户数据）
UPDATE sys_role SET data_scope = '4' WHERE role_key = 'buyer_main';

-- 买家子账户角色：本人数据权限
UPDATE sys_role SET data_scope = '5' WHERE role_key = 'buyer_sub';

-- =====================================================
-- 3. 获取菜单ID（假设的菜单ID，实际需要根据你的菜单表调整）
-- =====================================================

-- 系统管理菜单组
SET @system_management = 1;           -- 系统管理
SET @user_management = 100;           -- 用户管理
SET @role_management = 101;           -- 角色管理
SET @menu_management = 102;           -- 菜单管理
SET @dept_management = 103;           -- 部门管理
SET @dict_management = 104;           -- 字典管理
SET @config_management = 105;         -- 参数设置
SET @notice_management = 106;         -- 通知公告

-- 监控管理菜单组
SET @monitor_management = 2;          -- 系统监控
SET @online_monitor = 200;            -- 在线用户
SET @job_monitor = 201;               -- 定时任务
SET @server_monitor = 202;            -- 服务监控
SET @cache_monitor = 203;             -- 缓存监控
SET @log_management = 204;            -- 日志管理

-- 业务管理菜单组
SET @business_management = 3;         -- 业务管理
SET @buyer_management = 300;          -- 买家管理
SET @buyer_list = 301;                -- 买家列表
SET @buyer_stats = 302;               -- 买家统计
SET @subaccount_management = 303;     -- 子账户管理
SET @subaccount_list = 304;           -- 子账户列表
SET @account_usage = 305;             -- 账户使用情况

-- 个人中心菜单组
SET @personal_center = 4;             -- 个人中心
SET @personal_info = 400;             -- 个人信息
SET @change_password = 401;           -- 修改密码
SET @personal_profile = 402;          -- 个人资料

-- =====================================================
-- 4. 管理员角色菜单权限绑定（admin, system_admin, tech_admin, ops_admin）
-- =====================================================

-- 超级管理员（admin）- 所有权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r, sys_menu m
WHERE r.role_key = 'admin' AND m.status = 0;

-- 系统管理员（system_admin）- 除定时任务外的所有系统管理权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r, sys_menu m
WHERE r.role_key = 'system_admin'
  AND m.status = 0
  AND m.menu_id != @job_monitor;  -- 排除定时任务

-- 技术管理员（tech_admin）- 技术相关权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r, sys_menu m
WHERE r.role_key = 'tech_admin'
  AND m.status = 0
  AND m.menu_id IN (
    @system_management, @user_management, @role_management,
    @menu_management, @dept_management,
    @monitor_management, @online_monitor, @server_monitor,
    @cache_monitor, @log_management
  );

-- 运营管理员（ops_admin）- 运营相关权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r, sys_menu m
WHERE r.role_key = 'ops_admin'
  AND m.status = 0
  AND m.menu_id IN (
    @system_management, @user_management, @role_management,
    @dept_management, @notice_management,
    @business_management, @buyer_management, @buyer_list, @buyer_stats,
    @monitor_management, @online_monitor, @log_management
  );

-- =====================================================
-- 5. 代理角色菜单权限绑定（agent）
-- =====================================================

-- 代理角色菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r, sys_menu m
WHERE r.role_key = 'agent'
  AND m.status = 0
  AND m.menu_id IN (
    @business_management, @buyer_management, @buyer_list, @buyer_stats,
    @personal_center, @personal_info, @change_password, @personal_profile
  );

-- =====================================================
-- 6. 买家总账户角色菜单权限绑定（buyer_main）
-- =====================================================

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r, sys_menu m
WHERE r.role_key = 'buyer_main'
  AND m.status = 0
  AND m.menu_id IN (
    @business_management, @subaccount_management, @subaccount_list, @account_usage,
    @personal_center, @personal_info, @change_password, @personal_profile
  );

-- =====================================================
-- 7. 买家子账户角色菜单权限绑定（buyer_sub）
-- =====================================================

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r, sys_menu m
WHERE r.role_key = 'buyer_sub'
  AND m.status = 0
  AND m.menu_id IN (
    @personal_center, @personal_info, @change_password, @personal_profile
  );

-- =====================================================
-- 8. 验证权限绑定结果
-- =====================================================

-- 查看各角色的菜单权限数量
SELECT
    r.role_name,
    r.role_key,
    r.dept_type,
    r.data_scope,
    COUNT(rm.menu_id) as menu_count
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.role_id = rm.role_id
WHERE r.status = '0' AND r.del_flag = '0'
GROUP BY r.role_id, r.role_name, r.role_key, r.dept_type, r.data_scope
ORDER BY r.role_id;

-- =====================================================
-- 9. 客户端接口权限说明
-- =====================================================

/*
客户端接口权限通过Controller的@PreAuthorize注解控制，不需要在菜单表中配置

使用示例：
- 代理接口：@PreAuthorize("hasRole('AGENT')")
- 买家接口：@PreAuthorize("hasRole('BUYER_MAIN') or hasRole('BUYER_SUB')")
- 所有买家类型：@PreAuthorize("hasAnyRole('AGENT', 'BUYER_MAIN', 'BUYER_SUB')")

具体的权限控制逻辑：
1. 角色权限控制菜单访问
2. 数据权限控制数据范围
3. 代码逻辑控制具体操作对象的权限
*/