# Seata 分布式事务实施方案

## 一、为什么选 Seata AT 模式

| 对比维度 | AT 模式 | TCC 模式 | Saga 模式 |
|---------|:---:|:---:|:---:|
| 代码侵入 | **低**（仅加注解） | 高（每个接口写 Try/Confirm/Cancel） | 中（手动补偿） |
| 回滚机制 | **自动**生成 undo_log | 手动实现补偿 | 手动实现补偿 |
| 适用场景 | 关系型数据库操作 | 复杂业务含外部系统 | 长事务/高吞吐 |
| 改造工作量 | **小** | 大 | 中 |
| 性能 | 中（多一次 undo_log 写入） | 高 | 高 |

### 选择 AT 模式的理由

1. 本项目跨服务操作都是 **MySQL + Feign 调用**，无外部不可回滚的系统
2. 现有代码只加注解即可，**无需重构业务逻辑**
3. TCC 需要为每个写操作写 3 套代码（Try/Confirm/Cancel），工作量至少 **3 倍**
4. 判题流程对一致性要求高但吞吐量可接受，AT 模式的 undo_log 开销可以承受

---

## 二、架构图

```
                    ┌─────────────────────────┐
                    │    Seata Server (TC)     │
                    │    192.168.141.129       │
                    │    端口: 8091/7091        │
                    │    Docker 部署            │
                    └──────┬──────────────────┘
                           │
              ┌────────────┼──────────────┐
              │            │              │
       ┌──────▼──────┐┌───▼────────┐┌───▼──────────┐
       │ Nacos       ││ MySQL      ││ seata-server │
       │ 注册/配置    ││ 存undo_log ││ 全局事务日志   │
       └─────────────┘└────────────┘└──────────────┘
              │
    ┌─────────┴──────────────────────────┐
    │         微服务 (TM/RM)              │
    │  ┌──────┐ ┌──────┐ ┌──────┐       │
    │  │ user │ │problem│ │contest│ ...  │
    │  │ :8081│ │ :8082│ │ :8083│       │
    │  └──────┘ └──────┘ └──────┘       │
    │  @GlobalTransactional             │
    │  @Transactional                   │
    └────────────────────────────────────┘
```

**角色说明：**
- **TC** (Transaction Coordinator)：Seata Server，协调全局事务
- **TM** (Transaction Manager)：标注 `@GlobalTransactional` 的方法发起方
- **RM** (Resource Manager)：参与事务的微服务，管理分支事务和 undo_log

---

## 三、Docker 部署 Seata Server

### 3.1 前置条件

- Nacos 已运行在 `192.168.141.129:8848`
- MySQL 可用，新建数据库 `seata` 用于存储事务日志
- Seata Server 与 Nacos 同机部署（虚拟机 `192.168.141.129`）

### 3.2 创建 Seata 数据库

```sql
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARSET utf8mb4;

USE seata;

CREATE TABLE IF NOT EXISTS global_table (
    xid VARCHAR(128) NOT NULL PRIMARY KEY,
    transaction_id BIGINT,
    status TINYINT NOT NULL,
    application_id VARCHAR(32),
    transaction_service_group VARCHAR(32),
    transaction_name VARCHAR(128),
    timeout INT,
    begin_time BIGINT,
    application_data VARCHAR(2000),
    gmt_create DATETIME,
    gmt_modified DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS branch_table (
    branch_id BIGINT NOT NULL PRIMARY KEY,
    xid VARCHAR(128) NOT NULL,
    transaction_id BIGINT,
    resource_group_id VARCHAR(32),
    resource_id VARCHAR(256),
    branch_type VARCHAR(8),
    status TINYINT,
    client_id VARCHAR(64),
    application_data VARCHAR(2000),
    gmt_create DATETIME,
    gmt_modified DATETIME,
    KEY idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lock_table (
    row_key VARCHAR(128) NOT NULL PRIMARY KEY,
    xid VARCHAR(128),
    transaction_id BIGINT,
    branch_id BIGINT NOT NULL,
    resource_id VARCHAR(256),
    table_name VARCHAR(32),
    pk VARCHAR(36),
    gmt_create DATETIME,
    gmt_modified DATETIME,
    KEY idx_branch_id (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.3 Seata Server 配置

在 `192.168.141.129` 上创建 `/data/seata/application.yml`：

```yaml
server:
  port: 7091

seata:
  config:
    type: nacos
    nacos:
      server-addr: 192.168.141.129:8848
      namespace: public
      group: SEATA_GROUP
      username: nacos
      password: nacos
      data-id: seata.properties
  registry:
    type: nacos
    nacos:
      server-addr: 192.168.141.129:8848
      namespace: public
      group: SEATA_GROUP
      username: nacos
      password: nacos
      application: seata-server
  store:
    mode: db
    db:
      datasource: druid
      db-type: mysql
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://192.168.141.1:3306/seata?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      user: root
      password: qwer1234
      min-conn: 10
      max-conn: 100
  security:
    secretKey: SeataSecretKey2024OJ
    tokenValidityInMilliseconds: 1800000
```

> **注意**：容器内访问宿主机 MySQL 使用 `192.168.141.1`（非 localhost）。

### 3.4 Nacos 添加 Seata 配置

在 Nacos 控制台 `http://192.168.141.129:8848/nacos` 创建配置：

- **Data ID**：`seata.properties`
- **Group**：`SEATA_GROUP`
- **配置内容**：

```properties
service.vgroupMapping.default_tx_group=default
store.mode=db
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.cj.jdbc.Driver
store.db.url=jdbc:mysql://192.168.141.1:3306/seata?useUnicode=true
store.db.user=root
store.db.password=qwer1234
store.db.minConn=5
store.db.maxConn=30
```

### 3.5 启动 Seata Server

```bash
docker run -d \
  --name seata-server \
  --restart always \
  -p 8091:8091 \
  -p 7091:7091 \
  -v /data/seata/application.yml:/seata-server/resources/application.yml \
  -e SEATA_IP=192.168.141.129 \
  seataio/seata-server:2.1.0
```

验证：访问 `http://192.168.141.129:7091`，用户名/密码 `seata/seata`。

---

## 四、项目中集成 Seata 客户端

### 4.1 根 pom.xml 添加版本管理

```xml
<properties>
    <!-- 新增 Seata -->
    <seata.version>2.1.0</seata.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Seata -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
        </dependency>
        <dependency>
            <groupId>io.seata</groupId>
            <artifactId>seata-spring-boot-starter</artifactId>
            <version>${seata.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 4.2 需要集成 Seata 的服务

| 服务 | 是否集成 | 原因 |
|------|:---:|------|
| oj-user-service | ✅ | 注册时写用户表 + Feign 记录活动 |
| oj-problem-service | ✅ | 题目/题单创建涉及多表 + Feign 记录活动 |
| oj-contest-service | ✅ | 竞赛/报告创建涉及多表 + 写 Redis |
| oj-judge-service | ✅ | 判题落库涉及 4 个服务 + RocketMQ |
| oj-gateway | ❌ | 纯路由，无数据库操作 |
| oj-ai-service | ❌ | 无数据库，纯内存/RAG 操作 |
| oj-common-api | ❌ | JAR 包，无需独立集成 |

### 4.3 服务级依赖

每个需集成的服务 `pom.xml` 添加：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
```

### 4.4 每个服务的 `application.yml` 添加

```yaml
seata:
  registry:
    type: nacos
    nacos:
      server-addr: 192.168.141.129:8848
      namespace: public
      group: SEATA_GROUP
      username: nacos
      password: nacos
      application: seata-server
  config:
    type: nacos
    nacos:
      server-addr: 192.168.141.129:8848
      namespace: public
      group: SEATA_GROUP
      username: nacos
      password: nacos
      data-id: seata.properties
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default
  data-source-proxy-mode: AT
```

### 4.5 每个数据库添加 undo_log 表

```sql
-- 在 oj_user_db, oj_problem_db, oj_contest_db, oj_judge_db 中均执行
CREATE TABLE IF NOT EXISTS undo_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    xid VARCHAR(128) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB NOT NULL,
    log_status INT NOT NULL,
    log_created DATETIME NOT NULL,
    log_modified DATETIME NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 五、项目中具体使用方式

### 5.1 规则：`@Transactional` vs `@GlobalTransactional`

| 场景 | 注解 | 说明 |
|------|------|------|
| 单服务内写多张表 | `@Transactional` | 本地事务，由 Seata AT 代理自动管理 undo_log |
| 跨服务调用（A 服务调 B 服务） | `@GlobalTransactional` | 发起方标注，Seata 自动传播 xid 到下游 |
| 纯读操作 | 无需注解 | 无写入无需事务 |
| MQ 消费者 | `@GlobalTransactional` | 需要额外配置 xid 传递 |

### 5.2 各服务具体使用位置

#### oj-user-service

```java
// UserServiceImpl.java - 用户注册（单表，本地事务即可）
@Override
@Transactional  // 新增
public void saveUser(UserDTO userDTO) {
    User user = new User();
    BeanUtils.copyProperties(userDTO, user);
    user.setPassword(PasswordConstant.DEFAULT_PASSWORD);
    user.setCreatedAt(LocalDateTime.now());
    userMapper.insert(user);
}
```

#### oj-problem-service

```java
// ProblemServiceImpl.java - 题目创建（写2张表 + Feign记录活动）
@Override
@Transactional  // 新增：保护 problem + problem_types_rel 原子性
public void problemSave(ProblemDTO problemDTO) {
    // ...现有代码不变...
    // Feign 记录活动放在事务外以 try-catch 包裹，不影响主流程
}

// ProblemServiceImpl.java - 题目删除（写3张表）
@Override
@Transactional  // 新增
public void deleteAll(List<Integer> ids) {
    // ...删除 problem + problem_types_rel + test_case...
}

// GroupServiceImpl.java - 题单操作
@Override
@Transactional  // 新增
public void saveGroup(GroupDTO groupDTO) { ... }

@Override
@Transactional  // 新增
public void update(GroupDTO groupDTO) { ... }
```

#### oj-contest-service

```java
// ContestServiceImpl.java - 竞赛创建（写2张表）
@Override
@Transactional  // 新增
public void saveContest(ContestDTO contestDTO) { ... }

@Override
@Transactional  // 新增
public void update(ContestDTO contestDTO) { ... }

@Override
@Transactional  // 新增
public void deleteId(Long id) { ... }
```

#### oj-judge-service（核心改造）

`DatabaseUpdateConsumer` 是判题落库的核心，涉及 4 个服务，需要 `@GlobalTransactional`：

```java
// DatabaseUpdateConsumer.java - 判题结果落库
@Override
@GlobalTransactional(name = "judge_result_save", timeoutMills = 300000)  // 新增
public void onMessage(DatabaseUpdateMessage msg) {
    // 注意：RocketMQ 消费者需要额外配置传递 xid
    // 方案：消息体中增加 xid 字段，消费端通过 RootContext.bind(xid) 绑定
    
    // 1. 保存 submission（本地事务，自动记录 undo_log）
    subMissionMapper.insert(submission);
    
    // 2. Feign 调用（Seata 自动通过 FeignRequestInterceptor 传播 xid）
    if (msg.isFirstAc()) {
        userClient.updateUserSolvedCount(msg.getUserId());
    }
    problemClient.updateProblemAcceptance(msg.getProblemId());
    
    // 3. 竞赛排名更新
    if (msg.getContestId() != null && "Accepted".equals(msg.getStatus())) {
        contestClient.updateRankOnAccepted(...);
    }
    
    // WebSocket 推送不受事务影响（不可回滚的外部操作，放最后）
    webSocketServer.sendToAllClient(...);
}
```

### 5.3 Feign 拦截器（自动传播 xid）

在 `oj-common-api` 中添加 Seata Feign 拦截器：

```java
// FeignConfig.java 或新建 SeataFeignConfig.java
@Configuration
public class SeataFeignConfig {
    
    @Bean
    public RequestInterceptor seataRequestInterceptor() {
        return requestTemplate -> {
            String xid = RootContext.getXID();
            if (StringUtils.hasText(xid)) {
                requestTemplate.header(RootContext.KEY_XID, xid);
            }
        };
    }
}
```

### 5.4 RocketMQ 消费者传递 xid

判题流程涉及 RocketMQ，需要在消息中传递 xid：

```java
// JudgeServiceImpl.java - 发送判题任务时
@Override
@GlobalTransactional(name = "judge_submit", timeoutMills = 300000)
public void submit(JudgeSubmitDTO dto) {
    // 本地操作：Redis Lua 限流
    // ...
    
    // 发送 MQ 时传递 xid
    JudgeTaskMessage msg = new JudgeTaskMessage();
    msg.setXid(RootContext.getXID());  // 新增
    rocketMQTemplate.convertAndSend(MqConstant.JUDGE_TASK_TOPIC, msg);
}
```

---

## 六、为什么这样设计

### 6.1 `@Transactional` 选择理由

- Seata AT 模式通过 **DataSourceProxy** 自动代理数据源
- 加 `@Transactional` 后，Spring 本地事务和 Seata 全局事务协同工作
- 本地事务失败 → undo_log 自动回滚；全局事务二阶段通知回滚 → undo_log 中前镜像恢复

### 6.2 `@GlobalTransactional` 选择理由

- 标注在 **发起跨服务调用的方法**上
- Seata 自动做三件事：
  1. 向 TC 注册全局事务（获取 xid）
  2. 通过 Feign 拦截器将 xid 传播到下游服务
  3. 下游服务的 `@Transactional` 自动注册为分支事务
- 任一分支失败 → TC 通知所有分支回滚

### 6.3 为什么不改造到 TCC 模式

| 原因 | 说明 |
|------|------|
| 代码量 | TCC 需要为 7+ 个方法各写 3 套实现，保守估计 **3000+ 行**新代码 |
| 测试成本 | 每个补偿方法需要独立测试，容易遗漏边界情况 |
| 业务匹配度 | 本项目全是数据库操作，AT 模式的 undo_log 是天然适配的 |
| 维护成本 | AT 模式只需维护 undo_log 表，TCC 需要持续维护补偿逻辑 |

### 6.4 WebSocket 在事务最后推送的原因

- WebSocket 是外部不可回滚操作，无法被 Seata 管理
- 放在事务最后：事务提交成功才推送，避免用户收到被回滚的结果
- 如果必须先推送，需要用本地消息表 + 重试机制确保最终一致性

---

## 七、事务流示例：判题落库

```
@GlobalTransactional("judge_result_save")
DatabaseUpdateConsumer.onMessage()
│
├─[分支1] subMissionMapper.insert(submission)  ← oj_judge_db.undo_log
│   └── Seata AT: 自动记录 before-image
│
├─[分支2] userClient.updateUserSolvedCount()   ← oj_user_db.undo_log
│   └── Feign 头携带 xid → UserInternalController → @Transactional
│
├─[分支3] problemClient.updateProblemAcceptance() ← oj_problem_db.undo_log
│   └── Feign 头携带 xid → ProblemInternalController → @Transactional
│
├─[分支4] contestClient.updateRankOnAccepted()    ← Redis (非DB，不参与回滚)
│   └── 失败通过 try-catch 降级，不触发全局回滚
│
└─[分支5] webSocketServer.sendToAllClient()       ← 外部不可回滚
    └── 事务提交成功后执行

如果分支2失败 → TC通知分支1回滚 → undo_log恢复数据
如果分支3失败 → TC通知分支1、分支2回滚 → 三库数据一致
```

---

## 八、实施步骤

| 步骤 | 内容 | 影响 |
|:---:|------|------|
| 1 | 部署 Seata Server（Docker + Nacos 配置） | 基础设施 |
| 2 | 4个业务数据库各加 `undo_log` 表 | 一次性 DDL |
| 3 | 根 pom.xml 加 seata 版本管理 | 依赖管理 |
| 4 | 4个业务服务 pom.xml 加 seata 依赖 | 编译依赖 |
| 5 | 4个业务服务 application.yml 加 seata 配置 | 运行时配置 |
| 6 | oj-common-api 加 seata Feign 拦截器 | 自动传播 xid |
| 7 | 7个 ServiceImpl 方法加 `@Transactional` | 本地事务保护 |
| 8 | DatabaseUpdateConsumer 加 `@GlobalTransactional` | 核心跨服务事务 |
| 9 | JudgeServiceImpl 消息体增加 xid 传递 | MQ 事务传播 |
| 10 | 集成测试验证 | 回归验证 |
