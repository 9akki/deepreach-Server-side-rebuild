-- ==========================================
-- DeepReach项目密码重置脚本 (最终版)
-- 使用真实生成的BCrypt加密值
-- 执行日期: 2025-10-29
-- ==========================================

-- 推荐密码方案：
-- 1. 超级管理员: DeepReach@2024
-- 2. 技术相关用户: Tech@2024
-- 3. 产品相关用户: Product@2024
-- 4. 市场相关用户: Market@2024
-- 5. 开发人员: Dev@2024
-- 6. 代理用户: Agent@2024
-- 7. 买家总账户: Buyer@2024
-- 8. 买家子账户: User@2024

-- ==========================================
-- 方案一：按用户类型设置不同密码 (推荐)
-- ==========================================

-- 超级管理员 (user_id = 1) - DeepReach@2024
-- 加密值: $2a$10$qQ0P98Is111sWAwHAem4l.Qrvx.6Z/etSxfKv6MRWWm/B.cXEKnn.
UPDATE sys_user
SET password = '$2a$10$qQ0P98Is111sWAwHAem4l.Qrvx.6Z/etSxfKv6MRWWm/B.cXEKnn.',
    update_time = NOW(),
    update_by = 'system'
WHERE user_id = 1;

-- 技术经理 (tech_manager) - Tech@2024
-- 加密值: $2a$10$.Ktp2Itu2vd.UV3wuhp4deZT6FhfbpwI/Qn.vOoSGVVmhiKpba1QC
UPDATE sys_user
SET password = '$2a$10$.Ktp2Itu2vd.UV3wuhp4deZT6FhfbpwI/Qn.vOoSGVVmhiKpba1QC',
    update_time = NOW(),
    update_by = 'system'
WHERE username = 'tech_manager';

-- 产品经理 (product_manager) - Product@2024
-- 加密值: $2a$10$XEjvfgN09F3fPSWJRRgUAuztz0.f4fpqW6lq9PwbrwPuO1YUx6CBq
UPDATE sys_user
SET password = '$2a$10$XEjvfgN09F3fPSWJRRgUAuztz0.f4fpqW6lq9PwbrwPuO1YUx6CBq',
    update_time = NOW(),
    update_by = 'system'
WHERE username = 'product_manager';

-- 市场经理 (market_manager) - Market@2024
-- 加密值: $2a$10$7b3hdMEWmg2KiqdEmgBsPOljHGqe7IDT2rux2QAgXmmHXaAJQ9JeS
UPDATE sys_user
SET password = '$2a$10$7b3hdMEWmg2KiqdEmgBsPOljHGqe7IDT2rux2QAgXmmHXaAJQ9JeS',
    update_time = NOW(),
    update_by = 'system'
WHERE username = 'market_manager';

-- 开发人员 - Dev@2024
-- 加密值: $2a$10$2iKBqjb16yD6jJhDaM6LM.vOlpcLnkp3zSsJrW/h4C9NFZx3YXPpy
UPDATE sys_user
SET password = '$2a$10$2iKBqjb16yD6jJhDaM6LM.vOlpcLnkp3zSsJrW/h4C9NFZx3YXPpy',
    update_time = NOW(),
    update_by = 'system'
WHERE username IN ('tech_developer1', 'tech_developer2');

-- 所有代理用户 - Agent@2024
-- 加密值: $2a$10$X8Umc7ZRBn6G.K.KRqjbO.DOR0GFnWflxRxvV12H8DFlJX96zmJK.
UPDATE sys_user
SET password = '$2a$10$X8Umc7ZRBn6G.K.KRqjbO.DOR0GFnWflxRxvV12H8DFlJX96zmJK.',
    update_time = NOW(),
    update_by = 'system'
WHERE username LIKE '%_agent' OR username LIKE '%agent%';

-- 买家总账户用户 - Buyer@2024
-- 加密值: $2a$10$IXN2rIKMVoq6RLQIASp.U.bvluWQ2KLcz.4vGGM./xnlzK0DXxbMC
UPDATE sys_user
SET password = '$2a$10$IXN2rIKMVoq6RLQIASp.U.bvluWQ2KLcz.4vGGM./xnlzK0DXxbMC',
    update_time = NOW(),
    update_by = 'system'
WHERE username LIKE '%_admin' AND dept_id IN (
    SELECT dept_id FROM sys_dept WHERE dept_type = '3'
);

-- 买家子账户用户 - User@2024
-- 加密值: $2a$10$/7YhHs9HDlqUCreeku.G7OqZ5K8EmRgVKZeoI8aailWR7PGxO0FQy
UPDATE sys_user
SET password = '$2a$10$/7YhHs9HDlqUCreeku.G7OqZ5K8EmRgVKZeoI8aailWR7PGxO0FQy',
    update_time = NOW(),
    update_by = 'system'
WHERE dept_id IN (
    SELECT dept_id FROM sys_dept WHERE dept_type = '4'
);

-- ==========================================
-- 方案二：统一简单密码 (备选)
-- 如果想要所有用户使用同一个密码，请执行以下语句
-- 统一密码: admin123
-- 加密值: $2a$10$Fpr6V.lvizZME./Lf5BH1.kix0HnRJvwejfDVGcA.xsE/Qmbg3jPm
-- ==========================================

-- UPDATE sys_user
-- SET password = '$2a$10$Fpr6V.lvizZME./Lf5BH1.kix0HnRJvwejfDVGcA.xsE/Qmbg3jPm',
--     update_time = NOW(),
--     update_by = 'system'
-- WHERE 1=1;

SELECT '密码重置完成！' AS result;
SELECT '请使用以下密码登录系统：' AS info;

-- ==========================================
-- 密码清单 (方案一)
-- ==========================================

-- 超级管理员: DeepReach@2024
-- 技术经理: Tech@2024
-- 产品经理: Product@2024
-- 市场经理: Market@2024
-- 开发人员: Dev@2024
-- 代理用户: Agent@2024
-- 买家总账户: Buyer@2024
-- 买家子账户: User@2024

SELECT '所有密码都已更新为2024年度版本' AS status;