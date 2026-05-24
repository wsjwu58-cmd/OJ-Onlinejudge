# OJ-Project (微服务架构)

在线判题系统（Online Judge），微服务架构，支持代码提交、自动判题、AI辅助判题、竞赛管理、题单管理。

## 项目目录结构

```
E:\vue-project1\                                        # 用户端前端 (Vue 3 + Element Plus + Monaco Editor)
│   ├── package.json                                   # npm scripts: dev / build / preview
│   ├── vite.config.js
│   └── src/
│       ├── api/                                       # API 封装 → 全部指向 Gateway
│       ├── router/                                    # 路由配置
│       ├── store/                                     # Pinia 状态管理
│       ├── utils/                                     # 工具函数 (request.js Axios封装)
│       ├── components/                                # 公共组件
│       └── views/                                     # 页面视图

├── vue-Element\                                       # 管理端前端 (Vue 3 + Element Plus + ECharts)
│   ├── package.json                                   # npm scripts: dev / build / preview
│   ├── vite.config.js
│   └── src/
│       ├── api/                                       # API 封装 → 全部指向 Gateway
│       ├── router/                                    # 路由配置
│       ├── util/                                      # 工具函数
│       ├── components/                                # 公共组件 (Layout, CountTo)
│       └── views/                                     # 页面视图
│
├── oj-microservice/                                   # 后端微服务 (Maven 多模块)
│   ├── pom.xml                                        # 父POM (Spring Boot 3.4.2 + Spring Cloud, Java 17)
│   │
│   ├── oj-common/                                     # 公共模块 (JAR，不部署)
│   │   └── src/main/java/com/oj/common/
│   │       ├── constant/                              # MqConstant, RedisKey, 状态码常量
│   │       ├── enumeration/                           # 枚举类 (JudgeStatus, ContestStatus 等)
│   │       ├── exception/                             # 全局异常体系 (BaseException, GlobalExceptionHandler)
│   │       ├── result/                                # Result<T> / PageResult 统一返回体
│   │       ├── dto/ / entity/ / vo/                   # 共享数据对象 (PageQueryDTO 等)
│   │       ├── utils/                                 # JwtUtil, AliOssUtil, BaseContext (ThreadLocal)
│   │       └── properties/                            # 公共配置属性类 (JwtProperties, AliOssProperties)
│   │
│   ├── oj-common-api/                                 # Feign 接口定义模块 (JAR)
│   │   └── src/main/java/com/oj/api/
│   │       ├── UserClient.java                        # 用户服务 Feign (4个接口)
│   │       ├── ProblemClient.java                     # 题目服务 Feign (8个接口)
│   │       ├── ContestClient.java                     # 竞赛服务 Feign (4个接口)
│   │       ├── JudgeClient.java                       # 判题服务 Feign (4个接口)
│   │       └── fallback/                              # Sentinel 降级工厂 (4个 FallbackFactory)
│   │
│   ├── oj-gateway/                                    # API网关 :8080
│   │   └── src/main/java/com/oj/gateway/
│   │       ├── GatewayApplication.java
│   │       ├── config/                                # 路由表 / CORS / Sentinel
│   │       └── filter/                                # AuthGlobalFilter (JWT鉴权) / TraceIdFilter
│   │
│   ├── oj-user-service/                               # 用户服务 :8081
│   │   └── src/main/java/com/oj/user/
│   │       ├── controller/
│   │       │   ├── admin/                             # UserController (用户管理)
│   │       │   ├── user/                              # LoginController / UserProfileController
│   │       │   └── internal/                          # UserInternalController (Feign服务端)
│   │       ├── service/                               # UserService + CaptchaService + impl
│   │       ├── mapper/                                # UserMapper
│   │       └── config/                                # WebMvcConfig + UserInfoInterceptor
│   │   └── src/main/resources/ddl/user_ddl.sql
│   │
│   ├── oj-problem-service/                            # 题目+题单服务 :8082
│   │   └── src/main/java/com/oj/problem/
│   │       ├── controller/
│   │       │   ├── admin/                             # Problem / ProblemTypes / TestCase / Knowledge / Group
│   │       │   ├── user/                              # ProblemUser / GroupUser
│   │       │   └── internal/                          # ProblemInternalController (Feign服务端)
│   │       ├── service/                               # Problem / ProblemType / TestCase / Group / Knowledge + impl
│   │       ├── mapper/                                # 9个 Mapper (Problem/Type/TestCase/Group)
│   │       └── config/                                # WebMvcConfig + UserInfoInterceptor
│   │   └── src/main/resources/ddl/problem_ddl.sql
│   │
│   ├── oj-contest-service/                            # 竞赛+报告+题解服务 :8083
│   │   └── src/main/java/com/oj/contest/
│   │       ├── controller/
│   │       │   ├── admin/                             # Contest / Report / WorkSpace
│   │       │   ├── user/                              # UserContest / SolutionComment
│   │       │   └── internal/                          # ContestInternalController (Feign服务端)
│   │       ├── service/                               # Contest / UserContest / WorkSpace / Report / Comment + impl
│   │       ├── mapper/                                # 4个 Mapper (Contest/Participant/Comment)
│   │       └── config/                                # WebMvcConfig + UserInfoInterceptor + RedisConfig
│   │   └── src/main/resources/ddl/contest_ddl.sql
│   │
│   ├── oj-judge-service/                              # 判题服务 :8084 ⭐
│   │   └── src/main/java/com/oj/judge/
│   │       ├── controller/
│   │       │   ├── admin/                             # SubmissionAdmin / MaliciousCode
│   │       │   ├── user/                              # Judge / Submission
│   │       │   └── internal/                          # JudgeInternalController (Feign服务端)
│   │       ├── service/                               # JudgeService / SubmissionService + impl
│   │       ├── mapper/                                # SubmissionMapper / MaliciousCodeLogMapper
│   │       ├── mq/                                    # 4个 RocketMQ Consumer + DeadLetter Consumer
│   │       ├── websocket/                             # WebSocketServer (@ServerEndpoint)
│   │       ├── config/                                # Judge0Client / RedisLua / ThreadPool / WebSocketConfig
│   │       └── metrics/                               # JudgeMetrics (Micrometer)
│   │   └── src/main/resources/ddl/judge_ddl.sql
│   │
│   ├── oj-ai-service/                                 # AI智能服务 :8086
│   │   └── src/main/java/com/oj/ai/
│   │       ├── controller/
│   │       │   └── user/                              # AiJudge / Agent
│   │       ├── service/
│   │       │   ├── agent/                             # AgentService / AgentAssistant / LangGraphOrchestrator
│   │       │   │   ├── specialized/                   # Router / Solution / CodeJudge / Learning / Knowledge / Supervisor Agent
│   │       │   │   └── graph/                         # OJAgentState (StateGraph 状态定义)
│   │       │   ├── tools/                             # @Tool 注解工具 (AiJudge / SolutionGenerator / LearningAnalyzer / KnowledgeRetrieval)
│   │       │   ├── memory/                            # RedisChatMemoryStore / LongTermMemory / DialogMemory
│   │       │   └── impl/                              # AiJudgeServiceImpl / RAGServiceImpl / KnowledgeRetrievalServiceImpl
│   │       └── config/                                # AgentStudioConfig / McpClientConfig / WebMvcConfig
│   │
│   └── docker/                                        # Docker 部署 (待创建)
│       ├── docker-compose.yml                         # 微服务组件（新虚拟机）
│       └── dockerfiles/                               # 各服务 Dockerfile
│
└── docs/                                              # 文档
    ├── 微服务架构改造设计文档.md                        # v1 原版
    └── 微服务架构改造设计文档_v2.md                     # v2 精简版 (当前架构依据)
```

## 常用命令

### 后端 — 全局编译

```bash
# 从 oj-microservice 目录
mvn clean compile                          # 全量编译
mvn clean package -DskipTests              # 全量打包
```

### 后端 — 各服务独立启动

```bash
# 从 oj-microservice 目录，依次启动
mvn spring-boot:run -pl oj-gateway         # 网关 :8080
mvn spring-boot:run -pl oj-user-service    # 用户服务 :8081
mvn spring-boot:run -pl oj-problem-service   # 题目+题单服务 :8082
mvn spring-boot:run -pl oj-contest-service   # 竞赛+报告服务 :8083
mvn spring-boot:run -pl oj-judge-service     # 判题服务 :8084
mvn spring-boot:run -pl oj-ai-service        # AI服务 :8086
```

### 中间件（新虚拟机 192.168.141.129）

```bash
# Nacos + Sentinel
docker compose -f docker/docker-compose.yml up -d
```

### 用户端前端

```bash
# 开发服务器 (http://localhost:5174)
npm run dev

# 生产构建
npm run build
```

### 管理端前端

```bash
cd vue-Element

# 开发服务器 (http://localhost:5173)
npm run dev

# 生产构建
npm run build
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.4.2, Spring Cloud, Java 17 |
| 服务注册/配置 | Nacos 2.4.3 |
| API网关 | Spring Cloud Gateway |
| 服务调用 | OpenFeign + LoadBalancer |
| 熔断降级 | Sentinel |
| ORM | MyBatis-Plus 3.5.7, Druid 连接池 |
| 数据库 | MySQL 8.0 (`exercise` 库，暂共享) |
| 缓存 | Redis 7.0 + Jedis |
| 消息队列 | RocketMQ（异步判题） |
| 分布式事务 | RocketMQ 事务消息（无 Seata） |
| AI | LangChain4j 1.12.2, LangGraph4j 1.8.11 |
| 认证 | JWT (jjwt 0.12.6) — Gateway 层统一鉴权 |
| 文档 | Knife4j 4.5.0 |
| 前端 | Vue 3 + Vite 3 + Element Plus + Pinia |
| 代码编辑 | Monaco Editor |
| 判题引擎 | Judge0 (Docker) |
| 监控 | Micrometer + Prometheus + Skywalking |

## 服务架构

| 服务 | 端口 | 职责 | 数据库表 | 负载等级 |
|------|------|------|----------|----------|
| oj-gateway | 8080 | 路由转发 / JWT鉴权 / 限流 / CORS | 无 | 中 |
| oj-user-service | 8081 | 登录注册 / JWT签发 / 用户信息 | users, user_attendance, captcha | 中 |
| oj-problem-service | 8082 | 题目 / 题单 / 测试用例 / 知识库 | problems*, problem_types*, test_cases, problem_groups*, group_* | 中 |
| oj-contest-service | 8083 | 竞赛 / 参赛 / 排名 / 报告 | contests, contest_problems, contest_participants, solution_comments, workspace | 中 |
| oj-judge-service | 8084 | 代码提交 / Judge0判题 / WebSocket | submissions, malicious_code_log | **高** |
| oj-ai-service | 8086 | AI Agent / RAG / AI判题辅助 | 无独立表 | 中 |

## 架构约定

### 服务间通信

- **同步查询**：OpenFeign + LoadBalancer，接口定义在 `oj-common-api` 模块
- **异步判题**：RocketMQ Topic 模式（沿用现有 4 个 Topic）
- **实时推送**：WebSocket 保留在 `oj-judge-service`，Gateway Nginx 直接代理
- **服务发现**：所有服务启动时向 Nacos 注册，Feign 自动通过服务名调用

### Gateway 路由规则

| 路径前缀 | 目标服务 |
|----------|----------|
| `/api/user/**`, `/api/login/**` | oj-user-service |
| `/api/problem/**`, `/api/knowledge/**` | oj-problem-service |
| `/api/contest/**`, `/api/report/**`, `/api/workspace/**`, `/api/problemset/**`, `/api/group/**`, `/api/comment/**` | oj-contest-service |
| `/api/judge/**`, `/api/submission/**` | oj-judge-service |
| `/api/ai/**`, `/api/agent/**` | oj-ai-service |
| `/ws/judge/**` | oj-judge-service |

### 后端约定

- Controller 三层结构：
  - `admin/` → 管理端接口（需要 admin 角色）
  - `user/` → 用户端接口（需要 user 角色）
  - `internal/` → **InternalController**（微服务间 Feign 调用的服务端实现，路径 `/internal/**`，不经过 Gateway 鉴权）
- Service 命名：接口放 `service/`，实现放 `service/impl/`（命名 `XxxServiceImpl.java`）
- Mapper 全部继承 `BaseMapper<T>`，自定义SQL在对应的 `mapper/*.xml` 文件中
- 统一返回体：`Result<T>` 和 `PageResult`
- JWT 鉴权统一在 Gateway 层，使用全局过滤器（替代原 Interceptor 模式）
- 用户信息传播：Gateway 解析 Token → 设置 `X-User-Id`/`X-User-Role` 请求头 → 各服务 `UserInfoInterceptor` 读取 → 存入 `BaseContext` (ThreadLocal)
- Feign 调用传播：`FeignRequestInterceptor` 自动将 `X-User-Id`/`X-User-Role` 从当前请求上下文传播到下游 Feign 调用
- RocketMQ Topic 和 Consumer Group 常量定义在 `oj-common` 的 `MqConstant.java`
- 判题流程：Gateway → JudgeController → Redis Lua限流 → RocketMQ → JudgeTaskConsumer → Judge0 → DatabaseUpdateConsumer → WebSocket
- 新增 Feign 接口时：先在 `oj-common-api` 定义接口 + fallback 降级处理，再在服务端 `internal/` Controller 中实现

### 前端约定

- 用户端和管理端 API 全部指向 Gateway 地址
- 用户端请求 baseURL: `/api`
- 管理端请求 baseURL: `/admin/api`
- API 封装文件与后端 Controller 一一对应
- Axios 拦截器在 `utils/request.js` 中统一处理 Token 注入和异常提示

### 跨模块依赖

- **oj-common** → 无依赖，被所有模块依赖
- **oj-common-api** → 依赖 oj-common，被所有服务依赖
- **oj-gateway** → 依赖 oj-common
- **oj-user-service** → 依赖 oj-common + oj-common-api
- **oj-problem-service** → 依赖 oj-common + oj-common-api
- **oj-contest-service** → 依赖 oj-common + oj-common-api（通过 ProblemClient Feign 调用 problem-service 获取题目信息）
- **oj-judge-service** → 依赖 oj-common + oj-common-api（通过 ProblemClient/UserClient/ContestClient Feign 获取测试用例/用户/竞赛数据）
- **oj-ai-service** → 依赖 oj-common + oj-common-api（通过 ProblemClient/JudgeClient/UserClient Feign 获取题目/提交/用户数据）

## 基础设施

| 组件 | 地址 | 用途 | 所在虚拟机 |
|------|------|------|-----------|
| MySQL | `localhost:3306` | 主数据库 (库: exercise, 用户: root) | 宿主机 |
| Redis | `192.168.141.128:6378` | 缓存/Lua限流 (密码: qwer1234) | VM1 |
| RocketMQ | `192.168.141.128:9876` | 判题任务异步分发 | VM1 |
| Judge0 | `192.168.141.128:2358` | 代码编译执行沙箱 | VM1 |
| Skywalking OAP | `192.168.141.128:11800` | 链路追踪 | VM1 |
| Nacos | `192.168.141.129:8848` | 服务注册 + 配置中心 | VM2 |
| Sentinel | `192.168.141.129:8858` | 流量控制 + 熔断降级 | VM2 |
| AI API | `api.siliconflow.cn` | Qwen3-Coder-30B 模型 | 外部 |
| OSS | Aliyun OSS | 头像/文件上传 | 外部 |

## 关键注意事项

- **不要引入 Lombok 以外的注解处理器**，项目统一使用 Lombok 简化代码
- **修改 Controller 路径时**，同步更新 Gateway 路由表 和前端 `api/` 目录下对应的请求路径
- **新增 RocketMQ Topic** 时，需在 `oj-common` 的 `MqConstant.java` 中定义常量并同步 Consumer 的 `@RocketMQMessageListener` 注解
- **新增 Feign 接口**时，先在 `oj-common-api` 定义接口 + fallback 降级处理，再在对应服务的 `controller/internal/` 中实现 InternalController
- **InternalController** 路径统一为 `/internal/**`，这些接口不经过 Gateway 鉴权，仅供微服务间 Feign 调用使用
- **MQ 消费者中的 Feign 调用**：RocketMQ 消费者无 HTTP 请求上下文，Feign 调用时不会携带用户信息头，InternalController 的 `/internal/**` 接口不依赖用户上下文
- **修改 Entity 字段** 时，检查对应的 DTO/VO 和 Feign 接口返回是否需要同步修改
- **AI Service 修改** 后，需要重启整个应用（LangChain4j Agent 在启动时编译）
- **WebSocket 路径** 为 `/ws/{sid}`，前端连接时需传递用户标识；Gateway/Nginx 需配置 WebSocket 代理
- **InMemoryEmbeddingStore** 是内存存储，oj-ai-service 初期单实例部署，后续迁移到 Redis Vector
- **redis-stack 容器** 当前状态为 Exited，如需要使用 RedisJSON/RediSearch 功能需手动启动
- **各服务配置**统一由 Nacos Config 管理，本地仅保留 `bootstrap.yml`（指定 nacos 地址）
- **数据库暂共享**，各服务仅访问自己的表（代码约束），后续独立拆分
