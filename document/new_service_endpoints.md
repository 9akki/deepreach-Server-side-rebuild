# 新增接口说明

本文档汇总近期新增的 4 个后台接口。所有返回示例均以简化形式列出核心字段，实际响应继承系统统一的 `Result` 包装格式。

## 1. 买家层级综合统计

- **方法**：`GET`
- **路径**：`/buyer/instances/overview/{buyerMainUserId}`
- **说明**：面向管理员或运营，按买家总账户用户 ID 汇总伞下子部门、子用户、实例、平台、人设与 DR 账户信息。

### 请求参数

| 参数名 | 位置 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- | --- |
| `buyerMainUserId` | Path | Long | 是 | 买家总账户用户 ID |

### 响应字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `buyerMainUserId` | Long | 买家总账户用户 ID |
| `buyerMainUsername` | String | 买家总账户用户名 |
| `buyerMainNickname` | String | 买家总账户昵称 |
| `buyerMainDeptId` | Long | 买家总部门 ID |
| `buyerMainDeptName` | String | 买家总部门名称 |
| `subDeptCount` | Integer | 买家子部门数量 |
| `subUserCount` | Integer | 买家子用户数量 |
| `instanceCount` | Integer | 子用户实例总数 |
| `instanceTypeStatistics` | Object | 实例类型统计（`marketing`、`prospecting`、`other`） |
| `platformStatistics` | Array\<Object\> | 按平台统计：`platformId`、`instanceCount`、`platformLabel` |
| `subUsers` | Array\<Object\> | 子用户明细：`userId`、`username`、`nickname`、`realName`、`deptId`、`deptName`、`deptStatus`、`instanceCount`、`instances`（原始实例对象列表） |
| `aiCharacterStatistics` | Object | 人设统计：`totalCount`、`systemCount`、`userCreatedCount`、`emotionCount`、`businessCount` |
| `drAccount` | Object | DR 账户快照：`userId`、`drBalance`、`preDeductedBalance`、`totalRecharge`、`totalConsume`、`totalRefund`、`frozenAmount`、`status` |

---

## 2. DR 积分收支明细查询

- **方法**：`GET`
- **路径**：`/dr/billing/user/{userId}/transactions`
- **说明**：按商户用户 ID 拉取 DR 积分账单明细，可选多条件筛选。返回记录包含 `consumer` 字段标识发起人。

### 请求参数

| 参数名 | 位置 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- | --- |
| `userId` | Path | Long | 是 | 商户用户 ID |
| `startTime` | Query | String | 否 | 起始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `endTime` | Query | String | 否 | 截止时间，格式同上 |
| `minAmount` | Query | Decimal | 否 | 最小 DR 金额 |
| `maxAmount` | Query | Decimal | 否 | 最大 DR 金额 |
| `billType` | Query | Integer | 否 | 账单类型（1 充值 / 2 消费 / 3 退款等） |
| `businessType` | Query | String | 否 | 业务类型标识 |

### 响应字段

返回 `DrBillingRecord` 列表，核心字段包括：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `billId` | Long | 账单 ID |
| `billNo` | String | 账单编号 |
| `userId` | Long | 对应商户用户 ID |
| `operatorId` | Long | 操作者 ID |
| `billType` | Integer | 账单类型 |
| `billingType` | Integer | 结算类型（1 秒结秒扣 / 2 日结日扣） |
| `businessType` | String | 业务类型常量 |
| `businessId` | Long | 业务关联 ID |
| `drAmount` | BigDecimal | DR 金额（正负区分收支） |
| `balanceBefore` / `balanceAfter` | BigDecimal | 操作前/后的余额 |
| `description` | String | 描述 |
| `consumer` | String | 发起人用户名 |
| `status` | Integer | 状态（1 成功 / 0 失败） |
| `createTime` | String | 创建时间 |

---

## 3. 管理员获取进行中的结算请求

- **方法**：`GET`
- **路径**：`/agent/commission/settlement/admin/in-progress`
- **说明**：列出所有处于进行中（待审批）状态的代理佣金结算申请。

### 响应字段

返回 `AgentCommissionSettlement` 列表，核心字段包括：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `settlementId` | Long | 结算申请 ID |
| `agentUserId` | Long | 代理用户 ID |
| `requestAmount` | BigDecimal | 申请金额 |
| `approvedAmount` | BigDecimal | 审批金额（进行中默认 0） |
| `status` | String | 状态（`0` = 待审批） |
| `remark` | String | 备注 |
| `createTime` / `updateTime` | String | 创建 / 更新时间 |

---

## 4. 管理员获取已处理结算记录

- **方法**：`GET`
- **路径**：`/agent/commission/settlement/admin/completed`
- **说明**：查询所有状态为已完成（通过 / 拒绝 / 取消）的佣金结算记录，便于管理员对账或复核。

### 响应字段

同上，`status` 取值为：

| 状态码 | 说明 |
| --- | --- |
| `1` | 审批通过 |
| `2` | 审批拒绝 |
| `3` | 申请取消 |

---

## 5. 管理员获取总结算佣金概览

- **方法**：`POST`
- **路径**：`/agent/commission/overview`
- **说明**：获取代理体系内的佣金汇总，返回结算完成金额、筛选区间内金额、代理数量及代理佣金账户列表。

### 请求参数

Body 使用 `application/json`，字段均为可选：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `username` | String | 过滤伞下代理用户名（模糊匹配） |
| `startTime` | String | 统计开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `endTime` | String | 统计结束时间，格式同上 |
| `minAmount` | Decimal | 佣金下限 |
| `maxAmount` | Decimal | 佣金上限 |

### 响应字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `totalSettlementCommission` | BigDecimal | 历史累计已结算佣金总额 |
| `earnedCommissionInRange` | BigDecimal | 按筛选条件统计的佣金收益总额（仅基于收益流水，非已结算金额） |
| `agentCount` | Integer | 伞下代理数量 |
| `agents` | Array\<AgentCommissionAccountDTO\> | 代理佣金账户列表，包含 `agentUserId`、`username`、`nickname`、`availableCommission`、`frozenCommission`、`pendingSettlementCommission`、`settlementCommission`、`earnedCommissionInRange` 等字段 |

---

## 6. 获取代理已结算佣金总额

- **方法**：`GET`
- **路径**：`/agent/commission/settlement/total`
- **说明**：聚合所有代理账户的已结算佣金，返回总金额（单位：DR）。

### 响应字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `data` | BigDecimal | 已结算佣金总额，保留四位小数 |

---

## 7. 查询部门用户列表

- **方法**：`POST`
- **路径**：`/system/user/dept`
- **说明**：分页查询指定部门及其子部门用户，支持多条件筛选。

### 查询参数（可选）

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| `pageNum` | Integer | 页码，默认 `1` |
| `pageSize` | Integer | 每页数量，默认 `10` |
| `username` | String | 用户名模糊匹配 |

### 请求体（`application/json`）

使用 `SysUser` 字段作为筛选条件，`deptId` 必填，其他字段可选：

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `deptId` | Long | 是 | 目标部门 ID |
| `username` | String | 启动用户名模糊匹配 |
| `nickname` | String | 昵称模糊匹配 |
| `realName` | String | 真实姓名模糊匹配 |
| `phone` | String | 手机号模糊匹配 |
| `email` | String | 邮箱模糊匹配 |
| `status` | String | 账号状态（`0` 正常 / `1` 停用） |
| `params.beginTime` | String | 注册起始时间，`yyyy-MM-dd` |
| `params.endTime` | String | 注册结束时间，`yyyy-MM-dd` |

### 响应字段

返回统一分页结构 `TableDataInfo<UserVO>`，核心字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `total` | Long | 总记录数 |
| `pageNum` | Integer | 当前页码 |
| `pageSize` | Integer | 每页数量 |
| `rows` | Array\<UserVO\> | 用户列表，包含 `userId`、`username`、`nickname`、`deptId`、`deptName`、`roles`、`agentLevel`、`status`、`createTime` 等字段 |
