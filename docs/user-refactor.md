# 用户体系重构备忘

> 文档版本：2025-11-04  
> 范围：在现有 DeepReach 系统中移除「部门（dept）」概念、重建用户/权限/资金体系时的影响和建议。

## 1. 当前部门依赖概况

- **数据模型层面**
  - `sys_dept` 主表及所有 `dept_id` 外键列（`sys_user.dept_id`、`sys_role_dept.dept_id`、佣金记录中的 `agent_dept_id` / `buyer_dept_id` 等）。
  - 大量初始化/迁移脚本：`sql/authority.sql`、`sql/role_rebuild.sql`、`sql/accountDesign.sql`、`sql/create_test_data.sql` 等都默认以部门层级构造组织树。
- **Java 代码引用**  
  - `deptId` 仍用于部分树形节点定位，但 `deptType` 相关代码已在核心模块移除，身份判断统一依赖 `UserIdentity` + 角色集合。
  - 核心依赖面调整为 `SysUserServiceImpl`、`AgentCommissionServiceImpl`、`UserDrBalanceServiceImpl`、`BuyerInstanceStatisticsServiceImpl`、`DataScopeCalculator` 等用户树实现；原 `SysDeptServiceImpl`、`DeptUtils` 已删除。
- **业务逻辑依赖**
  - 权限模型：登录态 `LoginUser` 只保留 `deptId` 以兼容历史数据，身份写入角色集合；`JwtTokenUtil` 不再写入 `deptType`，数据范围检查逐步转向用户树 + 身份标签。
  - 资金链路：代理佣金结算、买家余额调账均通过部门层级识别角色。
  - 报表统计：买家实例报表、后台统计接口按部门聚合。

## 2. 重构步骤建议

| 阶段 | 目标 | 关键动作 |
| ---- | ---- | ---- |
| 1. 定义替代模型 | 明确新的组织/角色抽象，确定是否引入“代理树”“买家分组”等实体 | 制定实体关系图，梳理权限规则、资金归属规则 |
| 2. 双轨改造 | 在不破坏线上功能的前提下，引入新模型并写兼容代码 | 逐步为核心服务增加“新模型”判定分支，保留旧字段；编写迁移脚本填充新结构 |
| 3. 切换与清理 | 全量替换接口、移除旧部门依赖 | 回归核心流程，移除 `dept` 字段与相关校验，清理脚本及文档 |

> **关键原则**：先让新模型在旁路运行并获得数据，再逐步把主流程切过去，最后删除老逻辑。

## 3. 影响面拆解

- **接口与前端**
  - `/system/dept`, `/system/user`、代理结算、余额调账等接口的请求/响应结构均含 `deptId`；需要设计新字段或新接口替换。
  - 前端页面、表格展示、“部门树”控件需同步替换为新的组织呈现方式。
- **权限系统**
  - 所有基于部门层级的权限判定必须迁移至新的关联关系（例如基于代理-客户树、角色标签等）。
  - `sys_role_dept`、`DeptUtils`、`DataScopeCalculator` 等工具类需要重写或废弃。
- **资金与结算**
  - 佣金分发、买家余额维护需要从“代理部门层级”转变为“代理用户层级”或其它替代信息。
  - 数据库中的 `agent_dept_id`/`buyer_dept_id` 字段要么转换为新外键，要么废弃。
- **初始化脚本与测试数据**
  - 初始化账号、部门拓扑、批量脚本（含重置密码、导入测试数据等）均需重新设计。

## 4. 数据迁移思路

1. **抽取现有关系**：通过 `sys_user.dept_id` + 历史 `sys_dept.level`（如仍保留）生成用户层级或身份标签，写入过渡表备用。
2. **补充缺省信息**：对未填写负责人、层级的历史数据，提前生成旁路标记（避免上线后出现孤儿数据）。
3. **全量回填与校验**：在新表/字段写入完成后执行对账脚本，确保用户数量、资金余额、佣金等指标一致。
4. **灰度切换**：上线新接口后，按批量迁移用户，观察日志与监控；确认稳定再删除旧字段。

## 5. 风险与验证

- **权限穿透风险**：拆掉部门后，代理/买家层级权限点需要重新验证，建议补充自动化测试覆盖典型场景。
- **财务数据一致性**：任何资金相关表结构调整务必先在影子库演练，确保余额、流水、佣金不丢失。
- **历史报表**：如果保留老数据，需要定义新旧模型下的报表兼容策略，避免统计口径变化导致误差。

## 6. 推荐配套工作

- 制定分支策略与迁移计划，事先开跨团队评审会明确范围、时间线、回滚预案。
- 为新模型准备详细的 ER 图、接口契约、API 文档。
- 建立自动化验证脚本（数据校验、回归用例）和运行手册。

---

如需继续拆分任务，可在此文档后补充「里程碑与负责人」「测试计划」等附录。该文档将随重构进度实时更新。 

## 7. 身份共存改造执行记录（2025-11-05）

- 新增 `UserIdentity` 枚举与 `UserRoleUtils`（路径：`deepreach-common/src/main/java/com/deepreach/common/security`），用角色 `role_key` 映射六类身份。
- `SysUser`、`LoginUser`、`UserVO` 引入基于角色的身份判断方法（`isAdminIdentity`、`isAgentIdentity` 等），原 `isSystemDeptUser()` 等方法改为调用新逻辑，并标注 `@Deprecated` 供后续清理。
- `LoginUser.fromSysUser` 在构建登录态时同步填充身份集合；相关布尔方法默认走角色判断，避免对 `deptType` 的强依赖。
- 文档同步：本章记录完成的改造范围，后续阶段继续更新（数据权限、接口参数等）。
- 2025-11-06：全面删除 `deptType` 字段与兼容逻辑；`LoginUser`、`SysUser`、`SysUserService`、`SysRoleService`、`SysUserMapper` 等全部改为基于角色身份和用户树；控制器及 Web 层校验统一走身份判断。

> 效果：核心领域模型在不移除部门字段的前提下，可以使用角色身份完成业务判断，为下一阶段的数据权限改造铺平道路。

## 8. 用户视图瘦身执行记录（2025-11-05）

- `UserVO` 精简为基础字段（ID、账号信息、状态、父子关系、角色/权限等），去除所有部门/层级展示属性。
- `SysUserMapper.selectCompleteUserInfo` 改为只查询 `sys_user` 基础列，不再 `JOIN sys_dept`。
- `SysUserServiceImpl.getCompleteUserInfo` 适配新的结果结构，仅补充角色和权限集合，避免额外的部门查询。

> 效果：对外接口返回的用户数据与部门模型彻底解耦，前端只需依赖基础信息与角色即可完成展示和权限判断。

## 9. 受影响业务接口清单

以下 REST 接口已切换至“用户层级 + 角色身份”实现，原有 `deptType` 参数、校验或返回字段全部清理，请在联调或回归时重点关注：

- **用户管理 (`/system/user`)**
  - `POST /system/user/list`：列表检索改用身份/用户树过滤，返回精简字段。
  - `GET /system/user/leader/{leaderId}/hierarchy-users`：直属成员视图改为用户层级分组。
  - `POST /system/user`、`POST /system/user/register`：创建逻辑依据创建者身份与父用户限定。
    - **身份对照**：管理员可直接创建总代/一级/二级代理、商家；总代可创建一级/二级代理、商家；一级代理可创建二级代理、商家；二级代理可创建商家；商家可创建子账号。创建者若需要“代为创建”，必须在用户树上是父用户的祖先并显式传入 `parent_id`。
    - 返回的用户数据新增 `invitationCode` 字段，用于前端展示和邀请链路；旧的 `deptDisplayName` 等部门字段已经剔除。
  - `PUT /system/user`、`PUT /system/user/{userId}/status`、`PUT /system/user/{userId}/roles` 等更新类接口：内部校验统一走身份模型。
  - `GET /system/user/{userId}/statistics`：统计结果使用角色分布与层级数据。
  - **权限判定调整**：移除 `system:user:*` 权限位依赖，所有 CUD/查询均通过身份枚举 + 用户树祖先关系判定。
- **统计中心 (`/statistics`)**
  - `GET /statistics/managed-depts`、`/managed-users`、`/agent-levels`、`/buyer-accounts`、`/dashboard`：统计维度由部门改为身份/用户树。
- **余额与扣费 (`/dr`)**
  - `POST /dr/balance/recharge`、`POST /dr/balance/deduct`、`POST /dr/balance/manual-adjust`：买家（总/子账号）判定基于角色集合。
  - `GET /dr/balance/{userId}`、`GET /dr/balance/list` 及相关账单查询接口：返回结构不再包含部门属性，身份标签供前端展示。

> 以上接口的 OpenAPI/前端契约请同步更新，不再传递或依赖 `deptType`、`deptName` 等部门字段；前端需根据 `roles` / `identities` 做权限与展示判断。
- LoginUser 恢复兼容字段（deptId 等）并标记说明，先保障编译通过，后续在迁移到用户树数据权限后再彻底删除。
- 新增 `SysUserService.hasUserHierarchyPermission`，利用用户树和角色身份判断数据访问范围；控制器改为调用该方法，彻底取代旧的部门权限校验。
- 数据权限校验切换到用户树：`SysUserService.hasUserHierarchyPermission` 与 `UserHierarchyService` 完成接入，控制器以及服务层不再依赖 `deptId` 做权限判断。
- 统计模块进入迁移阶段：`SysDeptServiceImpl` 与 `StatisticsController` 正在改造为基于用户树和身份过滤，目前通过 `selectUsersWithinHierarchy` 结果进行汇总，保留 legacy 查询仅供过渡。
- 统计模块继续迁移中：`SysDeptServiceImpl`/`StatisticsController` 已引入用户树范围，但仍保留 `findUsersByDeptLegacy` 兼容接口；下一步将逐项替换统计 SQL，以角色和用户节点为核心聚合维度。
- 数据权限切面完成重写：`DataScopeCalculator` + `DataScopeAspect` 已改为基于用户树+身份判断生成 `user_id` 过滤条件，彻底告别 `dept_id` SQL。
- 统计服务首批切换完成：`SysUserService.getManagedUsersStatistics` 改为直接使用用户树范围与角色身份计数，新增 Mapper `countUsersByRoleKeys`、`countActiveUsersByIds` 以支持基于 `user_id` 的聚合，返回结构补充 `identityBreakdown`、`agentLevelBreakdown`、`unknownUserCount`，后续接口可据此完成前端改版。
- 部门统计接口同步迁移：`SysDeptServiceImpl.getManagedDeptsStatistics`、`getManagedAgentLevelsStatistics`、`getManagedBuyerAccountsStatistics` 改为直接依赖用户层级和角色身份输出统计结果，并暴露 `managedUserIds`、`identityBreakdown` 等新字段，为下一阶段彻底移除部门表做铺垫。
- 代理/买家统计补充用户维度明细：新增 `SysUserMapper.selectUserRoleMappings`、`selectUsersByIds` 支撑身份映射，`getManagedAgentLevelsStatistics` 现汇总分层代理的买家体量、充值合计与佣金总额，同时下钻买家主账号的子账号数量与充值明细，为后续业务接口替换部门 SQL 提供直接数据源。
- 佣金业务去部门化：`AgentCommissionServiceImpl.getAgentCommissionOverview`、`getCommissionAccount` 以及手动调整流程改为基于用户树与角色身份获取代理列表和等级，生成的统计/凭证不再依赖部门信息。
- 用户创建规则重构：`SysUserServiceImpl` 依据创建者身份 + 指定父用户身份限制可创建的角色（管理员纵向创建除 buyer_sub 外所有直属账号，并可代其他用户创建其子代；各级代理只能创建比自身层级更低的代理或商家，且禁止创建 buyer_sub；商家仅能在自身名下创建 buyer_sub），默认将 `parent_id` 绑定到合法的父用户并执行层级校验，彻底摆脱部门校验逻辑。
- 部门接口清理：移除 `SysDeptController`，日志切面改为记录用户身份标签，`UserDetailsServiceImpl` 清理 `system:dept:*` 默认权限，为后续彻底删除 `sys_dept` 相关表/脚本扫清入口。
- 直属用户视图去部门化：`SysUserService.listUsersByLeaderDirectDepts` 现基于用户树直接子节点返回身份分组结果（`UserHierarchyGroupDTO`），`UserIdentity` 更新为匹配六种角色标识（`admin`、`agent_level_1/2/3`、`buyer_main`、`buyer_sub`），前端可使用身份标签替换原部门名称展示。
- 清理遗留依赖：`SysUserServiceImpl` 移除 `SysDeptService` 注入与所有部门 SQL，删除 `selectUsersByDeptType` / `selectUsersByAgentLevel` 等 Mapper，接口侧保留的查询全部改为用户树+角色模型；相关 JavaDoc 与 Mapper XML 均已同步收缩。
- 统计能力迁移：新增 `HierarchyStatisticsService`（`HierarchyStatisticsServiceImpl`）承接原部门统计逻辑，`StatisticsController` 改为依赖该服务输出用户树统计；`SysDeptServiceImpl` 同步裁剪掉所有统计与部门 SQL 辅助方法，仅保留尚未迁移的部门 CRUD 能力。
- 角色模块去部门化：`SysRoleServiceImpl` 不再依赖 `SysDeptService`，角色查询/默认角色/适用性判定全部根据 `UserIdentity` 映射完成，`SysRoleMapper` 新增 `selectRolesByRoleKeys` 支撑身份化查询。
