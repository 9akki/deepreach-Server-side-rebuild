# DeepReach 账号体系数据库设计

## 概述

本文档描述了DeepReach系统的账号体系数据库设计，支持多级代理和买家账户管理，包括充值、扣费、实例管理等核心功能。

**重要说明**：所有扣费逻辑均在应用代码中实现，不使用数据库存储过程、触发器和定时任务。

## 文件说明

- `account_system_extension.sql`: 账号体系核心表结构扩展脚本
- `authority.sql`: 基础认证系统表结构
- `accountDesign.sql`: 现有表结构参考

## 核心设计理念

### 1. 表结构扩展策略

在现有表基础上进行最小化扩展，遵循以下原则：
- **非必要不新增字段**：保持现有表结构简洁
- **业务明确性优先**：增加字段能使业务逻辑更清晰时则添加
- **平衡点**：在新增表和新增字段之间找到最佳平衡点
- **代码实现优先**：业务逻辑在应用代码中实现，数据库只负责数据存储

### 2. 账号体系层级

```
DeepReach科技（总部）
├── 一级代理
│   ├── 二级代理后台（会有多个）
│   │   └── 三级代理后台（会有多个）
│   │         └── 买家总账户后台（会有多个）
│   │                  └── 子账户客户端（会有多个）
│   └── 买家总账号后台
│           └── 子账户客户端
│
├── 一级代理
│
│
└── 买家总账户
        └── 子账户客户端
```

**四种用户类型：**
1. **总后台**：DeepReach科技总部
2. **代理**：一级代理、二级代理、三级代理
3. **买家总账号**：管理子账户和充值
4. **客户端子账号**：实际使用系统的员工账号

## 核心表结构

### 1. 扩展现有表

#### sys_dept (部门表) 扩展字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| dept_type | CHAR(1) | 部门类型（1总部 2一级代理 3二级代理 4三级代理 5买家） |
| agent_code | VARCHAR(50) | 代理编码（唯一标识） |
| parent_agent_code | VARCHAR(50) | 上级代理编码 |
| level | INT(2) | 部门层级 |
| contact_person | VARCHAR(50) | 联系人 |
| contact_phone | VARCHAR(20) | 联系电话 |
| commission_rate | DECIMAL(5,2) | 佣金比例 |
| balance | DECIMAL(10,2) | 账户余额 |
| dr_points | DECIMAL(10,2) | DR积分余额 |
| instance_count | INT(10) | 实例数量 |
| recharge_amount | DECIMAL(10,2) | 充值业绩 |

#### sys_user (用户表) 扩展字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| agent_code | VARCHAR(50) | 所属代理编码（有值表示代理后台用户） |
| parent_user_id | BIGINT(20) | 父用户ID（用于买家子账号） |
| account_type | CHAR(1) | 账号类型（1管理员 2代理 3买家总账号 4买家子账号） |
| marketing_accounts | INT(5) | 营销账号数量 |
| prospecting_accounts | INT(5) | 拓客账号数量 |
| sms_accounts | INT(5) | 短信账号数量 |
| character_consumption | BIGINT(20) | 字符消耗数量 |
| unlock_prospecting_count | INT(5) | 解锁拓客账号数量 |
| virtual_balance | DECIMAL(10,2) | 虚拟币余额 |

**字段说明：**
- **agent_code**：如果此字段有值，表示该用户是代理后台用户；如果为NULL，表示非代理用户
- **account_type**：与agent_code配合使用，更精确地定义用户类型
- **parent_user_id**：仅买家子账号有值，指向其父账号（买家总账号）

#### sys_role (角色表) 扩展字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| role_category | CHAR(1) | 角色类别（1系统角色 2代理角色 3买家角色） |
| max_create_level | INT(2) | 最大创建层级 |
| max_child_accounts | INT(5) | 最大子账号数量 |
| can_view_performance | TINYINT(1) | 是否可查看业绩 |
| can_create_accounts | TINYINT(1) | 是否可创建账号 |
| can_recharge | TINYINT(1) | 是否可充值 |
| can_view_billing | TINYINT(1) | 是否可查看账单 |
| can_config_price | TINYINT(1) | 是否可配置价格 |

### 2. 新增业务表

#### sys_account_config (账户配置表)

存储各种服务的价格配置：
- 营销账号价格：6DR/天
- 拓客账号价格：1DR/天
- 短信价格：0.05DR/条
- 字符价格：10DR/100万字符

#### sys_billing_record (账单记录表)

记录所有充值和消费明细：
- 账单类型：1充值 2消费 3退款
- 结算类型：1秒结秒扣 2日结日扣

#### sys_instance (实例表)

管理所有营销、拓客、短信实例：
- 实例类型：1营销 2拓客 3短信
- 平台：whatsapp, facebook, instagram, tiktok, sms等
- 每日价格：根据实例类型和平台确定
- 最后计费时间：用于应用代码判断是否需要扣费

#### sys_recharge_record (充值记录表)

管理充值申请和处理：
- 支付状态：0待处理 1已成功 2已失败
- Telegram联系方式：用于人工处理

#### sys_agent_relation (代理关系表)

维护代理层级关系：
- 层级关系：1-2级 2-3级 3-4级

#### sys_account_usage (账户使用统计表)

统计各类账号的使用情况：
- 按日期统计使用数量
- 支持多维度查询

## 核心业务流程

### 1. 创号逻辑

1. **项目总后台创建代理账号**：
   - 可以直接创建买家总账号
   - 也可以通过代理层级创建

2. **买家总账号创建子账号**：
   - 买家总账号可以创建员工子账号
   - 子账号密码由总账号设置

3. **员工子账号创建营销和拓客账号**：
   - 每充值100DR可以开1个营销账号
   - 每创建1个营销账号，解锁10个拓客账号

### 2. 扣费逻辑（应用代码实现）

#### 秒结秒扣

- 创建实例成功时实时扣除DR积分
- 首日费用按创建时间比例计算：
  ```
  首日费用 = 每日价格 × (24 - 创建小时) / 24
  ```
  例如：18点创建，价格5DR，则首日费用 = 5 × (24-18)/24 = 1.25DR

#### 日结日扣

- 每天凌晨通过应用定时任务扣费
- 所有现存实例直接扣除全天费用
- 扣费时更新实例的last_billing_time字段

#### 短信扣费

- 按实际使用量扣费
- 每条短信0.05DR
- 实时扣费，余额不足时发送失败

#### 字符扣费

- 每100万字符10DR
- 按实际使用量扣费
- 支持多平台字符统计

### 3. 拓客账号解锁逻辑

- 每创建一个营销账号，解锁每个应用平台最大创建10个拓客账号
- 例如：创建1个whatsapp营销账号，解锁：
  - 10个facebook拓客账号
  - 10个instagram拓客账号
  - 10个tiktok拓客账号
  - 10个短信拓客账号

## 权限控制

### 1. 超管后台（项目总后台）

- 查看和筛选所有账号
- 查看代理和买家总账号的充值业绩、实例数量、DR积分余额
- 查看买家子账号的营销和拓客账号使用数量
- 查看扣费明细账单
- 配置收费价格
- 配置拓客数量解锁规则

### 2. 一级代理后台

- 可创建二级代理账号、三级代理账号和买家总账号
- 查看伞下所有代理账号和买家总账号明细和业绩

### 3. 二级代理后台

- 可创建三级代理账号和买家总账号
- 查看伞下所有代理账号和买家总账号明细和业绩

### 4. 三级代理后台

- 可创建买家总账号
- 查看伞下所有买家总账号明细和业绩

### 5. 商家后台总账号

- 充值虚拟币功能（通过Telegram联系客服）
- 可创建员工客户端账号
- 子账户扣费同步扣商家后台总账户DR
- 查看所有子账号的账号数量和字符消耗数量

## 数据视图

### 1. v_account_detail (账户详细信息视图)

包含用户、部门、角色的完整信息，支持复杂查询。

### 2. v_agent_hierarchy (代理层级视图)

展示代理层级关系和基本信息。

### 3. v_billing_statistics (账单统计视图)

统计各类账单数据，支持业绩分析。

### 4. v_instance_statistics (实例统计视图)

统计各类实例使用情况和费用。

## 应用代码实现指南

### 1. 扣费逻辑实现

#### 创建实例并扣费

```java
@Transactional
public boolean createInstanceWithBilling(Long userId, String instanceType, 
                                       String platform, String instanceName) {
    // 1. 获取用户余额
    User user = userMapper.selectById(userId);
    
    // 2. 获取实例价格
    AccountConfig config = accountConfigMapper.getByType(instanceType);
    
    // 3. 计算首日费用
    BigDecimal firstDayCost = calculateFirstDayCost(config.getPrice());
    
    // 4. 检查余额
    if (user.getVirtualBalance().compareTo(firstDayCost) < 0) {
        return false; // 余额不足
    }
    
    // 5. 创建实例
    Instance instance = new Instance();
    instance.setUserId(userId);
    instance.setInstanceType(instanceType);
    instance.setPlatform(platform);
    instance.setInstanceName(instanceName);
    instance.setDailyPrice(config.getPrice());
    instance.setCreateTime(new Date());
    instanceMapper.insert(instance);
    
    // 6. 扣费
    user.setVirtualBalance(user.getVirtualBalance().subtract(firstDayCost));
    userMapper.updateById(user);
    
    // 7. 记录账单
    BillingRecord billing = new BillingRecord();
    billing.setUserId(userId);
    billing.setBillType("2"); // 消费
    billing.setBillingType("1"); // 秒结秒扣
    billing.setAmount(firstDayCost);
    billing.setBalanceBefore(user.getVirtualBalance().add(firstDayCost));
    billing.setBalanceAfter(user.getVirtualBalance());
    billing.setDescription("创建" + platform + " " + instanceName + "实例首日费用");
    billingMapper.insert(billing);
    
    // 8. 更新用户账号数量
    updateUserAccountCount(userId, instanceType);
    
    return true;
}
```

#### 每日扣费定时任务

```java
@Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
public void dailyInstanceBilling() {
    // 1. 查询需要扣费的实例
    List<Instance> instances = instanceMapper.selectNeedBillingInstances();
    
    for (Instance instance : instances) {
        try {
            processDailyBilling(instance);
        } catch (Exception e) {
            log.error("处理实例扣费失败: {}", instance.getInstanceId(), e);
        }
    }
}

@Transactional
public void processDailyBilling(Instance instance) {
    // 1. 获取用户余额
    User user = userMapper.selectById(instance.getUserId());
    
    // 2. 检查余额
    if (user.getVirtualBalance().compareTo(instance.getDailyPrice()) < 0) {
        // 余额不足，停用实例
        instance.setStatus("1"); // 停用
        instanceMapper.updateById(instance);
        return;
    }
    
    // 3. 扣费
    user.setVirtualBalance(user.getVirtualBalance().subtract(instance.getDailyPrice()));
    userMapper.updateById(user);
    
    // 4. 记录账单
    BillingRecord billing = new BillingRecord();
    billing.setUserId(instance.getUserId());
    billing.setBillType("2"); // 消费
    billing.setBillingType("2"); // 日结日扣
    billing.setAmount(instance.getDailyPrice());
    billing.setBalanceBefore(user.getVirtualBalance().add(instance.getDailyPrice()));
    billing.setBalanceAfter(user.getVirtualBalance());
    billing.setDescription(instance.getPlatform() + " " + instance.getInstanceName() + " 每日费用");
    billingMapper.insert(billing);
    
    // 5. 更新实例最后扣费时间
    instance.setLastBillingTime(new Date());
    instanceMapper.updateById(instance);
}
```

#### 短信扣费

```java
@Transactional
public boolean smsBilling(Long userId, Integer smsCount) {
    // 1. 获取短信价格
    AccountConfig config = accountConfigMapper.getByType("3");
    BigDecimal totalCost = config.getPrice().multiply(new BigDecimal(smsCount));
    
    // 2. 获取用户余额
    User user = userMapper.selectById(userId);
    
    // 3. 检查余额
    if (user.getVirtualBalance().compareTo(totalCost) < 0) {
        return false; // 余额不足
    }
    
    // 4. 扣费
    user.setVirtualBalance(user.getVirtualBalance().subtract(totalCost));
    userMapper.updateById(user);
    
    // 5. 记录账单
    BillingRecord billing = new BillingRecord();
    billing.setUserId(userId);
    billing.setBillType("2"); // 消费
    billing.setBillingType("1"); // 秒结秒扣
    billing.setAmount(totalCost);
    billing.setBalanceBefore(user.getVirtualBalance().add(totalCost));
    billing.setBalanceAfter(user.getVirtualBalance());
    billing.setDescription("发送" + smsCount + "条短信");
    billingMapper.insert(billing);
    
    // 6. 更新短信使用统计
    updateAccountUsage(userId, "3", smsCount);
    
    return true;
}
```

#### 子账号同步扣费

```java
@Transactional
public boolean syncSubAccountBilling(Long subUserId, BigDecimal amount, String description) {
    // 1. 获取子账号信息
    User subUser = userMapper.selectById(subUserId);
    if (!"4".equals(subUser.getAccountType())) {
        throw new BusinessException("非子账号");
    }
    
    // 2. 获取父账号
    User parentUser = userMapper.selectById(subUser.getParentUserId());
    
    // 3. 检查父账号余额
    if (parentUser.getVirtualBalance().compareTo(amount) < 0) {
        return false; // 父账号余额不足
    }
    
    // 4. 从父账号扣费
    parentUser.setVirtualBalance(parentUser.getVirtualBalance().subtract(amount));
    userMapper.updateById(parentUser);
    
    // 5. 记录父账号账单
    BillingRecord parentBilling = new BillingRecord();
    parentBilling.setUserId(parentUser.getUserId());
    parentBilling.setBillType("2"); // 消费
    parentBilling.setBillingType("1"); // 秒结秒扣
    parentBilling.setAmount(amount);
    parentBilling.setBalanceBefore(parentUser.getVirtualBalance().add(amount));
    parentBilling.setBalanceAfter(parentUser.getVirtualBalance());
    parentBilling.setDescription("子账号扣费：" + description);
    billingMapper.insert(parentBilling);
    
    // 6. 更新子账号余额（仅用于记录）
    subUser.setVirtualBalance(subUser.getVirtualBalance().subtract(amount));
    userMapper.updateById(subUser);
    
    // 7. 记录子账号账单
    BillingRecord subBilling = new BillingRecord();
    subBilling.setUserId(subUserId);
    subBilling.setBillType("2"); // 消费
    subBilling.setBillingType("1"); // 秒结秒扣
    subBilling.setAmount(amount);
    subBilling.setBalanceBefore(subUser.getVirtualBalance().add(amount));
    subBilling.setBalanceAfter(subUser.getVirtualBalance());
    subBilling.setDescription(description);
    billingMapper.insert(subBilling);
    
    return true;
}
```

### 2. 数据库查询示例

#### 获取代理下级用户

```java
public List<User> getAgentUsers(String agentCode) {
    return userMapper.selectList(
        new QueryWrapper<User>()
            .eq("agent_code", agentCode)
            .or()
            .inSql("dept_id",
                "SELECT dept_id FROM sys_dept WHERE parent_agent_code = '" + agentCode + "'")
    );
}
```

#### 判断用户是否为代理

```java
public boolean isAgentUser(User user) {
    // 方法1：通过agent_code判断
    return StringUtils.isNotBlank(user.getAgentCode());
    
    // 方法2：通过account_type判断
    return "2".equals(user.getAccountType());
    
    // 推荐使用agent_code判断，更灵活
}
```

#### 获取用户所属代理信息

```java
public Dept getAgentInfo(User user) {
    if (StringUtils.isBlank(user.getAgentCode())) {
        return null; // 非代理用户
    }
    
    return deptMapper.selectOne(
        new QueryWrapper<Dept>()
            .eq("agent_code", user.getAgentCode())
            .eq("del_flag", "0")
    );
}
```

#### 计算代理业绩

```java
public AgentPerformance calculateAgentPerformance(String agentCode, Date startDate, Date endDate) {
    // 使用视图查询
    return agentPerformanceMapper.selectByAgentAndDateRange(agentCode, startDate, endDate);
}
```

## 使用指南

### 1. 初始化数据库

```bash
# 1. 执行基础表结构
mysql -u root -p your_database < authority.sql

# 2. 执行账号体系扩展
mysql -u root -p your_database < account_system_extension.sql
```

### 2. 应用配置

确保应用中配置了：
- 定时任务框架（如Spring Task）
- 事务管理
- 数据库连接池
- 日志记录

### 3. 创建示例账号

```java
// 创建一级代理账号
userService.createAgentAccount("AGENT_001", "agent001", "password", "一级代理1", 
                            "agent001@example.com", "13800138001");

// 创建买家总账号
userService.createBuyerAccount("BUYER_001", "buyer001", "password", "买家1", 
                             "buyer001@example.com", "13800138002");

// 创建买家子账号
userService.createSubAccount(101, "sub001", "password", "子账号1", 
                           "sub001@example.com", "13800138003");
```

### 4. 创建实例

```java
// 创建营销账号实例
instanceService.createInstanceWithBilling(101, "1", "whatsapp", "WhatsApp营销1");

// 创建拓客账号实例
instanceService.createInstanceWithBilling(101, "2", "facebook", "Facebook拓客1");
```

### 5. 充值操作

```java
// 记录充值申请
rechargeService.createRechargeRecord(101, new BigDecimal("100.00"), 
                                  new BigDecimal("100.00"), "@customer_service", "充值100DR");

// 人工处理后上分
rechargeService.processRecharge(1L, "admin"); // recharge_id=1, process_by=admin
```

## 注意事项

1. **余额检查**：所有扣费操作前都会检查余额，余额不足时操作失败
2. **数据同步**：子账号扣费需要同步到父账号，确保数据一致性
3. **实例状态**：余额不足时实例会自动停用，充值后需要手动启用
4. **权限控制**：不同角色有不同的数据访问权限，确保数据安全
5. **审计日志**：所有重要操作都会记录到操作日志表，便于追踪
6. **并发安全**：所有余额操作需要考虑并发安全，建议使用数据库事务
7. **定时任务**：每日扣费需要通过应用定时任务实现，建议在凌晨1点执行

## 扩展建议

1. **多租户支持**：可以添加tenant_id字段支持多租户
2. **缓存优化**：对频繁查询的数据添加Redis缓存
3. **消息队列**：对扣费等关键操作使用消息队列确保可靠性
4. **监控告警**：添加余额不足、实例异常等监控告警
5. **数据分析**：基于现有数据构建更丰富的数据分析功能
6. **API限流**：对扣费等敏感操作添加API限流保护
7. **分布式锁**：在分布式环境中使用分布式锁确保扣费操作的原子性