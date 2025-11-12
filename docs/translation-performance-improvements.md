# 翻译接口性能/可靠性改造说明

## 1. 缓存改造

- **扣费账号解析 (ChargeAccountResolver)**  
  - 为 `sys_user` 的层级查询增加 5 分钟 TTL 缓存，命中率高的场景不再重复执行多表 JOIN。  
  - 当用户余额被消费或外部操作需要刷新时，可调用 `ChargeAccountResolver.evict(userId)` 立即失效缓存，确保角色/父子关系的更新能在下一次请求生效。

- **余额快照 (ConsumptionBalanceGuard)**  
  - 新增 5 秒 TTL 的余额缓存，仅用于「服务前余额校验」阶段，真正扣费仍依赖数据库乐观锁保证一致性。  
  - 当扣费成功后，消费者会调用 `evictBalance(chargeUserId)`，因此缓存至多脏读一个周期。  
  - 如果缓存判断余额不足，会强制刷新一次后再作结论，避免因旧数据误报。

- **计价配置 (TranslationBillingServiceImpl)**  
  - 翻译 token 单价加了 60 秒缓存，避免每次请求命中 `dr_price_config`。管理端调整价格后可主动清缓存或等待 TTL 过期。

### 一致性保障

1. 缓存只用于“快照”或“默认值”，真正扣费仍由 `update ... where version=?` 决定；一旦 DB 写成功即回写/驱逐缓存。  
2. 任何状态异常（账户冻结、余额不足）都会触发强制刷新，从数据库重新获取最新状态。  
3. 如需立即使缓存失效（例如手动调整余额），调用 `ConsumptionBalanceGuard.evictBalance(userId)` 即可。

## 2. Kafka 异步扣费

- **发送端**：去掉 Kafka 事务配置，扣费事件发送改为异步 fire-and-forget，彻底消除 InitProducerId 带来的 3~4 秒阻塞。若发送失败仅记录 error 日志，需要依赖监控或补偿流程。
- **消费端 (TranslationChargeConsumer)**：
  - 针对乐观锁冲突或“请稍后重试”类错误做最多 3 次快速重试，仍失败则写入 DLQ（默认 `translation-charge-dlq`），并抛出 `ServiceException` 提醒观察。  
  - 扣费成功后会立刻清除余额缓存，保证下一次校验看到最新余额。  
  - 运维可通过监控 DLQ Topic，结合 eventId 进行人工或后台补扣，防止漏账。

## 3. 运维建议

1. **缓存观测**：关注 `TranslationServiceImpl` 日志中的 `cost(ms)` 字段，如 `balance` 段重新飙升，可考虑临时关闭缓存或检查 Redis/Caffeine 命中。  
2. **DLQ 处理**：为 `translation-charge-dlq` 建立监控/告警，及时消费或人工重放其中的事件，防止长时间欠费。  
3. **价格变更**：修改 `dr_price_config` 后可重启翻译服务或调用管理接口触发缓存刷新，避免 60 秒内使用旧单价。  
4. **用户变更**：若发现用户角色/父子关系变动后仍旧数据，可调用提供的缓存失效接口或等待 5 分钟 TTL 过期。  
5. **Kafka 重试**：若 DLQ 中大量累积，优先排查 `user_dr_balance` 乐观锁冲突（版本号）、余额不足或账号被禁用等场景。

## 4. 死信队列处理流程

DLQ (`translation-charge-dlq`) 中的消息代表翻译已成功但扣费未入账，建议按以下步骤处理：

1. **监控告警**：为 DLQ Topic 设置实时告警，记录 `eventId / chargeUserId / amount / reason`，出现新消息立即跟进。  
2. **人工或脚本补扣**：导出消息后调用内部脚本或接口重新执行 `userDrBalanceService.deductWithDailyAggregation`，成功后记录处理日志并标记已完成，避免重复扣费。  
3. **自动化补偿**：项目内新增的 `TranslationChargeDlqConsumer` 会监听 `translation-charge-dlq`，自动重扣最多 5 次，每次间隔 2 秒；成功即 ACK，失败则重新入队并递增 `retryCount`。达到上限会输出错误日志提醒人工处理。  
4. **积压分析**：统计 reason 字段，确认是乐观锁冲突、余额不足还是账号异常，并针对性优化主流程或提醒用户充值。  
5. **对账**：将 DLQ 的补偿结果纳入财务对账，确保每次翻译最终都被扣费或有明确欠费记录。

通过以上调整，翻译接口的主要瓶颈已回到 LLM 外部调用本身，内部扣费和账户校验的延迟大幅下降，并具备了更明确的补偿策略。*** End Patch
