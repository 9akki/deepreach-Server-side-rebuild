# 管理员部门与用户创建接口说明

> ⚠️ **重要提示**：自 2025-11-06 起，`sys_dept.dept_type` 字段与 `/system/dept` 部门接口已下线，账户体系完全转向“用户树 + 角色身份”模型。本页保留为历史参考，新的权限与创建规则请参阅《docs/user-hierarchy-migration-guide.md》。

本文档保留近期针对部门与用户创建流程的约束更新，便于回顾旧版本后台管理及接口联调逻辑。

## 1. 新增/调整的接口

- `POST /system/dept`：创建部门。
  - 系统后台入口：`/system/dept`，对应 `SysDeptController#add`。
- `POST /system/user`：创建用户。
  - 系统后台入口：`/system/user`，对应 `SysUserController#add`。

## 2. `POST /system/dept`

| 字段 | 类型 | 是否必填 | 说明 |
| ---- | ---- | -------- | ---- |
| `deptName` | string | 是 | 部门名称。 |
| `deptType` | string | **已移除** | 旧版部门类型字段；现由用户角色身份替代。 |
| `parentId` | long | 管理员必填，代理可选 | 上级部门 ID。管理员（系统/超管）必须显式指定；代理缺省时默认为自身部门，也可指定名下子层级部门。 |
| `leaderUserId` | long | 跨部门必填 | 新部门负责人用户 ID，须隶属 `parentId` 指定的部门。管理员及“越级”创建场景必须填写。 |
| `level` | int | 代理部门建议显式传递 | 旧版代理层级。保留用于兼容历史数据，未来将由用户层级树推导。 |
| `orderNum` | int | 否 | 排序号，默认 0。 |
| `status` | string | 否 | `0` 正常、`1` 停用，默认 `0`。 |
| `remark` | string | 否 | 备注。 |

**角色行为摘要：**

- **管理员（系统部门/超管）**  
  - 可为任意已有部门创建子部门，需同时提供 `parentId` 与 `leaderUserId`。  
  - 可创建代理（`deptType=2`，当请求体包含 `level` 时按原值入库，否则根据父部门推导层级）、买家总账号（`deptType=3`）及买家子账号（`deptType=4`）。
- **一级、二级代理**  
  - 默认在本部门下创建；若需为下级（含孙级）部门新增子部门，需显式传入 `parentId` 与 `leaderUserId`，系统会校验层级关系。  
  - 允许创建代理子部门（层级自动 +1，最高至三级）及买家总部门；禁止创建买家子账号部门。
- **买家总账号** 不再具备部门创建能力；买家子账号保持不可创建。

负责人未显式指定时，仅当代理在自身部门下建子部门才会自动回填为当前用户。

> **提醒**：自 2025-11 起，`POST /system/dept` 将优先使用请求体中的 `level` 字段填充代理部门层级，不再覆写为默认值；请前端在创建一级/二级/三级代理时分别传入 `1`、`2`、`3`，以避免层级错位。

**能力矩阵（部门）**

| 操作人 | 可创建的部门类型 | 是否可越级指定父部门 | 是否可创建买家子部门 |
| ------ | ---------------- | ---------------------- | -------------------- |
| 超级管理员 / 系统部门管理员 | 代理、买家总账号、买家子账号 | 是（需提供 `parentId`、`leaderUserId`） | 是 |
| 一级代理 | 二级代理、三级代理、买家总账号 | 是，仅限自身及子层级部门 | 否 |
| 二级代理 | 三级代理、买家总账号 | 是，仅限自身及子层级部门 | 否 |
| 三级代理 | 买家总账号 | 是，仅限自身及子层级部门 | 否 |
| 买家总账号 / 买家子账号 | 无权限 | - | - |

## 3. `POST /system/user`

| 字段 | 类型 | 是否必填 | 说明 |
| ---- | ---- | -------- | ---- |
| `username` | string | 是 | 3–20 位，字母/数字/下划线。 |
| `password` | string | 是 | 6–20 位。 |
| `deptId` | long | 是 | 所属部门 ID，决定用户类型与权限。 |
| `nickname` | string | 否 | 昵称。 |
| `realName` | string | 否 | 真实姓名。 |
| `email` | string | 否 | 邮箱。 |
| `phone` | string | 否 | 手机号。 |
| `gender` | string | 否 | `1` 男、`2` 女、`0` 未知，默认 `2`（未知）。 |
| `status` | string | 否 | `0` 正常、`1` 停用，默认 `0`。 |
| `parentUserId` | long | 子账号部门需提供 | 当 `deptId` 对应部门 `deptType=4`（买家子账号）时，父用户必须等于该部门的 `leaderUserId`。未显式传时系统会自动填充为负责人 ID。 |

**角色行为摘要：**

- **管理员（系统部门/超管）**  
  - 可在任意部门创建用户（含买家子账号）。  
  - 创建子账号用户时强制校验父用户与部门负责人一致，且父用户必须归属买家总部门。
- **代理用户（任意层级）**  
  - 可在自身及其子层级部门内创建用户（系统自动按 `deptId` 赋予角色类型）。  
  - 禁止为买家子账号部门建用户，其余逻辑与管理员一致。  
- **买家总账号用户** 仅可在自己管理的部门（需为负责人）创建用户；买家子账号无法创建用户。

**能力矩阵（用户）**

| 操作人 | 可创建的用户所属部门 | 是否可创建买家子账号用户 | 备注 |
| ------ | -------------------- | -------------------------- | ---- |
| 超级管理员 / 系统部门管理员 | 任意部门 | 是（需满足负责人 & 父用户校验） | 禁止新建超级管理员账号 |
| 代理用户（任意层级） | 自身及其子层级部门 | 否 | 部门需位于自身管理范围内 |
| 买家总账号 | 自身负责的买家总部门 | 否 | 必须为目标部门负责人 |
| 买家子账号 | 无权限 | 否 | - |

## 4. `GET /system/user/leader/{leaderId}/direct-dept-users`

- 对应 `SysUserController#listDirectDeptUsersByLeader`
- 按部门分组返回某个负责人直接管理的部门及其用户列表，不包含下级子部门

**请求参数**

| 路径参数 | 类型 | 是否必填 | 说明 |
| -------- | ---- | -------- | ---- |
| `leaderId` | long | 是 | 部门负责人的用户 ID |

**响应结构**

```json
[
  {
    "deptId": 123,
    "deptName": "一级代理A",
    "deptType": "2",
    "level": 1,
    "users": [
      {
        "userId": 456,
        "username": "agent_user",
        "nickname": "代理用户",
        "realName": "张三",
        "phone": "138****",
        "email": "user@example.com",
        "status": "0",
        "userType": 1
      }
    ]
  }
]
```

**权限说明**

- 拥有 `system:user:list` 权限的管理员可查询任意负责人。
- 非管理员仅可查询自身作为负责人的部门用户。

## 5. `POST /agent/commission/manual-adjust`

- 对应 `AgentCommissionController#manualAdjust`
- 手动调整代理的佣金账户：`amount` 为正表示调增佣金，负表示扣减；扣减金额若超过可用佣金则直接归零，不触发返佣

**请求体（示例）**

```json
{
  "agentUserId": 20001,
  "amount": -1200.00,
  "remark": "活动奖励调增"
}
```

**字段说明**

| 字段 | 类型 | 是否必填 | 说明 |
| ---- | ---- | -------- | ---- |
| `agentUserId` | long | 是 | 代理用户 ID |
| `amount` | decimal | 是 | 调账金额：正数调增、负数扣减、0 无效 |
| `remark` | string | 否 | 备注 / 调整原因 |

**响应**

- 返回更新后的 `AgentCommissionAccountDTO`，包含当前可用、冻结、累计结算等佣金字段，`earnedCommissionInRange` 置 0。

**校验要点**

- 仅支持状态正常的代理佣金账户；系统会先校验账户是否存在，若不存在将自动创建后再执行调账。
- 调增时同步累计 (`totalCommission`) 与可用佣金；调减仅影响可用佣金，扣减金额不足时归零。

## 6. `POST /dr/balance/manual-adjust`

- 对应 `DrBalanceController#manualAdjust`
- 商家（买家总账号）DR 余额手动调账：`amount` 为正表示调增，负表示扣减；扣减金额超过当前余额时会将余额直接归零，不触发代理返佣

**请求体（示例）**

```json
{
  "userId": 10001,
  "amount": 5000.00,
  "remark": "活动赠送"
}
```

| 字段 | 类型 | 是否必填 | 说明 |
| ---- | ---- | -------- | ---- |
| `userId` | long | 是 | 买家总账户用户 ID |
| `amount` | decimal | 是 | 调账金额：正数调增、负数扣减、0 无效 |
| `remark` | string | 否 | 备注 / 调整原因 |

**响应**

- 返回 `DrBalanceAdjustResult`：包含 `balance`（最新余额信息）、`billingRecord`（生成的调账账单）以及 `appliedAmount`（实际调账幅度，扣减时为负数）

**校验要点**

- 仅允许对买家总账户执行调账，必要时会自动创建余额账户。
- 扣减时若余额不足会直接清零并记录实际扣减金额；不影响预扣费余额、不触发代理佣金。

## 7. `POST /dr/bill/list`

- 对应 `DrBalanceController#billingList`
- 获取 DR 资产明细（账单记录），支持按用户/业务类型/时间等条件分页查询

**常用查询字段**

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| `userId` | long | 指定买家总账户 ID（只返回该用户的账单） |
| `billType` | int | 账单类型：`1` 充值、`2` 消费、`3` 退款 |
| `businessType` | string | 业务类型（如 `RECHARGE`、`INSTANCE_MARKETING` 等） |
| `params[beginTime]` | string | 创建时间起始（格式 `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss`） |
| `params[endTime]` | string | 创建时间结束（同上），与 `beginTime` 组合筛选账单时间段 |
| 其他 | — | 支持 `pageNum`、`pageSize` 分页参数 |

**JSON 示例**

```json
{
  "userId": 10001,
  "billType": 2,
  "params": {
    "beginTime": "2025-11-01 00:00:00",
    "endTime": "2025-11-30 23:59:59"
  }
}
```

**返回**

- 标准 `TableDataInfo<DrBillingRecord>`：包含总数、分页信息以及账单列表。

## 8. 开发QA建议

1. 使用管理员账号调用 `/system/dept`，验证在不同父部门下创建时负责人校验与层级计算是否生效。
2. 使用一级代理尝试为自身及下级部门创建代理、买家总部门，确认越级校验逻辑。
3. 通过 `/agent/commission/manual-adjust` 测试代理佣金的增减，确认佣金账户与流水记录同步更新。
4. 针对子账号部门，测试以下场景：
   - 不传 `parentUserId`，确认自动回填为负责人；
   - 传入非负责人或非买家总账号用户，接口应提示错误。
5. 继续保持原有字段兼容性，如有自定义字段（`roles`、`postIds` 等），按原协议随附即可。 

## 9. 与部门强关联的系统组件统计

> 以下统计基于 2025-11-04 当前代码库，通过 `rg` 搜索 `deptId`/`deptType` 并人工核验模块而得，实际引用可能更多。前端依赖未在本表内统计。

| 分类 | 数量 | 代表文件/说明 |
| ---- | ---- | ---- |
| **数据库与DDL脚本** | ≥7 | `sys_dept` 全量建表脚本、`sys_user.dept_id` 外键、`sys_role_dept` 授权表、`dr_agent_commission_record.agent_dept_id/buyer_dept_id`、若干初始化/迁移脚本（如 `sql/authority.sql`, `sql/role_rebuild.sql`, `sql/accountDesign.sql`）均直接依赖部门层级。 |
| **核心领域实体/DTO** | 12 | `SysDept`, `SysUser`, `SysRole`, `LoginUser`, `DeptUserGroupDTO`, `UserVO` 等均内置 `deptId/deptType/level` 字段，用于权限判定与展示。 |
| **控制器 / 接口层** | 6 | `SysDeptController`, `SysUserController`, `SysRoleController`, `AgentCommissionController`, `DrBalanceController`, `BuyerInstanceStatisticsController` 在接口参数、校验逻辑中直接读取部门信息。 |
| **服务与业务实现** | 11 | `SysDeptServiceImpl`, `SysUserServiceImpl`, `SysRoleServiceImpl`, `AgentCommissionServiceImpl`, `UserDrBalanceServiceImpl`, `BuyerInstanceStatisticsServiceImpl` 等在创建、权限校验、资金结算流程中均以部门层级作为决策依据。 |
| **Mapper/XML 层** | ≥9 | `SysDeptMapper.xml`, `SysUserMapper.xml`, `SysRoleMapper.xml`, `AgentCommissionAccountMapper.xml`, `AgentCommissionRecordMapper.xml` 等查询条件与字段映射都包含 `dept_id/level/dept_type`。 |
| **安全与工具类** | 3 | `DeptUtils`, `DataScopeCalculator`, `JwtTokenUtil`（缓存/TokenClaims 中包含 `deptId`）用于数据权限判定与登陆态缓存。 |
| **统计 / 报表模块** | 2 | `StatisticsController`, `BuyerInstanceStatisticsServiceImpl` 中的统计接口按部门聚合或过滤数据。 |

综合上述，Java 代码中至少有 22 个类文件直接引用 `deptId`，21 个文件引用 `deptType`。要移除“部门”概念，需同步改造上述所有模块以及依赖它们的脚本与测试用例。
