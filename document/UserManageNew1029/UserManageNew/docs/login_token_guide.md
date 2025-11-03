# Login Token 与关联参数获取指南

更新时间：2025-10-29

## 1. 文档目的

本文说明如何在阿里云弹性云桌面（Elastic Cloud Desktop, ECD）环境中获取 Login Token、Session ID、Desktop ID、Login Region ID 四个关键参数，并提供所需 API 及操作步骤，便于应用程序或自动化脚本完成云桌面登录流程。

## 2. 前置条件

- 拥有已开通 ECD 服务的阿里云账号（主账号或具备相应权限的 RAM 子账号）。
- 已创建且处于可用状态的桌面或桌面组，知晓其所属地域（Region）。
- 拥有一对有效的 AccessKey ID/Secret，用于签名 OpenAPI 请求。
- 本地已安装并配置 Java 运行环境或任意 HTTP 客户端工具（如 `curl`、Postman、OpenAPI Explorer）。

## 3. 参数概览

| 参数 | 获取方式 |
| --- | --- |
| Login Token | 调用 `GetLoginToken`，从响应中读取 `LoginToken` 字段 |
| Session ID | 调用 `GetLoginToken`，从响应中读取 `SessionId` 字段 |
| Desktop ID | 调用 `DescribeDesktops` 或在控制台复制 `DesktopId` |
| Login Region ID | 调用 `GetLoginToken` 时直接传入外部提供的 `RegionId` 参数 |

## 4. 获取流程

### 4.1 准备 Login Region ID

- 若业务流程已传入 `Login Region ID`（例如 `cn-hangzhou`），直接带入后续 API 请求，无需额外查询。
- 若仍需确认可用地域，可调用 `DescribeRegions` 列出账号可用地域后再选择目标 `RegionId`。

### 4.2 获取 Desktop ID

1. **控制台方式**：在“弹性云桌面 → 桌面管理”页面中，复制目标桌面的 `桌面 ID`。
2. **OpenAPI 方式**：调用 `DescribeDesktops` 接口，按需传入 `RegionId`、`DesktopName` 或标签等过滤条件，从响应数组中读取 `DesktopId` 字段。该接口同样会返回桌面状态，可用于校验是否处于 `Running`。

示例请求体（以 JSON 表示参数含义）：

```json
{
  "Action": "DescribeDesktops",
  "Version": "2020-10-02",
  "RegionId": "cn-hangzhou",
  "DesktopId": [
    "ecd-xxxxxxxxxxxxxxxxxxxxx"
  ]
}
```

### 4.3 申请 Login Token 与 Session ID

在已确认 `DesktopId` 与 `RegionId` 的前提下，调用 `GetLoginToken` 接口，提交登录主体（账号、验证方式）等必要参数，接口返回的 `LoginToken` 与 `SessionId` 需后续与云桌面客户端对接。

常用输入字段：

- `RegionId`：步骤 4.1 获取的地域 ID。
- `EndUserId`：对应云桌面授权用户的用户名。
- `LoginTokenExpire`（可选）：自定义令牌有效期（单位：分钟）。
- `DesktopId` / `DesktopGroupId`：指定登录目标。

OpenAPI Explorer 或 `curl` 请求示例：

````bash
curl -X POST "https://ecd.aliyuncs.com" \
  -d "Action=GetLoginToken" \
  -d "Version=2020-10-02" \
  -d "RegionId=cn-hangzhou" \
  -d "EndUserId=alice" \
  -d "DesktopId=ecd-xxxxxxxxxxxxxxxxxxxxx" \
  -H "x-acs-signature-method:HMAC-SHA1" \
  -H "x-acs-version:2020-10-02" \
  -H "Content-Type:application/x-www-form-urlencoded"
````

返回体关键字段说明：

- `LoginToken`：传给桌面客户端或后续 API 的核心凭据。
- `SessionId`：用于轮询 `DescribeDesktops` 或 `GetConnectionTicket` 等接口时校验会话状态。
- `NextStage`：若开启多因素认证，会显示下一步验证要求。

### 4.4 整体调用顺序参考

1. 调用下游流程时直接传入既有的 `RegionId`；若暂未确定，可先使用 `DescribeRegions` 查询后再选择目标地域。
2. 通过控制台或 `DescribeDesktops` 确认 `DesktopId` 是否可用。
3. 使用 `GetLoginToken` 获取 `LoginToken` 与 `SessionId`，必要时根据 `NextStage` 完成二次验证。
4. 将 `RegionId`、`DesktopId`、`LoginToken`、`SessionId` 四个参数交付客户端或业务系统用于后续连接。

## 5. 常见问题与排查提示

- **令牌过期**：`LoginToken` 有效期较短，推荐在客户端发起连接前现取现用，并处理超时后的重发逻辑。
- **桌面状态异常**：若 `DescribeDesktops` 返回桌面状态非 `Running`，需先在控制台启动桌面再获取令牌。
- **地域或账号权限不足**：若 `DescribeRegions` 无返回目标地域，请检查账号是否已在该地域开通 ECD 服务或 RAM 策略是否授权。

## 6. 记录与持续改进

- 建议在团队知识库中登记调用过程与脚本，并定期校验 AccessKey 的有效性。
- 若采用自动化流程，可将上述接口封装为定时任务或函数服务，确保日志记录完整，便于追踪问题。
