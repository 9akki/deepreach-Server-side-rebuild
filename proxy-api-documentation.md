# 代理管理接口文档

## 接口概述

代理管理模块提供完整的HTTP和SOCKS5代理配置管理功能，包括代理的增删改查、连接测试、状态管理等。

**基础信息：**
- 基础路径：`http://localhost:8080/api/proxy`
- 认证方式：Bearer Token
- 数据格式：JSON
- 字符编码：UTF-8

## 认证说明

所有接口都需要在请求头中携带有效的JWT Token：

```
Authorization: Bearer <your-jwt-token>
```

获取Token：
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456"
}
```

## 接口详情

### 1. 查询接口

#### 1.1 获取代理配置列表

**接口路径：** `GET /proxy/list`

**功能描述：** 获取当前用户的代理配置列表，支持分页和多条件查询。

**请求参数：**
- Query参数（可选）：
  - `proxyType`：代理类型（0=HTTP, 1=SOCKS5）
  - `proxyHost`：代理主机地址（模糊查询）
  - `proxyPort`：代理端口
  - `proxyUsername`：代理用户名
  - `status`：状态（0=正常, 1=弃用）
  - `pageNum`：页码（默认1）
  - `pageSize`：每页大小（默认10）

**请求示例：**
```bash
GET http://localhost:8080/api/proxy/list?pageNum=1&pageSize=10&proxyType=0
Authorization: Bearer <token>
```

**响应示例：**
```json
{
    "total": 2,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1,
    "rows": [
        {
            "proxyId": 3,
            "userId": 1,
            "proxyType": 0,
            "proxyHost": "192.168.1.100",
            "proxyPort": "3128",
            "proxyUsername": null,
            "proxyPassword": null,
            "status": "0",
            "createTime": "2025-10-29 15:38:13",
            "updateTime": null,
            "remark": "公司HTTP代理",
            "proxyTypeDisplay": "HTTP",
            "statusDisplay": "正常",
            "proxyAddress": "192.168.1.100:3128",
            "proxyUrl": "http://192.168.1.100:3128"
        }
    ],
    "code": 200,
    "msg": "查询成功"
}
```

#### 1.2 获取代理详情

**接口路径：** `GET /proxy/{proxyId}`

**功能描述：** 根据代理ID获取详细信息。

**路径参数：**
- `proxyId`：代理ID（必填）

**请求示例：**
```bash
GET http://localhost:8080/api/proxy/3
Authorization: Bearer <token>
```

**响应示例：**
```json
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "proxyId": 3,
        "userId": 1,
        "proxyType": 0,
        "proxyHost": "192.168.1.100",
        "proxyPort": "3128",
        "proxyUsername": null,
        "proxyPassword": null,
        "status": "0",
        "createTime": "2025-10-29 15:38:13",
        "remark": "公司HTTP代理",
        "proxyTypeDisplay": "HTTP",
        "statusDisplay": "正常"
    }
}
```

#### 1.3 获取当前用户的代理列表

**接口路径：** `GET /proxy/user`

**功能描述：** 获取当前用户的所有代理配置。

**请求示例：**
```bash
GET http://localhost:8080/api/proxy/user
Authorization: Bearer <token>
```

#### 1.4 获取可用代理列表

**接口路径：** `GET /proxy/available`

**功能描述：** 获取状态为正常的代理配置列表。

**请求参数：**
- `proxyType`：代理类型（可选，0=HTTP, 1=SOCKS5）

**请求示例：**
```bash
GET http://localhost:8080/api/proxy/available?proxyType=0
Authorization: Bearer <token>
```

#### 1.5 随机获取可用代理

**接口路径：** `GET /proxy/random`

**功能描述：** 随机获取一个状态正常的代理配置。

**请求参数：**
- `proxyType`：代理类型（可选，0=HTTP, 1=SOCKS5）

**请求示例：**
```bash
GET http://localhost:8080/api/proxy/random
Authorization: Bearer <token>
```

### 2. 新增接口

#### 2.1 新增代理配置

**接口路径：** `POST /proxy`

**功能描述：** 创建新的代理配置。

**请求参数：**
```json
{
    "proxyType": 0,              // 代理类型（0=HTTP, 1=SOCKS5）
    "proxyHost": "proxy.test.com", // 代理主机地址
    "proxyPort": "8080",         // 代理端口
    "proxyUsername": "user123",  // 代理用户名（可选）
    "proxyPassword": "pass123",  // 代理密码（可选）
    "status": "0",               // 状态（可选，默认0=正常）
    "remark": "测试代理"         // 备注（可选）
}
```

**请求示例：**
```bash
POST http://localhost:8080/api/proxy
Content-Type: application/json
Authorization: Bearer <token>

{
    "proxyType": 0,
    "proxyHost": "proxy.test.com",
    "proxyPort": "8080",
    "proxyUsername": "user123",
    "proxyPassword": "pass123",
    "remark": "测试HTTP代理"
}
```

**响应示例：**
```json
{
    "code": 200,
    "msg": "新增代理配置成功",
    "data": {
        "proxyId": 5,
        "userId": 1,
        "proxyType": 0,
        "proxyHost": "proxy.test.com",
        "proxyPort": "8080",
        "status": "0",
        "createTime": "2025-10-31 11:57:30",
        "remark": "测试HTTP代理"
    }
}
```

### 3. 修改接口

#### 3.1 更新代理配置

**接口路径：** `PUT /proxy`

**功能描述：** 更新代理配置信息。

**请求参数：**
```json
{
    "proxyId": 3,                // 代理ID（必填）
    "proxyType": 0,              // 代理类型（可选）
    "proxyHost": "192.168.1.101", // 代理主机地址（可选）
    "proxyPort": "3129",         // 代理端口（可选）
    "proxyUsername": "newuser",  // 代理用户名（可选）
    "proxyPassword": "newpass",  // 代理密码（可选）
    "remark": "更新后的备注"     // 备注（可选）
}
```

**请求示例：**
```bash
PUT http://localhost:8080/api/proxy
Content-Type: application/json
Authorization: Bearer <token>

{
    "proxyId": 3,
    "proxyHost": "192.168.1.101",
    "proxyPort": "3129",
    "remark": "更新后的公司HTTP代理"
}
```

**响应示例：**
```json
{
    "code": 200,
    "msg": "修改代理配置成功"
}
```

#### 3.2 更新代理状态

**接口路径：** `PUT /proxy/{proxyId}/status/{status}`

**功能描述：** 更新代理的启用状态。

**路径参数：**
- `proxyId`：代理ID
- `status`：状态（0=启用, 1=弃用）

**请求示例：**
```bash
PUT http://localhost:8080/api/proxy/3/status/1
Authorization: Bearer <token>
```

**响应示例：**
```json
{
    "code": 200,
    "msg": "弃用代理配置成功"
}
```

### 4. 删除接口

#### 4.1 删除单个代理

**接口路径：** `DELETE /proxy/{proxyId}`

**功能描述：** 删除指定的代理配置。

**路径参数：**
- `proxyId`：代理ID

**请求示例：**
```bash
DELETE http://localhost:8080/api/proxy/3
Authorization: Bearer <token>
```

**响应示例：**
```json
{
    "code": 200,
    "msg": "删除代理配置成功"
}
```

#### 4.2 批量删除代理

**接口路径：** `DELETE /proxy`

**功能描述：** 批量删除多个代理配置。

**请求参数：**
```json
[3, 4, 5]  // 代理ID数组
```

**请求示例：**
```bash
DELETE http://localhost:8080/api/proxy
Content-Type: application/json
Authorization: Bearer <token>

[3, 4, 5]
```

**响应示例：**
```json
{
    "code": 200,
    "msg": "批量删除代理配置成功",
    "data": {
        "totalCount": 3,
        "successCount": 3,
        "failureCount": 0,
        "successIds": [3, 4, 5],
        "failureIds": [],
        "allSuccess": true,
        "partialSuccess": false,
        "allFailure": false
    }
}
```

### 5. 测试接口

#### 5.1 测试代理连接

**接口路径：** `POST /proxy/{proxyId}/test`

**功能描述：** 测试指定代理的连接可用性。

**路径参数：**
- `proxyId`：代理ID

**请求示例：**
```bash
POST http://localhost:8080/api/proxy/3/test
Authorization: Bearer <token>
```

**响应示例：**
```json
{
    "code": 200,
    "msg": "测试完成",
    "data": {
        "success": false,
        "message": "代理连接失败，请检查代理配置",
        "responseTime": 5000,
        "timestamp": "2025-10-31 11:58:00"
    }
}
```

#### 5.2 测试临时代理连接

**接口路径：** `POST /proxy/test`

**功能描述：** 测试临时代理配置的连接可用性（不保存到数据库）。

**请求参数：**
```json
{
    "proxyType": 0,
    "proxyHost": "proxy.test.com",
    "proxyPort": "8080",
    "proxyUsername": "user123",
    "proxyPassword": "pass123"
}
```

**请求示例：**
```bash
POST http://localhost:8080/api/proxy/test
Content-Type: application/json
Authorization: Bearer <token>

{
    "proxyType": 0,
    "proxyHost": "proxy.test.com",
    "proxyPort": "8080"
}
```

#### 5.3 批量测试代理连接

**接口路径：** `POST /proxy/batch-test`

**功能描述：** 批量测试多个代理的连接可用性。

**请求参数：**
```json
[3, 4, 5]  // 代理ID数组
```

### 6. 统计接口

#### 6.1 获取代理统计信息

**接口路径：** `GET /proxy/statistics`

**功能描述：** 获取当前用户的代理统计信息。

**请求示例：**
```bash
GET http://localhost:8080/api/proxy/statistics
Authorization: Bearer <token>
```

**响应示例：**
```json
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "totalProxies": 5,
        "normalProxies": 3,
        "disabledProxies": 2,
        "httpProxies": 3,
        "socks5Proxies": 2,
        "authenticatedProxies": 2,
        "publicProxies": 3
    }
}
```

#### 6.2 获取指定用户的代理统计

**接口路径：** `GET /proxy/statistics/user/{userId}`

**功能描述：** 获取指定用户的代理统计信息（管理员权限）。

### 7. 验证接口

#### 7.1 检查代理地址唯一性

**接口路径：** `GET /proxy/check-unique`

**功能描述：** 检查代理地址和端口是否已存在。

**请求参数：**
- `host`：代理主机地址（必填）
- `port`：代理端口（必填）
- `proxyId`：排除的代理ID（可选，用于更新时验证）

**请求示例：**
```bash
GET http://localhost:8080/api/proxy/check-unique?host=proxy.test.com&port=8080&proxyId=3
Authorization: Bearer <token>
```

**响应示例：**
```json
{
    "code": 200,
    "msg": "操作成功",
    "data": true  // true表示唯一，false表示已存在
}
```

#### 7.2 验证代理配置

**接口路径：** `POST /proxy/validate`

**功能描述：** 验证代理配置的完整性和有效性。

**请求参数：**
```json
{
    "proxyType": 0,
    "proxyHost": "proxy.test.com",
    "proxyPort": "8080",
    "proxyUsername": "user123",
    "proxyPassword": "pass123"
}
```

### 8. 高级功能接口

#### 8.1 检查代理池健康状态

**接口路径：** `GET /proxy/health`

**功能描述：** 检查代理池的整体健康状态。

**请求示例：**
```bash
GET http://localhost:8080/api/proxy/health
Authorization: Bearer <token>
```

#### 8.2 获取代理使用建议

**接口路径：** `GET /proxy/recommendations`

**功能描述：** 获取代理使用的优化建议。

**请求示例：**
```bash
GET http://localhost:8080/api/proxy/recommendations
Authorization: Bearer <token>
```

## 数据模型

### Proxy实体模型

| 字段名 | 类型 | 描述 | 必填 | 备注 |
|--------|------|------|------|------|
| proxyId | Long | 代理ID | 否 | 主键，自增长 |
| userId | Long | 用户ID | 是 | 关联用户 |
| proxyType | Integer | 代理类型 | 是 | 0=HTTP, 1=SOCKS5 |
| proxyHost | String | 代理主机地址 | 是 | IP或域名 |
| proxyPort | String | 代理端口 | 是 | 1-65535 |
| proxyUsername | String | 代理用户名 | 否 | 可选 |
| proxyPassword | String | 代理密码 | 否 | 可选 |
| status | String | 状态 | 否 | 0=正常, 1=弃用，默认0 |
| remark | String | 备注 | 否 | 可选 |

## 错误码说明

| 错误码 | 描述 | 解决方案 |
|--------|------|----------|
| 200 | 操作成功 | - |
| 401 | 未授权访问 | 检查Token是否有效 |
| 403 | 权限不足 | 检查用户权限 |
| 404 | 资源不存在 | 检查代理ID是否正确 |
| 500 | 服务器内部错误 | 检查请求参数和服务器状态 |

## 测试用例

### 基础CRUD测试

```bash
# 1. 获取代理列表
curl -X GET "http://localhost:8080/api/proxy/list" \
  -H "Authorization: Bearer <token>"

# 2. 新增HTTP代理
curl -X POST "http://localhost:8080/api/proxy" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"proxyType": 0, "proxyHost": "proxy.test.com", "proxyPort": "8080", "remark": "测试HTTP代理"}'

# 3. 更新代理配置
curl -X PUT "http://localhost:8080/api/proxy" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"proxyId": 3, "proxyHost": "192.168.1.101", "proxyPort": "3129", "remark": "更新后的代理"}'

# 4. 删除代理
curl -X DELETE "http://localhost:8080/api/proxy/3" \
  -H "Authorization: Bearer <token>"
```

### 高级功能测试

```bash
# 测试代理连接
curl -X POST "http://localhost:8080/api/proxy/3/test" \
  -H "Authorization: Bearer <token>"

# 获取可用代理
curl -X GET "http://localhost:8080/api/proxy/available" \
  -H "Authorization: Bearer <token>"

# 获取统计信息
curl -X GET "http://localhost:8080/api/proxy/statistics" \
  -H "Authorization: Bearer <token>"
```

## 注意事项

1. **权限控制**：用户只能管理自己创建的代理配置
2. **密码安全**：代理密码建议加密存储
3. **连接测试**：连接测试超时时间为5秒
4. **状态管理**：弃用状态的代理不会被系统使用
5. **唯一性约束**：同一用户不能重复添加相同地址和端口的代理
6. **数据验证**：所有输入参数都会进行格式和有效性验证

## 更新日志

- **v1.0** (2025-10-31): 初始版本，支持基础CRUD功能
- 支持HTTP和SOCKS5两种代理类型
- 提供完整的代理生命周期管理
- 包含连接测试和统计分析功能