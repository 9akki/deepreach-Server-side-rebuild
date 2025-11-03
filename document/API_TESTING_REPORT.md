# DeepReach项目API接口测试报告

## 📋 测试信息

### 测试环境
- **JWT Token**: `eyJhbGciOiJIUzI1NiJ9.eyJkZXB0SWQiOjEsInR5cGUiOiJhY2Nlc3MiLCJ1c2VySWQiOjEsImVtYWlsIjoiYWRtaW5AZGVlcHJlYWNoLmNvbSIsInN1YiI6ImFkbWluIiwiaXNzIjoiZGVlcHJlYWNoIiwiYXVkIjpbImRlZXByZWFjaC11c2VycyJdLCJpYXQiOjE3NjE2Njk1NDgsImV4cCI6MTg0ODA3OTU0OH0.HnellEBv73Sb-bjwu77HzzZ0z0_ZIKA1xOjIqLOWdAI`

### Token解析信息
- **用户ID**: 1
- **用户名**: admin
- **邮箱**: admin@deepreach.com
- **权限**: 系统管理员
- **部门ID**: 1 (系统部门)
- **角色**: access_token
- **有效期**: 2024-10-29 (已过期)

### ⚠️ 重要提醒
**此Token已过期！** (exp: 1761679548, 当前: 1761679548)
需要先获取新的有效token才能继续测试。

## 🔍 接口测试结果

### 1. 🔐 认证授权模块

#### 1.1 用户登录
**请求**:
```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

**预期响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "string",
    "refreshToken": "string",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "userId": 1,
      "username": "admin",
      "nickname": "超级管理员",
      "email": "admin@deepreach.com",
      "phone": "13800138000",
      "status": "0",
      "deptId": 1,
      "roles": ["admin"],
      "permissions": ["system:user:list", "system:role:list"]
    }
  }
}
```

**实际响应**:
**状态**: ⏳️ 待测试
**结果**:

---

## 📊 测试统计

### 测试进度
- **总接口数**: 87个
- **已测试**: 0个
- **成功**: 0个
- **失败**: 0个
- **待测试**: 87个

### 测试状态
- 🟢 成功: 0个
- 🔴 失败: 0个
- 🟡 待测试: 87个

---

## 🔄 测试执行记录

### 执行命令
```bash
# 测试认证接口
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 使用有效token测试其他接口
curl -X GET http://localhost:8080/system/user/list \
  -H "Authorization: Bearer {new_token}"
```

### 注意事项
1. 需要先成功登录获取有效token
2. 替换测试中的占位符为实际服务器地址
3. 检查应用是否正在运行
4. 验证CORS配置是否正确

---

**📝 测试进行中，请稍等...**