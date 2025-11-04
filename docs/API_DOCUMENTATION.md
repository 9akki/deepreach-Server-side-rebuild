# DeepReach 管理系统接口文档

本文档整理前端项目 `frontend/src/api` 中调用的后端接口，默认使用的接口域名为 `http://206.82.1.18:8080/api`，请求采用 JSON 体，除非另有说明。所有接口需在请求头携带 `Authorization: Bearer <token>`。

## 认证模块

| 方法 | 路径 | 描述 | 请求体 | 响应关键字段 |
| --- | --- | --- | --- | --- |
| POST | `/auth/login` | 后台用户登录 | `{ username, password }` | `accessToken`, `refreshToken`, `user`（含 `backendUser`, `roles`） |
| GET | `/auth/user` | 获取当前用户信息 | — | `user` |
| POST | `/auth/refresh` | 使用刷新令牌获取新 Token | `{ refreshToken }` | `accessToken`, `refreshToken` |
| POST | `/auth/logout` | 预留的登出接口（当前仅在前端清理缓存） | — | — |
| POST | `/auth/switch-role` | 预留的角色切换接口 | `{ role }` | — |

## 仪表盘模块

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| GET | `/dashboard` | 获取仪表盘总览数据 |
| GET | `/dashboard/member-stats` | 获取会员统计 |
| GET | `/dashboard/merchant-stats` | 获取商家统计 |
| GET | `/dashboard/dr-points` | 获取 DR 点数统计 |
| GET | `/dashboard/marketing-instances` | 获取营销实例数据 |
| GET | `/dashboard/customer-acquisition-instances` | 获取拓客实例数据 |

## 会员管理

| 方法 | 路径 | 描述 | 请求参数/体 |
| --- | --- | --- | --- |
| GET | `/members` | 查询会员列表 | Query：`role, account, userId, inviteCode, dateRange, page, pageSize` |
| POST | `/members` | 创建会员 | Body：`{ account, password, role, remark }` |
| PUT | `/members/{memberId}/role` | 更新会员角色或备注 | Body：`{ role, remark, ... }` |
| DELETE | `/members/{memberId}` | 删除会员 | — |
| POST | `/members/export` | 导出会员列表 | Body：筛选条件 |
| GET | `/members/{memberId}` | 获取会员详情 | — |

## 直推管理

| 方法 | 路径 | 描述 | 请求参数/体 |
| --- | --- | --- | --- |
| GET | `/members/{parentUserId}/direct-users` | 查询直推用户列表 | Query：`role, account, userId, inviteCode, dateRange, page, pageSize` |
| POST | `/members/{parentUserId}/direct-users` | 新增直推用户 | Body：`{ account, password, role, remark }` |
| POST | `/members/{userId}/adjust-asset` | 调整直推用户资产 | Body：`{ amount, reason, ... }` |
| POST | `/members/{parentUserId}/direct-users/export` | 导出直推用户列表 | Body：筛选条件 |

## 佣金管理

| 方法 | 路径 | 描述 | 请求参数/体 |
| --- | --- | --- | --- |
| GET | `/members/{userId}/commission-details` | 查询佣金明细 | Query：ID、时间、类型等筛选 |
| POST | `/members/{userId}/adjust-commission` | 调整佣金 | Body：`{ amount, reason, evidence, ... }` |
| POST | `/members/{userId}/commission-details/export` | 导出佣金明细 | Body：筛选条件 |

## 充值管理

| 方法 | 路径 | 描述 | 请求参数/体 |
| --- | --- | --- | --- |
| GET | `/recharge/orders` | 查询充值订单 | Query：状态、时间、角色等 |
| POST | `/recharge/orders/export` | 导出充值订单 | Body：筛选条件 |

## 结算管理

| 方法 | 路径 | 描述 | 请求参数/体 |
| --- | --- | --- | --- |
| GET | `/settlement/orders` | 查询结算订单 | Query：状态、时间等 |
| POST | `/settlement/orders/{orderId}/process` | 审核结算订单 | Body：`{ status, remark, ... }` |
| POST | `/settlement/orders/export` | 导出结算订单 | Body：筛选条件 |

## 价格配置

| 方法 | 路径 | 描述 | 请求参数/体 |
| --- | --- | --- | --- |
| GET | `/dr/price/config/list` | 获取价格配置列表 | Query：`pageNum, pageSize` |
| POST | `/dr/price/config` | 新增价格配置 | Body：`{ businessType, businessName, drPrice, priceUnit, billingType, status, remark }` |
| PUT | `/dr/price/config` | 更新价格配置 | Body：同新增并需携带 `priceId` |
| DELETE | `/dr/price/config/{priceId}` | 删除价格配置 | — |
| POST | `/dr/price/config/export` | 导出价格配置 | Body：筛选条件，返回文件流 |

## 代理端（Mock 预留）

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| GET | `/daili/dashboard` | 代理端仪表盘 |
| GET | `/daili/dashboard/member-stats` | 代理会员统计 |
| GET | `/daili/dashboard/commission` | 佣金看板 |
| GET | `/daili/dashboard/share-info` | 推广分享数据 |
| GET | `/daili/members/{parentUserId}/direct-users` | 代理查看直推 |
| POST | `/daili/members/{parentUserId}/direct-users` | 代理新增直推 |
| POST | `/daili/members/{userId}/adjust-asset` | 代理调整资产 |
| POST | `/daili/members/{parentUserId}/direct-users/export` | 导出代理直推 |

## 商户端（Mock 预留）

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| GET | `/shanghu/merchants/{merchantId}/dashboard` | 商户仪表盘 |
| GET | `/shanghu/merchants/{merchantId}/quick-actions` | 快捷操作建议 |
| GET | `/shanghu/merchants/{merchantId}/notifications` | 通知列表 |
| PUT | `/shanghu/notifications/{notificationId}/read` | 通知已读 |
| GET | `/shanghu/merchants/{merchantId}/asset-details` | 资产明细 |
| GET | `/shanghu/merchants/{merchantId}/asset-overview` | 资产概览 |
| POST | `/shanghu/merchants/{merchantId}/recharge` | 商户充值 |
| POST | `/shanghu/merchants/{merchantId}/withdraw` | 商户提现 |
| POST | `/shanghu/merchants/{merchantId}/asset-details/export` | 导出资产明细 |
| GET | `/shanghu/merchants/{merchantId}/employees` | 员工列表 |
| POST | `/shanghu/merchants/{merchantId}/employees` | 新增员工 |
| PUT | `/shanghu/employees/{employeeId}/status` | 启用/禁用员工 |
| POST | `/shanghu/employees/{employeeId}/adjust-points` | 调整员工积分 |
| POST | `/shanghu/employees/{employeeId}/reset-password` | 重置员工密码 |
| POST | `/shanghu/merchants/{merchantId}/employees/export` | 导出员工列表 |

> **说明**：部分接口目前由前端 Mock 数据实现，真实联调时请确保后端返回结构包含 `success`、`code` 字段，并与表格中列出的业务字段保持一致。

