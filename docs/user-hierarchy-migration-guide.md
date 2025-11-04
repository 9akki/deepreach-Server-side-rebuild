# 用户层级替换部门迁移指南

> 文档版本：2025-11-06  
> 适用范围：在 DeepReach 服务端仓库中使用“用户 ID 层级树 + 角色体系”替代 `sys_dept` 历史逻辑。  
> 更新时间：`deptType` 字段及 `SysDeptService` 已从代码中移除，仍保留的部门表仅承担历史数据/展示用途。

---

## 0. 术语约定

| 名称 | 含义 | 参考 |
| --- | --- | --- |
| 用户树 | 由 `UserHierarchyTreeDTO` 表示的用户父子关系缓存 | `deepreach-common/src/main/java/com/deepreach/common/core/domain/dto/UserHierarchyTreeDTO.java` |
| 层级缓存 KEY | `user:hierarchy:tree` | `UserHierarchyTreeBuilder.USER_TREE_CACHE_KEY` |
| 角色身份 | `admin`、`agent_level_1`、`agent_level_2`、`agent_level_3`、`buyer`、`sub_buyer` 六种角色 | `sys_role.role_key` |
| 旧部门字段 | `sys_user.dept_id`、`sys_dept.dept_type/level/ancestors` 等 | 各实体/mapper |

---

## 1. 现状与代码定位

需要彻底替换的对象按模块划分如下（仅列出核心类，更多引用可通过 `rg "dept"` 搜索）：

- **实体与模型**
  - `SysUser`、`LoginUser` 已改为仅使用角色身份；`deptType` 兼容字段删除，保留 `deptId` 以兼容历史展示。
  - `UserVO`、`UserInfo` 等 VO/DTO 仅暴露基础字段与角色集合，历史 `deptType` 字段标记为废弃。

- **服务与工具**
  - `SysUserServiceImpl` 现全部基于用户树与身份标签实现创建/授权；旧 `SysDeptService`、`DeptUtils` 已删除。
  - `SecurityUtils`、`DataScopeCalculator` 已迁移至用户树，继续跟进数据范围余项即可。

- **持久层**
  - `SysUserMapper.xml`：多处 `JOIN sys_dept`（`deepreach-common/src/main/resources/mapper/SysUserMapper.xml:23` 起）。
  - 统计/报表 SQL（如 `sql/authority.sql`、`sql/create_test_data.sql`）初始化部门树。

---

## 2. 迁移总流程

迁移建议划分为五个阶段，按顺序执行，阶段之间保持代码可运行且可回滚。

### 阶段 1：建立角色身份层（共存期）

1. **新增身份枚举与工具**
   - 创建 `UserIdentity` 枚举（建议路径：`deepreach-common/src/main/java/com/deepreach/common/security/enums/`）。
   - 编写 `UserRoleUtils`，提供 `hasRole(LoginUser, RoleKey...)`、`resolveIdentities(Set<String>)` 等方法。
2. **改写实体身份判断**
   - 在 `SysUser`、`LoginUser`、`UserVO`、`UserInfo` 中新增基于角色的布尔方法：
     ```java
     public boolean isAgentLevel2() {
         return roles != null && roles.contains("agent_level_2");
     }
     ```
   - 将 `isSystemDeptUser()` 等方法内部实现改为调用新方法，同时标注 `@Deprecated`，为后续删除做准备。
3. **服务层补齐身份信息**
   - `SysUserServiceImpl.setSimplifiedUserInfo`、`LoginUser.fromSysUser` 在装载用户时，把角色集合转换成身份枚举并缓存到对象上（方便后续判断）。
   - 对依赖 `deptType` 的业务校验（如子账号创建，`SysUserServiceImpl.validateParentUserForSubAccount`）改为检查身份枚举。

> **验收**：编译通过；原有接口仍按旧逻辑运行；新增的身份工具在单测里覆盖六种角色映射。

### 阶段 2：用用户树替换部门数据权限

1. **扩展缓存模型**
   - 在 `UserHierarchyTreeDTO` 增加以下字段（若暂时不写入，可先留 TODO）：
     - `Map<Long, List<Long>> descendantMapping`（祖孙关系）。
     - `Map<Long, List<Long>> ancestorPath`（父链，用于快速判断上下级关系）。
   - 在 `UserHierarchyTreeBuilder.build` 中填充上述字段，保证缓存一次构建即可满足查询需求。
2. **抽象用户层级服务**
   - 新建 `UserHierarchyService`（建议路径 `deepreach-common/src/main/java/com/deepreach/common/core/service`）：
     - 方法示例：`List<Long> findDescendants(Long userId)`、`boolean isAncestor(Long ancestorId, Long targetId)`。
     - 实现类从 Redis 缓存读取，若缓存为空调用 `SysUserService.rebuildUserHierarchyCache()`。
3. **迁移数据权限逻辑**
  - `SysUserServiceImpl.hasUserHierarchyPermission` 使用 `UserHierarchyService.isAncestor` 判断访问范围，权限位校验已移除。
   - `SysUserServiceImpl.selectUsersByDeptId` 等方法改写为 `selectUsersWithinHierarchy(Long rootUserId, SysUser filter)`，SQL 不再 `JOIN sys_dept`，改为：
     - 方案 A：使用 MySQL 递归 CTE（若库版本支持）。
     - 方案 B：在 Java 层根据缓存提供的 `descendantMapping` 过滤 userId 集合，再批量查询。
   - 保留旧方法签名以避免编译报错，但在内部调用新逻辑，并在 javadoc 中标注 “@Deprecated 部门逻辑待删除”。
4. **控制器与权限注解**
   - 将 `@PreAuthorize("@ss.hasDeptPermission(#deptId)")` 改为 `@PreAuthorize("@ss.hasUserHierarchyPermission(#userId)")`。
   - 更新 `SecurityUtils.hasPermission` 相关工具，避免传入 `deptId`。

> **验收**：核心用户列表/详情接口使用新的过滤逻辑仍返回相同结果；缓存刷新后权限判断准确无误；日志中不再出现 `JOIN sys_dept` 的 SQL。

### 阶段 3：接口与前端字段迁移

1. **VO/DTO 字段替换**
   - 在 `UserVO` / `DeptUserGroupDTO` 等类中新增字段 `parentUserId`、`hierarchyPath`、`primaryRoleKey`，用于前端展示。
   - 将 `deptId`、`deptName`、`deptType` 标注为 `@Deprecated` 并在返回时置空或映射到新的字段。
2. **API 响应调整**
   - `SysUserController` 等接口新增查询参数 `rootUserId` 或 `identityRole`，替代 `deptId`。
   - 对前端依赖的“部门树”接口，新增 `/system/user-tree` 返回用户层级；前端逐步过渡后下线 `/system/dept/tree`。
3. **文档同步**
   - 更新 `docs/admin-dept-user-management.md`、`docs/user-refactor.md` 等文档，说明字段替换和兼容策略。

> **验收**：前端切换到新数据结构后功能正常；旧接口即使暂时保留，也通过内部转换保证数据正确。

### 阶段 4：删除部门依赖

1. **清理代码**
   - 删除 `SysDeptService`、`SysDeptMapper`、`SysDept` 实体以及相关调用（注意保留必要的历史数据导入工具）。
   - 移除 `SysUser`/`LoginUser`/`UserVO` 中的部门字段与方法。
   - 清理 `SecurityUtils`、`DataScope` 等工具中针对 `dept` 的分支。
2. **数据库与脚本**
   - 迁移或删除 `sys_dept` 表及外键（`sys_user.dept_id` 等）。
   - 更新 `sql/authority.sql`、`sql/create_test_data.sql` 等初始化脚本，改为根据角色定义用户层级。
3. **缓存与配置**
   - 删除 Redis 中与部门相关的 Key（如存在 `dept:tree:*`），仅保留 `user:hierarchy:tree`。

> **验收**：代码全局搜索 `dept` 仅剩业务无关的历史文本；数据库不再包含部门表；CI/CD 构建无警告。

### 阶段 5：回归与上线

1. **测试矩阵**
   - 角色覆盖：六种角色分别登录，验证菜单、权限、数据可见范围。
   - 层级权限：挑选多级代理、买家总/子账号组合进行 CRUD 测试。
   - 资金/结算：对比迁移前后余额、佣金、计费记录。
   - 缓存刷新：验证用户增删改后 Redis 层级树实时更新（观察 `UserHierarchyCacheInitializer` 日志）。
2. **监控与回滚**
   - 上线前编写监控脚本：检测登录失败率、403/500 异常、核心接口耗时。
   - 如出现问题，准备回滚步骤：重新启用旧分支、执行数据恢复脚本、恢复 `sys_dept` 快照。

---

## 3. 参考代码与改动指引

| 任务 | 参考文件/方法 |
| --- | --- |
| 构建用户树缓存 | `UserHierarchyCacheInitializer` (`deepreach-web/src/main/java/com/deepreach/web/initializer/UserHierarchyCacheInitializer.java`) |
| 刷新缓存接口 | `SysUserService.rebuildUserHierarchyCache()` (`deepreach-common/src/main/java/com/deepreach/common/core/service/SysUserService.java:124-129`) |
| 部门身份判断 | `LoginUser` (`deepreach-common/src/main/java/com/deepreach/common/core/domain/model/LoginUser.java:352-402`)、`SysUser` (`:318-370`) |
| 数据权限 | `SysUserServiceImpl.hasUserHierarchyPermission` (`deepreach-common/src/main/java/com/deepreach/common/core/service/impl/SysUserServiceImpl.java`) |
| 用户列表按部门过滤 | `SysUserMapper.selectUsersByDeptId` (`deepreach-common/src/main/resources/mapper/SysUserMapper.xml:127` 起) |
| 子账号校验 | `SysUserServiceImpl.validateParentUserForSubAccount` (`:1160-1225`) |

改造时建议对照上述路径逐项替换，并在代码注释中标记迁移进展（例如：`// TODO(dept-removal): replace with hierarchy-based check`）。

---

## 4. 测试与验收建议

1. **单元测试**
   - 新建 `UserRoleUtilsTest`、`UserHierarchyServiceTest`，覆盖角色映射和层级判断。
   - 对 `SysUserServiceImpl` 的核心方法（创建、更新、删除、状态变更）增加缓存刷新断言。
2. **集成测试**
   - 使用 `SpringBootTest` 模拟不同角色登录，验证数据权限接口返回。
   - 编写 REST Assured 用例，覆盖用户树查询、新旧接口兼容场景。
3. **性能与回归**
   - 压测用户列表接口，确保移除部门 JOIN 后性能不下降；必要时在缓存层增加批量过滤器或预计算。
   - 对比迁移前后统计报表，确认总量与口径一致。

---

## 5. 里程碑建议

| 里程碑 | 完成标志 | 预计耗时 |
| --- | --- | --- |
| M1 身份共存上线 | 新增工具与布尔方法上线，旧接口保持可用 | ~2 人日 |
| M2 数据权限切换 | 关键服务改用用户树过滤，缓存稳定 | ~4 人日 |
| M3 前端/接口迁移 | API/前端完成字段替换，部门接口下线 | ~3 人日 |
| M4 部门清理 | 代码与数据库移除部门逻辑 | ~2 人日 |
| M5 上线回归 | 测试覆盖 + 监控告警配置完成 | ~1 人日 |

> 时间估算针对熟悉代码的 2 人小组，可根据实际人力调整。

---

## 6. 附录：常见问题

- **问：历史报表如何回溯部门信息？**  
  若必须保留历史口径，可在迁移前将 `sys_dept` 关键字段导出到历史表，如 `dept_snapshot_{date}`，供报表使用。

- **问：新角色体系如何与外部系统对接？**  
  建议提供 `GET /system/user-identities` 接口输出用户ID、主角色、父级ID，作为外部同步源。

- **问：用户并无父ID时如何判定根节点？**  
  在缓存构建时，`UserHierarchyTreeBuilder` 会将 `parent_user_id` 为空或无效的用户视为根节点；如需强制单根，可在导入脚本中指定。

---

> 如需了解最新进展，请同步更新 `docs/user-refactor.md` 与本指南的里程碑状态。建议在每次阶段交付后补充变更记录和测试报告链接。
