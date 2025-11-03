-- =====================================================
-- DeepReach 系统权限菜单配置
-- 生成时间: 2025-10-30
-- 说明: 为DeepReach系统的所有权限标识符生成菜单配置
-- 从menu_id 1013开始递增
-- =====================================================

-- 1. DeepReach主菜单
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1013, 'DeepReach管理', 0, 10, 'deepreach', NULL, NULL, 1, 0, 'M', 0, 0, NULL, 'money', 'admin', NOW(), 'DeepReach系统管理目录');

-- 2. 价格配置管理模块
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1014, '价格配置', 1013, 1, 'drPrice', 'deepreach/drPrice/index', NULL, 1, 0, 'C', 0, 0, 'dr:price:list', 'guide', 'admin', NOW(), '价格配置管理菜单');

-- 3. 价格配置按钮权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1015, '价格配置查询', 1014, 1, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:price:query', '#', 'admin', NOW(), ''),
(1016, '价格配置新增', 1014, 2, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:price:add', '#', 'admin', NOW(), ''),
(1017, '价格配置修改', 1014, 3, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:price:edit', '#', 'admin', NOW(), ''),
(1018, '价格配置删除', 1014, 4, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:price:remove', '#', 'admin', NOW(), ''),
(1019, '价格配置导出', 1014, 5, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:price:export', '#', 'admin', NOW(), ''),
(1020, '价格配置导入', 1014, 6, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:price:import', '#', 'admin', NOW(), '');

-- 4. 账单记录管理模块
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1021, '账单记录', 1013, 2, 'drBilling', 'deepreach/drBilling/index', NULL, 1, 0, 'C', 0, 0, 'dr:billing:list', 'documentation', 'admin', NOW(), '账单记录管理菜单');

-- 5. 账单记录按钮权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1022, '账单记录查询', 1021, 1, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:billing:query', '#', 'admin', NOW(), ''),
(1023, '账单记录统计', 1021, 2, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:billing:statistics', '#', 'admin', NOW(), ''),
(1024, '账单总览', 1021, 3, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:billing:overview', '#', 'admin', NOW(), ''),
(1025, '账单记录导出', 1021, 4, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:billing:export', '#', 'admin', NOW(), ''),
(1026, '账单记录编辑', 1021, 5, '#', '', NULL, 1, 0, 'F', 0, 0, 'dr:billing:edit', '#', 'admin', NOW(), '');

-- 6. 账户余额管理模块
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1027, '账户余额', 1013, 3, 'drBalance', 'deepreach/drBalance/index', NULL, 1, 0, 'C', 0, 0, 'system:dr:list', 'moneyCollect', 'admin', NOW(), '账户余额管理菜单');

-- 7. 账户余额按钮权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1028, '账户余额查询', 1027, 1, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:dr:query', '#', 'admin', NOW(), ''),
(1029, '账户充值', 1027, 2, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:dr:recharge', '#', 'admin', NOW(), ''),
(1030, '账户余额统计', 1027, 3, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:dr:statistics', '#', 'admin', NOW(), ''),
(1031, '预扣费操作', 1027, 4, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:dr:preDeduct', '#', 'admin', NOW(), '');

-- 8. 账单明细管理模块
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1032, '账单明细', 1013, 4, 'drBill', 'deepreach/drBill/index', NULL, 1, 0, 'C', 0, 0, 'system:dr:bill:list', 'list', 'admin', NOW(), '账单明细管理菜单');

-- 9. 账单明细按钮权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1033, '账单明细查询', 1032, 1, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:dr:bill:query', '#', 'admin', NOW(), '');

-- 10. 实例管理模块
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1034, '实例管理', 1013, 5, 'instance', 'deepreach/instance/index', NULL, 1, 0, 'C', 0, 0, 'system:instance:list', 'server', 'admin', NOW(), '实例管理菜单');

-- 11. 实例管理按钮权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1035, '实例查询', 1034, 1, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:instance:query', '#', 'admin', NOW(), ''),
(1036, '实例新增', 1034, 2, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:instance:add', '#', 'admin', NOW(), ''),
(1037, '实例修改', 1034, 3, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:instance:edit', '#', 'admin', NOW(), ''),
(1038, '实例删除', 1034, 4, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:instance:remove', '#', 'admin', NOW(), ''),
(1039, '实例计费', 1034, 5, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:instance:billing', '#', 'admin', NOW(), '');

-- 12. 系统管理补充按钮权限（补充已有的用户管理、角色管理、部门管理的缺失权限）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1040, '用户重置密码', 100, 6, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:user:resetPwd', '#', 'admin', NOW(), ''),
(1041, '角色分配数据', 101, 5, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:role:dataScope', '#', 'admin', NOW(), ''),
(1042, '部门分配数据', 103, 5, '#', '', NULL, 1, 0, 'F', 0, 0, 'system:dept:dataScope', '#', 'admin', NOW(), '');

-- =====================================================
-- 权限菜单配置说明
-- =====================================================

/*
权限层级结构：

1013 DeepReach管理 (主菜单 M)
├── 1014 价格配置 (菜单 C, 权限: dr:price:list)
│   ├── 1015 价格配置查询 (按钮 F, 权限: dr:price:query)
│   ├── 1016 价格配置新增 (按钮 F, 权限: dr:price:add)
│   ├── 1017 价格配置修改 (按钮 F, 权限: dr:price:edit)
│   ├── 1018 价格配置删除 (按钮 F, 权限: dr:price:remove)
│   ├── 1019 价格配置导出 (按钮 F, 权限: dr:price:export)
│   └── 1020 价格配置导入 (按钮 F, 权限: dr:price:import)
├── 1021 账单记录 (菜单 C, 权限: dr:billing:list)
│   ├── 1022 账单记录查询 (按钮 F, 权限: dr:billing:query)
│   ├── 1023 账单记录统计 (按钮 F, 权限: dr:billing:statistics)
│   ├── 1024 账单总览 (按钮 F, 权限: dr:billing:overview)
│   ├── 1025 账单记录导出 (按钮 F, 权限: dr:billing:export)
│   └── 1026 账单记录编辑 (按钮 F, 权限: dr:billing:edit)
├── 1027 账户余额 (菜单 C, 权限: system:dr:list)
│   ├── 1028 账户余额查询 (按钮 F, 权限: system:dr:query)
│   ├── 1029 账户充值 (按钮 F, 权限: system:dr:recharge)
│   ├── 1030 账户余额统计 (按钮 F, 权限: system:dr:statistics)
│   └── 1031 预扣费操作 (按钮 F, 权限: system:dr:preDeduct)
├── 1032 账单明细 (菜单 C, 权限: system:dr:bill:list)
│   └── 1033 账单明细查询 (按钮 F, 权限: system:dr:bill:query)
└── 1034 实例管理 (菜单 C, 权限: system:instance:list)
    ├── 1035 实例查询 (按钮 F, 权限: system:instance:query)
    ├── 1036 实例新增 (按钮 F, 权限: system:instance:add)
    ├── 1037 实例修改 (按钮 F, 权限: system:instance:edit)
    ├── 1038 实例删除 (按钮 F, 权限: system:instance:remove)
    └── 1039 实例计费 (按钮 F, 权限: system:instance:billing)

补充系统管理权限：
├── 100 用户管理 (已有)
│   └── 1040 用户重置密码 (按钮 F, 权限: system:user:resetPwd)
├── 101 角色管理 (已有)
│   └── 1041 角色分配数据 (按钮 F, 权限: system:role:dataScope)
└── 103 部门管理 (已有)
    └── 1042 部门分配数据 (按钮 F, 权限: system:dept:dataScope)

已覆盖的权限标识符列表：
1. dr:price:list, dr:price:query, dr:price:add, dr:price:edit, dr:price:remove, dr:price:export, dr:price:import
2. dr:billing:list, dr:billing:query, dr:billing:statistics, dr:billing:overview, dr:billing:export, dr:billing:edit
3. system:dr:list, system:dr:query, system:dr:recharge, system:dr:bill:list, system:dr:bill:query, system:dr:statistics, system:dr:preDeduct
4. system:instance:list, system:instance:query, system:instance:add, system:instance:edit, system:instance:remove, system:instance:billing
5. system:user:resetPwd (补充权限)
6. system:role:dataScope (补充权限)
7. system:dept:dataScope (补充权限)

注意：系统基础权限 (system:user:list, system:user:add, system:user:edit, system:user:remove, system:user:export,
                    system:role:list, system:role:query, system:role:add, system:role:edit, system:role:remove,
                    system:dept:list, system:dept:query, system:dept:add, system:dept:edit, system:dept:remove)
已在authority.sql中配置，此处不再重复添加。
*/

-- =====================================================
-- 角色菜单权限分配建议
-- =====================================================

/*
建议为以下角色分配对应菜单权限：

1. 超级管理员 (role_id=1): 拥有所有菜单权限
2. 系统管理员 (role_id=2): 拥有DeepReach管理下所有菜单权限
3. 技术管理员 (role_id=3): 拥有实例管理、价格配置权限
4. 运营管理员 (role_id=4): 拥有账户余额、账单记录权限
5. 代理角色 (role_id=10-12): 拥有账户余额、账单记录查询权限
6. 买家角色 (role_id=20-21): 拥有账户余额查询、账单记录查询权限

角色菜单分配示例：
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1013), (1, 1014), (1, 1015), (1, 1016), (1, 1017), (1, 1018), (1, 1019), (1, 1020),
(1, 1021), (1, 1022), (1, 1023), (1, 1024), (1, 1025), (1, 1026),
(1, 1027), (1, 1028), (1, 1029), (1, 1030), (1, 1031),
(1, 1032), (1, 1033),
(1, 1034), (1, 1035), (1, 1036), (1, 1037), (1, 1038), (1, 1039),
(1, 1040), (1, 1041), (1, 1042);
*/

-- =====================================================
-- 执行完成提示
-- =====================================================

SELECT 'DeepReach权限菜单配置完成，已创建30个菜单权限项，涵盖所有控制器中使用的权限标识符' AS message;