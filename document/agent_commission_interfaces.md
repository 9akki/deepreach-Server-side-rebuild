# Agent Commission Interfaces

本文档整理近期新增的代理佣金相关接口，包含接口路径、请求参数、响应示例及说明。

## 1. 获取代理佣金账户

- **Method / Path**: `GET /agent/commission/account/{agentUserId}`
- **描述**: 返回指定代理用户的佣金账户信息。如该代理从未产生佣金，金额类字段默认返回 0。
- **路径参数**:
  - `agentUserId` (Long): 代理用户 ID。
- **请求示例**:
  ```http
  GET /api/agent/commission/account/286
  ```
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "timestamp": 1762094000000,
    "data": {
      "agentUserId": 286,
      "username": "agent_level1",
      "nickname": "一级代理",
      "deptId": 643,
      "deptName": "一级代理部门",
      "deptLevel": 1,
      "totalCommission": 1200.0000,
      "availableCommission": 500.0000,
      "frozenCommission": 300.0000,
      "pendingSettlementCommission": 200.0000,
      "settlementCommission": 200.0000,
      "earnedCommissionInRange": 0
    }
  }
  ```

## 2. 获取代理佣金明细

- **Method / Path**: `POST /agent/commission/{agentUserId}/records`
- **描述**: 查询指定代理的佣金明细，支持按时间范围和佣金金额过滤。未传入过滤条件时返回该代理全部佣金记录。
- **路径参数**:
  - `agentUserId` (Long): 代理用户 ID。
- **请求体参数（可选）**:
  ```json
  {
    "startTime": "2025-11-01 00:00:00",
    "endTime": "2025-11-02 23:59:59",
    "minAmount": 5,
    "maxAmount": 500
  }
  ```
  - `startTime` / `endTime`: 时间范围，格式 `yyyy-MM-dd HH:mm:ss`。
  - `minAmount` / `maxAmount`: 佣金金额筛选范围。
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "recordId": 16,
        "agentUserId": 288,
        "agentDeptId": 645,
        "buyerUserId": 289,
        "buyerUsername": "merchant_a",
        "triggerBillingId": 158,
        "billingNo": "DR20251102215447000123",
        "triggerAmount": 100.0000,
        "commissionAmount": 10.0000,
        "commissionRate": 0.1000,
        "hierarchyLevel": 3,
        "businessType": "RECHARGE_COMMISSION",
        "status": "0",
        "description": "第3级代理获得充值佣金，充值金额:100, 佣金:10",
        "createTime": "2025-11-02 21:54:47"
      }
    ],
    "timestamp": 1762094000000
  }
  ```

## 3. 获取伞下代理佣金概览

- **Method / Path**: `POST /agent/commission/overview`
- **描述**: 返回当前登录用户可访问范围内所有代理的佣金账户信息以及指定条件下的已结算/统计数据。管理员可查看所有层级代理；一级代理仅能查看自身及下级代理。
- **请求体参数（可选）**:
  ```json
  {
    "username": "agent",
    "startTime": "2025-11-01 00:00:00",
    "endTime": "2025-11-02 23:59:59",
    "minAmount": 5,
    "maxAmount": 500
  }
  ```
  - `username`: 代理用户名或昵称关键字（模糊匹配）。
  - `startTime` / `endTime`: 佣金统计时间范围。
  - `minAmount` / `maxAmount`: 统计区间内佣金金额过滤范围。
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "timestamp": 1762094000000,
    "data": {
      "totalSettlementCommission": 1500.0000,
      "earnedCommissionInRange": 300.0000,
      "agentCount": 3,
      "agents": [
        {
          "agentUserId": 286,
          "username": "agent_level1",
          "nickname": "一级代理",
          "deptId": 643,
          "deptName": "一级代理部门",
          "deptLevel": 1,
          "totalCommission": 1200.0000,
          "availableCommission": 500.0000,
          "frozenCommission": 300.0000,
          "pendingSettlementCommission": 200.0000,
          "settlementCommission": 200.0000,
          "earnedCommissionInRange": 150.0000
        },
        {
          "agentUserId": 288,
          "username": "agent_level3",
          "nickname": "三级代理",
          "deptId": 645,
          "deptName": "三级代理部门",
          "deptLevel": 3,
          "totalCommission": 80.0000,
          "availableCommission": 30.0000,
          "frozenCommission": 20.0000,
          "pendingSettlementCommission": 10.0000,
          "settlementCommission": 20.0000,
          "earnedCommissionInRange": 30.0000
        }
      ]
    }
  }
  ```

## 说明

- 所有接口公共返回结构遵循 `Result<T>` 格式，`code=200` 表示成功，`code=400` 用于业务校验失败（如佣金不足）。
- 时间字段统一使用 `yyyy-MM-dd HH:mm:ss`。
- 金额字段均保留四位小数。
