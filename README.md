<p align="center">
  <h1 align="center">OJ-Onlinejudge</h1>
  <p align="center">在线判题系统 — Spring Cloud 微服务架构</p>
</p>

---

## 版本分支

| 分支 | 架构 | 说明 |
|------|------|------|
| **[main](https://github.com/wsjwu58-cmd/OJ-Onlinejudge)** | 微服务版 | Spring Cloud 微服务改造（当前分支） |
| **[master](https://github.com/wsjwu58-cmd/OJ-Onlinejudge/tree/master)** | 单体版 | Spring Boot 单体架构（原始项目） |

---

## 项目简介

OJ-Onlinejudge 是一个在线判题系统，支持代码提交与自动判题、AI 辅助判题、竞赛管理、题单管理等功能。本项目将原单体架构（`oj-project/`）完整迁移为 Spring Cloud 微服务架构，功能无遗漏、配置与原项目保持一致。

---

## 项目架构

```
┌──────────────────────────────────────────────────────────┐
│                       Frontend                           │
│  vue-project1 (用户端)          vue-Element (管理端)       │
│  Vue 3 + Element Plus          Vue 3 + Element Plus      │
│  Monaco Editor + Pinia          ECharts + Pinia           │
└────────────┬─────────────────────────────┬───────────────┘
             │          /api               │
             ▼                             ▼
┌──────────────────────────────────────────────────────────┐
│                   oj-gateway :8080                       │
│          Spring Cloud Gateway (路由/JWT鉴权/CORS)         │
└───┬─────────┬──────────┬──────────┬──────────┬──────────┘
    │         │          │          │          │
    ▼         ▼          ▼          ▼          ▼
┌────────┐┌────────┐┌────────┐┌────────┐┌──────────────┐
│  user  ││problem ││contest ││ judge  ││      ai      │
│ :8081  ││ :8082  ││ :8083  ││ :8084  ││    :8086     │
│ 用户   ││题目/题单││竞赛/报告││判题/提交││  AI智能辅助   │
└────────┘└────────┘└────────┘└────────┘└──────────────┘
    │         │          │          │
    └─────────┴──────────┴──────────┘
              │
    ┌─────────┴──────────┐
    │  Nacos + Sentinel   │  ← 服务注册/配置/熔断
    │  192.168.141.129    │
    └────────────────────┘
              │
    ┌─────────┴──────────┐
    │ RocketMQ + Redis    │  ← 异步判题/缓存/Lua限流
    │ Judge0 (判题沙箱)    │
    │ Skywalking (链路追踪) │
    │ 192.168.141.128     │
    └────────────────────┘
```

---

## 模块说明

| 模块 | 端口 | 数据库 | 职责 |
|------|:---:|--------|------|
| **oj-gateway** | 8080 | — | 路由转发、统一 JWT 鉴权、CORS、限流 |
| **oj-user-service** | 8081 | oj_user_db | 登录注册、JWT 签发、用户管理、验证码 |
| **oj-problem-service** | 8082 | oj_problem_db | 题目 CRUD、题单管理、测试用例、知识库 |
| **oj-contest-service** | 8083 | oj_contest_db | 竞赛管理、参赛排名、数据报告、工作台、题解 |
| **oj-judge-service** | 8084 | oj_judge_db | 代码提交、Judge0 判题、RocketMQ 异步处理、WebSocket 推送 |
| **oj-ai-service** | 8086 | — | AI 智能辅助、多 Agent 编排、RAG 检索 |
| **oj-common** | — | — | 公共模块（Result、异常、JWT、常量、枚举） |
| **oj-common-api** | — | — | Feign 接口定义 + Sentinel 降级 |

---

## 技术栈

### 后端
| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.4.2 |
| Spring Cloud | 2024.0.1 |
| Spring Cloud Alibaba | 2023.0.3.2 |
| Nacos | 服务注册 & 配置中心 |
| Sentinel | 流量控制 & 熔断降级 |
| OpenFeign | 服务间同步调用 |
| RocketMQ | 2.3.1 (异步判题) |
| MyBatis-Plus | 3.5.7 |
| Druid | 1.2.23 |
| MySQL | 8.0 |
| Redis | 7.0 (Jedis) |
| JJWT | 0.12.6 |
| Knife4j | 4.5.0 |
| LangChain4j | 1.12.2 |
| LangGraph4j | 1.8.11 |
| Judge0 | 代码沙箱 |
| Skywalking | 链路追踪 |

### 前端
| 技术 | 用户端 (vue-project1) | 管理端 (vue-Element) |
|------|:---:|:---:|
| Vue | 3.2 | 3.2 |
| Vite | 3 | 3 |
| Element Plus | 2.13 | 2.4 |
| Pinia | 3 | — |
| Axios | 1.13 | 1.13 |
| Vue Router | 4 | 4 |
| Monaco Editor | 0.55 | — |
| ECharts | — | 6.0 |

---

## 核心功能

### 在线判题
支持 Java / C++ / Python 等多语言代码提交，通过 Judge0 沙箱自动编译运行，对比测试用例输出进行判题。采用 RocketMQ 异步处理 + Redis Lua 限流，保障高并发下的稳定性。

### 竞赛系统
支持周赛、双周赛、模拟面试等多种竞赛模式。竞赛期间实时排名（Redis ZSet）、自动计分、比赛结束后排名持久化。

### Hack 机制
竞赛支持选手间相互 Hack：选手 AC 题目后可以锁定该题，查看其他 AC 选手的代码，构造针对性测试数据发起挑战。

- **C++ Validator 校验**：出题人编写 C++ 校验器（Validator），通过 exit 0/非0 验证 Hack 测试数据是否合法
- **三方比对判题**：Validator 校验通过后，目标代码和标准解答并行运行同一组输入，输出不一致即 Hack 成功
- **排名联动**：Hack 成功者加分、被攻击者扣分并清除 AC 标记（Redis Lua 原子操作保底）
- **测试用例沉淀**：Hack 成功的数据自动入库成为正式测试用例（`source_hack_id` 溯源），后续所有提交均须通过
- **安全隔离**：Validator 编译运行均在 Judge0 沙箱完成，源码 SHA-256 校验防篡改缓存，MQ 消息不传代码体仅传 ID 引用

流程：`AC → 锁定题目 → 查看对手代码 → 构造 Hack 数据 → Validator 校验 → 三方比对 → 结果通知`

### AI 智能辅助
集成 LangChain4j / LangGraph4j，多 Agent 编排（Router / Solution / CodeJudge / Knowledge），支持智能答疑和代码审查。

---

## 架构约定

- **Controller 三层结构**：`admin/`（管理端）、`user/`（用户端）、`internal/`（Feign 内部调用，不经 Gateway 鉴权）
- **鉴权流程**：Gateway 解析 JWT → 设置 `X-User-Id` / `X-User-Role` 请求头 → 各服务拦截器读取 → 存入 `BaseContext` (ThreadLocal)
- **服务间通信**：同步用 Feign（oj-common-api），异步用 RocketMQ
- **统一返回体**：`Result<T>` / `PageResult`
- **配置管理**：Nacos 统一管理，本地仅保留 `bootstrap.yml`
- **判题流程**：Gateway → JudgeController → Redis Lua 限流 → RocketMQ → JudgeTaskConsumer → Judge0 → DatabaseUpdateConsumer → WebSocket

---

## 基础设施

| 组件 | 地址 | 用途 |
|------|------|------|
| MySQL | localhost:3306 | 主数据库 |
| Redis | 192.168.141.128:6378 | 缓存 / Lua 限流 |
| RocketMQ | 192.168.141.128:9876 | 异步判题分发 |
| Judge0 | 192.168.141.128:2358 | 代码编译执行沙箱 |
| Skywalking | 192.168.141.128:11800 | 分布式链路追踪 |
| Nacos | 192.168.141.129:8848 | 服务注册 + 配置中心 |
| Sentinel | 192.168.141.129:8858 | 流量控制 + 熔断降级 |

---

## 快速启动

### 后端

```bash
# 确保 Nacos / Sentinel / RocketMQ / Redis / Judge0 已启动

# 全量编译
mvn clean compile

# 按顺序启动服务
mvn spring-boot:run -pl oj-gateway
mvn spring-boot:run -pl oj-user-service
mvn spring-boot:run -pl oj-problem-service
mvn spring-boot:run -pl oj-contest-service
mvn spring-boot:run -pl oj-judge-service
mvn spring-boot:run -pl oj-ai-service
```

### 前端

```bash
# 用户端 (http://localhost:5174)
cd vue-project1 && npm install && npm run dev

# 管理端 (http://localhost:5173)
cd vue-Element && npm install && npm run dev
```

---

## 开发规范

1. **功能完全迁移**：原单体项目所有功能完整迁移，不遗漏、不新增
2. **配置一致**：端口、数据库、中间件地址与原项目保持一致
3. **问题溯源**：遇到错误优先参考原单体项目 `oj-project/` 中对应实现
4. **禁止引入 Lombok 以外的注解处理器**
