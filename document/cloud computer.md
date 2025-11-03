---
title: cloud computer
createTime: 2025-10-30 22:05:03 星期四 晚上
lastModifyTime: 2025-10-30 22:34:28 星期四 晚上
---

# 数据库

```
URL = "jdbc:mysql://206.82.1.18:33900/cloud-computer?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai";

USER = "root";

密码8n2MDPdMb4qpRHYR
```

## 一、接口基本信息

- **接口名称**：getCloudComputerParameter
- **请求方式**：GET
- **入参**：user id（用户唯一标识）
- **核心逻辑**：通过用户 ID 判断是否分配云电脑，仅返回未分配云电脑的失败包；若已分配，则返回指定参数。

## 二、云电脑分配判断规则

1. 关联表字段：t_cloud_user 表的 `client_name` 字段，与 sys_user 表的 `username` 字段一一对应。
2. 判断逻辑：
    - 若通过 `client_name` 与 `username` 能匹配查询到数据，代表用户已分配云电脑。
    - 若无法匹配查询到数据，代表用户未分配云电脑，此时返回 " 未分配云电脑 " 的失败包。

## 三、返回参数说明（修订）

|参数名称|含义|获取方式|
|---|---|---|
|loginToken|登录令牌|参考 ZIP 文件，运行其中程序后，从日志输出中获取。|
|ClientId|客户端唯一标识|随机生成的 UUID 格式字符串。|
|computerId|云电脑唯一标识|从 "loginRegionId 获取流程 " 中同步获取，即查询 t_user_computer 表时得到的 `computer_id`。|
|loginRegionId|登录区域标识|1. 从 t_cloud_user 表获取当前用户的 `end_user_id`；<br><br>2. 用 `end_user_id` 查询 t_user_computer 表，同步得到 `computer_id`（即 computerId 参数）；<br><br>3. 用 `computer_id` 查询 t_computer 表，得到 `office_siteId`；<br><br>4. 截取 `office_siteId` 中 "+" 前的部分（如 `us-west-1+dir-5588339126` 截取后为 `us-west-1`），即为 loginRegionId。|
