# DR价格配置接口文档

## 接口概述

本文档提供DR价格配置管理的相关接口，包括新增、查询、修改和删除价格配置功能。

**基础路径:** `/dr/price/config`

---

## 1. 新增DR价格配置

**接口地址:** `POST /dr/price/config`

**请求方式:** POST

**接口描述:** 新增DR价格配置项

**请求头:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**请求参数:**
```json
{
  "businessType": "string",      // 业务类型(必填)
  "businessName": "string",      // 业务名称(必填)
  "price": "number",            // 价格(必填)
  "unit": "string",             // 单位(必填)
  "description": "string"       // 描述信息(可选)
}
```

**请求示例:**
```json
{
  "businessType": "ai_instance",
  "businessName": "AI实例",
  "price": 0.5,
  "unit": "小时",
  "description": "AI实例使用费用"
}
```

**响应示例:**
```json
{
  "code": 200,
  "msg": "新增成功",
  "data": "新增成功"
}
```

**错误响应:**
```json
{
  "code": 500,
  "msg": "新增价格配置'AI实例'失败，业务类型已存在",
  "data": null
}
```

---

## 2. 查询DR价格配置列表

**接口地址:** `GET /dr/price/config/list`

**请求方式:** GET

**接口描述:** 分页查询DR价格配置列表

**请求头:**
```
Authorization: Bearer {token}
```

**请求参数:**
```
businessType=string    // 业务类型(可选)
businessName=string    // 业务名称(可选)
pageNum=1              // 页码(可选，默认1)
pageSize=10            // 每页数量(可选，默认10)
```

**请求示例:**
```
GET /dr/price/config/list?businessType=ai_instance&pageNum=1&pageSize=10
```

**响应示例:**
```json
{
  "code": 200,
  "msg": "查询成功",
  "rows": [
    {
      "priceId": 1,
      "businessType": "ai_instance",
      "businessName": "AI实例",
      "price": 0.5,
      "unit": "小时",
      "description": "AI实例使用费用",
      "createTime": "2024-01-01 10:00:00",
      "updateTime": "2024-01-01 10:00:00"
    },
    {
      "priceId": 2,
      "businessType": "ai_character",
      "businessName": "AI人设",
      "price": 100,
      "unit": "个",
      "description": "AI人设创建费用",
      "createTime": "2024-01-01 10:00:00",
      "updateTime": "2024-01-01 10:00:00"
    }
  ],
  "total": 2
}
```

---

## 3. 修改DR价格配置

**接口地址:** `PUT /dr/price/config`

**请求方式:** PUT

**接口描述:** 修改已存在的DR价格配置

**请求头:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**请求参数:**
```json
{
  "priceId": 1,                // 价格配置ID(必填)
  "businessType": "string",    // 业务类型(必填)
  "businessName": "string",    // 业务名称(必填)
  "price": "number",          // 价格(必填)
  "unit": "string",           // 单位(必填)
  "description": "string"     // 描述信息(可选)
}
```

**请求示例:**
```json
{
  "priceId": 1,
  "businessType": "ai_instance",
  "businessName": "AI实例",
  "price": 0.8,
  "unit": "小时",
  "description": "AI实例使用费用(已调整)"
}
```

**响应示例:**
```json
{
  "code": 200,
  "msg": "修改成功",
  "data": "修改成功"
}
```

**错误响应:**
```json
{
  "code": 500,
  "msg": "修改价格配置'AI实例'失败，业务类型已存在",
  "data": null
}
```

---

## 4. 删除DR价格配置

**接口地址:** `DELETE /dr/price/config/{priceIds}`

**请求方式:** DELETE

**接口描述:** 批量删除DR价格配置

**请求头:**
```
Authorization: Bearer {token}
```

**路径参数:**
```
priceIds: Long[]     // 价格配置ID数组，多个ID用逗号分隔
```

**请求示例:**
```
DELETE /dr/price/config/1,2,3
```

**响应示例:**
```json
{
  "code": 200,
  "msg": "删除成功",
  "data": "删除成功"
}
```

**错误响应:**
```json
{
  "code": 500,
  "msg": "删除失败",
  "data": null
}
```

---

## 通用响应格式

**成功响应:**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "具体返回数据"
}
```

**错误响应:**
```json
{
  "code": 500,
  "msg": "错误信息",
  "data": null
}
```

## 业务规则

1. **业务类型唯一性**: 系统中业务类型不能重复
2. **数据验证**: 所有必填字段都需要进行数据验证
3. **权限控制**: 接口需要相应的操作权限(当前已注释)
4. **操作日志**: 所有修改操作都会记录操作日志

## 注意事项

1. 删除操作为物理删除，请谨慎操作
2. 价格字段建议使用正数，支持小数点后多位
3. 业务类型建议使用英文标识符
4. 单位字段支持中文描述

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权访问 |
| 403 | 权限不足 |
| 500 | 服务器内部错误 |

## 数据字典

### businessType 可选值

| 值 | 说明 |
|----|------|
| ai_instance | AI实例 |
| ai_character | AI人设 |
| cloud_computer | 云电脑 |
| proxy_service | 代理服务 |

### unit 可选值

| 值 | 说明 |
|----|------|
| 小时 | 按小时计费 |
| 天 | 按天计费 |
| 月 | 按月计费 |
| 次 | 按次计费 |
| 个 | 按个计费 |