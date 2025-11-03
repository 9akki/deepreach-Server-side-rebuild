# 阿里云ECD配置系统重构说明

## 概述

本次重构解决了 `AliyunCloudComputerServiceImpl` 类中的硬编码问题，将所有配置参数改为从数据库中动态读取，并实现了与云电脑业务表的完整集成，提高了系统的灵活性和可维护性。

## 变更内容

### 1. 数据库表结构

1. **配置参数表**: 创建了 `t_aliyun_ecd_config` 表来存储阿里云ECD配置参数：

```sql
CREATE TABLE IF NOT EXISTS t_aliyun_ecd_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value VARCHAR(500) NOT NULL COMMENT '配置值',
    description VARCHAR(200) COMMENT '配置描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阿里云ECD配置参数表';
```

2. **业务表**: 集成了现有的云电脑业务表：
   - `t_cloud_user`: 云用户表
   - `t_computer`: 电脑信息表
   - `t_user_computer`: 用户电脑关联表

### 2. 新增文件

1. **配置实体类**: `deepreach-web/src/main/java/com/deepreach/web/entity/AliyunEcdConfig.java`
   - 定义了配置参数的实体模型

2. **配置Mapper接口**: `deepreach-web/src/main/java/com/deepreach/web/mapper/AliyunEcdConfigMapper.java`
   - 提供了配置参数的数据库操作接口

3. **配置服务接口**: `deepreach-web/src/main/java/com/deepreach/web/service/AliyunEcdConfigService.java`
   - 定义了配置管理的业务接口

4. **配置服务实现**: `deepreach-web/src/main/java/com/deepreach/web/service/impl/AliyunEcdConfigServiceImpl.java`
   - 实现了配置管理的业务逻辑

5. **业务Mapper接口**:
   - `deepreach-web/src/main/java/com/deepreach/web/mapper/CloudUserMapper.java`: 云用户数据访问
   - `deepreach-web/src/main/java/com/deepreach/web/mapper/ComputerMapper.java`: 电脑信息数据访问
   - `deepreach-web/src/main/java/com/deepreach/web/mapper/UserComputerMapper.java`: 用户电脑关联数据访问

6. **MyBatis映射文件**:
   - `deepreach-web/src/main/resources/mapper/CloudUserMapper.xml`
   - `deepreach-web/src/main/resources/mapper/ComputerMapper.xml`
   - `deepreach-web/src/main/resources/mapper/UserComputerMapper.xml`

7. **SQL脚本**: `aliyun_ecd_config_table.sql`
   - 包含创建配置表和插入初始数据的SQL语句

### 3. 修改的文件

1. **AliyunCloudComputerServiceImpl**:
   - 移除了所有硬编码的配置参数
   - 添加了从数据库动态读取配置的逻辑
   - 集成了云用户、电脑信息和用户电脑关联的查询
   - 增加了配置参数和业务数据完整性检查
   - 使用数据库中的实际电脑ID和办公站点ID

2. **application.yml**:
   - 添加了阿里云ECD配置段（仅作为默认值参考）

## 使用说明

### 1. 初始化数据库

1. 执行 `cloud_computer_tables.sql` 脚本来创建云电脑业务表
2. 执行 `aliyun_ecd_config_table.sql` 脚本来创建配置表并插入初始配置数据

### 2. 配置管理

1. **配置参数**: 所有阿里云ECD相关的配置参数存储在 `t_aliyun_ecd_config` 表中，包括：
   - `access.key.id`: 阿里云AccessKeyId
   - `access.key.secret`: 阿里云AccessKeySecret
   - `endpoint`: 阿里云ECD服务端点
   - `api.version`: API版本
   - `action`: API操作
   - `region.id`: 区域ID
   - `client.id`: 客户端ID
   - `office.site.id`: 办公站点ID（默认值，可被电脑表中的值覆盖）

2. **业务数据**: 云电脑业务数据存储在以下表中：
   - `t_cloud_user`: 存储云用户信息，包含终端用户ID和客户端用户名
   - `t_computer`: 存储电脑信息，包含电脑ID、办公站点ID和电脑名称
   - `t_user_computer`: 存储用户和电脑的关联关系

### 3. 动态配置

系统启动时，`AliyunCloudComputerServiceImpl` 会从数据库中读取所有启用的配置参数和业务数据，无需重启应用即可通过修改数据库表来更新配置。

### 4. 配置状态

每个配置参数都有一个状态字段（status），可以启用或禁用特定配置。只有状态为1（启用）的配置才会被读取和使用。

### 5. 业务流程

1. 用户请求登录令牌时，系统会：
   - 从配置表读取阿里云ECD API配置参数
   - 根据终端用户ID查询用户信息
   - 查询用户关联的云电脑
   - 获取电脑的详细信息（包括电脑ID和办公站点ID）
   - 使用这些信息调用阿里云ECD API获取登录令牌

2. 如果API调用失败，系统会返回模拟数据，使用数据库中的实际电脑ID和客户端ID

## 优势

1. **灵活性**: 可以通过修改数据库表来动态调整配置和业务数据，无需修改代码
2. **可维护性**: 配置和业务数据集中管理，便于维护和审计
3. **安全性**: 敏感信息（如AccessKeySecret）可以更好地控制和保护
4. **扩展性**: 可以轻松添加新的配置参数和业务数据
5. **数据一致性**: 系统使用数据库中的实际业务数据，确保数据一致性
6. **业务集成**: 完全集成了云电脑业务流程，支持多用户和多电脑管理

## 注意事项

1. 确保数据库连接配置正确，能够访问到cloud-computer数据库
2. 修改配置或业务数据后，下次调用API时会自动使用新数据
3. 如果必要的配置参数缺失或被禁用，系统会记录错误日志并抛出异常
4. 用户必须有关联的云电脑才能获取登录令牌
5. 电脑表中的办公站点ID会覆盖配置表中的默认值