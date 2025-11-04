# 统计相关接口一览

本文整理后端当前提供的所有“统计”类接口，包含请求方式、路径、主要请求参数及返回结构。默认前缀为 `/api`。

---

## 1. 核心层级统计（`deepreach-common`）

### 1.1 管理范围部门统计
- **Method**: GET
- **Path**: `/statistics/managed-depts`
- **Auth**: 需要登录（基于用户树判定）
- **Params**: 无（从登录用户获取）
- **Response** (`Result<Map<String,Object>>`):
  ```json
  {
    "managedDeptCount": 12,
    "deptTypeBreakdown": {
      "agent": 8,
      "buyerMain": 3,
      "buyerSub": 1
    },
    "rootUserId": 10001
  }
  ```

### 1.2 管理范围用户统计
- **Method**: GET
- **Path**: `/statistics/managed-users`
- **Params**: 无
- **Response**:
  ```json
  {
    "totalUsers": 45,
    "managedUserIds": [ 10001, 10002 ],
    "identityBreakdown": {
      "agent_level_1": 3,
      "agent_level_2": 7,
      "buyer_main": 20,
      "buyer_sub": 15
    },
    "agentLevelBreakdown": {
      "level1": 3,
      "level2": 7,
      "level3": 0
    },
    "unknownUserCount": 0
  }
  ```

### 1.3 管理范围代理层级统计
- **Method**: GET
- **Path**: `/statistics/agent-levels`
- **Response**:
  ```json
  {
    "agentLevelCount": {
      "level1": 3,
      "level2": 7,
      "level3": 2
    }
  }
  ```

### 1.4 管理范围买家账户统计
- **Method**: GET
- **Path**: `/statistics/buyer-accounts`
- **Response**:
  ```json
  {
    "buyerMainCount": 20,
    "buyerSubCount": 60,
    "buyerMainDetails": [
      {
        "userId": 20001,
        "childCount": 5
      }
    ]
  }
  ```

### 1.5 综合仪表板统计
- **Method**: GET
- **Path**: `/statistics/dashboard`
- **Response**: 多项指标组合（用户活跃度、充值、佣金、实例等）。示例：
  ```json
  {
    "userSummary": {"total": 45, "weekIncrease": 3},
    "rechargeSummary": {"totalAmount": 12345.67, "orderCount": 28},
    "commissionSummary": {"pending": 5, "settled": 12},
    "instanceSummary": {"marketing": 80, "customerAcquisition": 35}
  }
  ```

### 1.6 管理员代理业绩统计
- **Method**: GET
- **Path**: `/statistics/adminAgentPerformanceStatistics`
- **Auth**: 仅管理员账号可访问
- **Params**: 无
- **Response**:
  ```json
  {
    "generalAgent": {
      "identity": "agent_level_1",
      "identityDisplay": "总代",
      "agentCount": 3,
      "totalRecharge": 45678.90,
      "totalCommission": 8123.45
    },
    "level1Agent": {
      "identity": "agent_level_2",
      "identityDisplay": "一级代理",
      "agentCount": 12,
      "totalRecharge": 22345.67,
      "totalCommission": 3123.45
    },
    "level2Agent": {
      "identity": "agent_level_3",
      "identityDisplay": "二级代理",
      "agentCount": 28,
      "totalRecharge": 9988.00,
      "totalCommission": 1456.78
    },
    "total": {
      "identity": "total",
      "identityDisplay": "合计",
      "agentCount": 43,
      "totalRecharge": 78012.57,
      "totalCommission": 12703.68
    }
  }
  ```

### 1.7 管理员商家业绩统计
- **Method**: GET
- **Path**: `/statistics/adminMerchantsPerformanceStatistics`
- **Auth**: 仅管理员账号可访问
- **Response**:
  ```json
  {
    "merchantOverview": {
      "merchantCount": 120,
      "employeeCount": 360,
      "totalPerformance": 812345.67
    },
    "aiCharacterOverview": {
      "totalCharacters": 58,
      "socialAiCount": 35,
      "customerServiceAiCount": 23
    },
    "marketingInstanceOverview": {
      "instanceCount": 420,
      "platformBreakdown": {
        "wechat": 180,
        "qq": 60,
        "facebook": 80,
        "unknown": 100
      }
    },
    "prospectingInstanceOverview": {
      "instanceCount": 210,
      "platformBreakdown": {
        "whatsapp": 100,
        "telegram": 40,
        "weixin": 30,
        "unknown": 40
      }
    }
  }
  ```

### 1.8 管理员商家资产统计
- **Method**: GET
- **Path**: `/statistics/adminMerchantsAssetStatistics`
- **Auth**: 仅管理员账号可访问
- **Response**:
  ```json
  {
    "totalRecharge": 812345.67,
    "settledCommission": 12345.67,
    "netValue": 799999.99
  }
  ```

### 1.9 指定用户统计
- **Method**: GET
- **Path**: `/system/user/{userId}/statistics`
- **Response**: 预留，当前返回空结构 `{}`（TODO）。

### 1.10 角色统计
- **Method**: GET
- **Path**: `/system/role/{roleId}/statistics`
- **Params**: Path `roleId`
- **Response**:
  ```json
  {
    "userCount": 12,
    "menuCount": 45,
    "lastAssignTime": "2025-11-04T10:00:00"
  }
  ```

### 1.11 全角色汇总统计
- **Method**: GET
- **Path**: `/system/role/statistics/all`
- **Response**:
  ```json
  {
    "totalRoles": 6,
    "assignableRoles": 5,
    "roleUserCounts": {
      "admin": 1,
      "agent_level_1": 3
    }
  }
  ```

---

## 2. Web 层扩展统计（`deepreach-web`）

### 2.1 买家实例统计
- **Method**: GET
- **Path**: `/buyer/instances/{buyerMainUserId}`
- **Response**:
  ```json
  {
    "totalSubAccounts": 5,
    "totalInstances": 120,
    "marketingInstances": 80,
    "customerAcquisitionInstances": 40
  }
  ```

- **Method**: GET
- **Path**: `/buyer/instances/by-username/{username}`
- **Response**: 同上，按用户名查询。

### 2.2 代理佣金统计
- **Method**: GET
- **Path**: `/agent/commission/settlement/total`
- **Response**:
  ```json
  {
    "code": 200,
    "data": 12345.67,
    "msg": "操作成功"
  }
  ```

### 2.3 代理概况统计
- **Method**: GET
- **Path**: `/proxy/statistics`
- **Response**: 代理全局概览（用户数、佣金、活跃度等）。

- **Method**: GET
- **Path**: `/proxy/statistics/user/{userId}`
- **Response**: 指定代理的统计。

### 2.4 账单统计（预留）
- **Method**: GET
- **Path**: `/dr/billing/statistics/{userId}`
- **Response**: 当前实现返回空结构，待补。

### 2.5 AI 实例使用统计
- **Method**: GET
- **Path**: `/instance/statistics/type`
- **Response**: 按实例类型统计（营销 / 拓客）。

- **Method**: GET
- **Path**: `/instance/statistics/platform`
- **Response**: 按接入平台（飞书、企微等）统计。

- **Method**: GET
- **Path**: `/instance/statistics/character`
- **Response**: 按 AI 人物形象统计使用量。

（`/instance/statistics`、`/instance/my/statistics` 等注释掉的接口暂不对外）

---

## 3. 注意事项
- 所有统计接口均需要携带有效 JWT，后台会根据用户身份和用户树判断可访问范围。
- 返回结构多数为 `Result` 包裹的 JSON，字段键值可能随业务扩展调整，前端使用时请按照响应结构做兼容处理。
- 若统计项为空或尚未实现，会返回空 Map 或空数组。
