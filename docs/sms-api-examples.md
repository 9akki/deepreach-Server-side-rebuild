# SMS 接口请求 / 响应示例

所有接口的顶层结构均与 Python 服务保持一致：

```json
{
  "success": true,
  "data": { ... },
  "message": ""
}
```

失败时 `success=false`，并在 `message`（可选 `code`）中写明原因。

---

## 1. 创建任务 `POST /api/sms/tasks`
**请求**
```json
{
  "userId": 400,
  "instanceId": 10001,
  "receiverNumbers": ["+12135550123", "+8613800138000"],
  "messageContents": ["Hi A", "Hi B"],
  "totalCount": 2
}
```
**响应**
```json
{ "success": true, "data": { "taskId": 9123 }, "message": "" }
```

## 2. 列出任务
### 2.1 概要 `GET /api/sms/tasks?userId=400&summary=true`
```json
{
  "success": true,
  "data": [
    { "taskId": 9123, "createdAt": "2025-11-07", "unreadCount": 1 },
    { "taskId": 9055, "createdAt": "2025-11-01", "unreadCount": 0 }
  ],
  "message": ""
}
```

### 2.2 详情 `GET /api/sms/tasks?userId=400&details=true`
```json
{
  "success": true,
  "data": [
    {
      "taskId": 9123,
      "totalCount": 200,
      "sentCount": 180,
      "replyCount": 7,
      "deliveryRate": 0.9,
      "status": 1,
      "createdAt": "2025-11-07"
    }
  ],
  "message": ""
}
```

### 2.3 单个任务 `GET /api/sms/tasks/9123`
```json
{
  "success": true,
  "data": {
    "taskId": 9123,
    "totalCount": 200,
    "sentCount": 180,
    "replyCount": 7,
    "deliveryRate": 0.9,
    "unreadCount": 1,
    "status": 1
  },
  "message": ""
}
```

## 3. 单条发送
### 3.1 现代接口 `POST /api/sms/messages`
**请求**
```json
{
  "taskId": 9123,
  "userId": 400,
  "targetNumber": "+12135550123",
  "messageContent": "Hello there!",
  "mediaUrls": "https://cdn/img.png",
  "messageFrom": "17707695953",
  "messageTo": "+12135550123",
  "sentAt": "2025-11-07T13:00:00Z",
  "status": 0,
  "read": 1
}
```
**响应**
```json
{
  "success": true,
  "data": {
    "messageId": 5011,
    "status": 0,
    "read": 1,
    "smsResult": {
      "code": 200,
      "message": "",
      "data": { "source": "17707695953", "source_id": "abc123" }
    }
  },
  "message": ""
}
```

### 3.2 旧版接口 `POST /api/sms/messages/send`
```json
{
  "success": true,
  "data": { "messageId": 5012, "status": 0, "read": 1 },
  "message": ""
}
```

## 4. 标记已读 `POST /api/sms/messages/read`
**请求** `{"taskId": 9123, "targetNumber": "+12135550123"}`
**响应** `{"success": true, "data": true, "message": ""}`

## 5. 联系人与聊天
### 5.1 联系人列表 `POST /api/sms/tasks/contacts`
```json
{
  "success": true,
  "data": [
    {
      "targetNumber": "12135550123",
      "latestMessage": "See you!",
      "latestTime": "2025-11-07 21:08:00",
      "unreadCount": 1
    }
  ],
  "message": ""
}
```

### 5.2 聊天记录 `POST /api/sms/tasks/contacts/messages`
```json
{
  "success": true,
  "data": [
    {
      "messageId": 5010,
      "messageContent": "Hello there!",
      "mediaUrls": null,
      "messageTo": "12135550123",
      "messageFrom": "17707695953",
      "sendAt": "2025-11-07 21:07:00",
      "status": 0
    }
  ],
  "message": ""
}
```

## 6. Webhook `/api/sms/webhook`（兼容旧 `/api/sms`）
**请求**
```json
{
  "messagesid": "abc123",
  "from": "+12135550123",
  "source": "17707695953",
  "message": "Got it",
  "mediaurls": "http://img.example.com/a.jpg",
  "receiveddatetime": "2025-11-07T21:05:00Z"
}
```
**响应**
```json
{
  "success": true,
  "data": {
    "messageSid": "abc123",
    "messageFrom": "12135550123",
    "messageTo": "17707695953",
    "source": "17707695953",
    "messageContent": "Got it",
    "mediaUrls": "http://img.example.com/a.jpg",
    "receivedDatetime": "2025-11-07T21:05:00Z",
    "targetNumber": "12135550123"
  },
  "message": "Webhook已写入历史"
}
```

> `messageFrom`/`messageTo` 会自动去掉 `+`、空白，完全兼容 Python 版本。
