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
    "level1Agent": {
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

### 1.9 总代理伞下贡献统计
- **Method**: GET
- **Path**: `/statistics/general-agent/{agentId}/contribution`
- **Auth**: 管理员、总代本人或对该总代拥有层级数据权限的上级
- **Response**:
  ```json
  {
    "generalAgentId": 332,
    "generalAgentUsername": "fanyonceshi",
    "generalAgentNickname": "总代-测试",
    "level1AgentCount": 3,
    "level1AgentTotalCommission": 8421.50,
    "level2AgentCount": 5,
    "level2AgentTotalCommission": 5123.40,
    "merchantCount": 12,
    "merchantTotalRecharge": 67890.12
  }
  ```

### 1.10 总代伞下统计
- **Method**: GET
- **Path**: `/statistics/agent/general/subtree`
- **Auth**: 需总代（`agent_level_1`）登录，自动从 token 解析用户
- **Response**:
  ```json
  {
    "generalAgent": {
      "userId": 10001,
      "username": "general_agent_001",
      "nickname": "总代-华南"
    },
    "level2Agents": {
      "identity": "agent_level_2",
      "displayName": "一级代理",
      "agentCount": 12,
      "totalCommission": 23888.00
    },
    "level3Agents": {
      "identity": "agent_level_3",
      "displayName": "二级代理",
      "agentCount": 37,
      "totalCommission": 11888.00
    },
    "merchants": {
      "identity": "buyer_main",
      "displayName": "商家",
      "merchantCount": 260,
      "totalRecharge": 880000.00
    },
    "totals": {
      "totalCommission": 35776.00,
      "totalRecharge": 880000.00
    }
  }
  ```

### 1.11 一级代理伞下统计
- **Method**: GET
- **Path**: `/statistics/agent/level1/subtree`
- **Auth**: 需一级代理（`agent_level_2`）登录
- **Response**:
  ```json
  {
    "level2Agent": {
      "userId": 21001,
      "username": "agent_level1_a",
      "nickname": "一级代理-A"
    },
    "level3Agents": {
      "identity": "agent_level_3",
      "displayName": "二级代理",
      "agentCount": 18,
      "totalCommission": 12888.00
    },
    "merchants": {
      "identity": "buyer_main",
      "displayName": "商家",
      "merchantCount": 96,
      "totalRecharge": 320000.00
    },
    "totals": {
      "totalCommission": 12888.00,
      "totalRecharge": 320000.00
    }
  }
  ```

### 1.12 二级代理伞下统计
- **Method**: GET
- **Path**: `/statistics/agent/level2/subtree`
- **Auth**: 需二级代理（`agent_level_3`）登录
- **Response**:
  ```json
  {
    "level3Agent": {
      "userId": 22001,
      "username": "agent_level2_a",
      "nickname": "二级代理-A"
    },
    "merchants": {
      "identity": "buyer_main",
      "displayName": "商家",
      "merchantCount": 42,
      "totalRecharge": 98000.00
    },
    "totals": {
      "totalCommission": 0.00,
      "totalRecharge": 98000.00
    }
  }
  ```

### 1.11 代理直属用户统计
- **Method**: GET
- **Path**: `/statistics/agent/{userId}/children-statistics`
- **Auth**: 登录态
- **Response**（根据代理身份返回不同组合）：
  ```json
  {
    "agentId": 20001,
    "agentUsername": "agent_level_1_user",
    "identity": "agent_level_1",
    "agentLevel2Count": 5,
    "agentLevel3Count": 12,
    "merchantCount": 48
  }
  ```

- **Method**: GET
- **Path**: `/statistics/agent/general/children?userId={id}`
- **说明**: 校验用户身份必须是 `agent_level_1`（总代），返回与上面相同结构。

- **Method**: GET
- **Path**: `/statistics/agent/level1/children?userId={id}`
- **说明**: 校验用户身份是 `agent_level_2`（一级代理），字段只包含 `agentLevel3Count` 与 `merchantCount`。

- **Method**: GET
- **Path**: `/statistics/agent/level2/children?userId={id}`
- **说明**: 校验用户身份是 `agent_level_3`（二级代理），字段只包含 `merchantCount`。

### 1.12 代理佣金概览
- **Method**: GET
- **Path**: `/statistics/agent/commission-overview`
- **Auth**: 需代理（`agent_level_1/2/3`）登录，自动从 token 解析用户
- **Response**:
  ```json
  {
    "agentUserId": 20001,
    "username": "agent_user_a",
    "nickname": "一级代理-A",
    "totalCommission": 23888.00,
    "settledCommission": 12000.00,
    "availableCommission": 11888.00
  }
  ```

### 1.13 指定用户统计
- **Method**: GET
- **Path**: `/system/user/{userId}/statistics`
- **Response**: 预留，当前返回空结构 `{}`（TODO）。

### 1.14 角色统计
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

### 1.15 全角色汇总统计
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
