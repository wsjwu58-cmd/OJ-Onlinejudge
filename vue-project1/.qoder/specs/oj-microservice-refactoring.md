# OJ-Project 微服务改造方案

## Context

当前 OJ-Project 是一个 Spring Boot 3.4.2 单体应用（位于 `E:\vue-project1\vue-Element\oj-project\`），包含 oj-config、oj-pojo、oj-web 三个模块。所有业务逻辑、数据访问、MQ消费者、WebSocket、AI Agent 均在一个进程中运行。

**改造目标**：将其拆分为 8 个微服务模块，使用 Spring Cloud + Nacos 进行服务治理，Gateway 统一鉴权，OpenFeign 实现跨服务调用，每个微服务拥有独立数据库。**核心要求是完整实现原项目所有功能**。

**新项目目录**：`E:\vue-project1\oj-microservice\`

---

## 1. 模块结构总览

```
oj-microservice/
├── pom.xml                          # 父POM (Spring Boot 3.4.2)
├── oj-common/                       # 公共模块 (JAR，不部署)
├── oj-common-api/                   # Feign接口+降级 (JAR)
├── oj-gateway/                      # API网关 :8080
├── oj-user-service/                 # 用户服务 :8081
├── oj-problem-service/              # 题目+题单服务 :8082
├── oj-contest-service/              # 竞赛+报告+题解服务 :8083
├── oj-judge-service/                # 判题服务 :8084
├── oj-ai-service/                   # AI智能服务 :8086
└── AGENTS.md                        # 项目说明文档
```

### 父POM版本矩阵

| 依赖 | 版本 |
|------|------|
| Spring Boot | 3.4.2 |
| Spring Cloud | 2024.0.1 |
| Spring Cloud Alibaba | 2023.0.3.2 |
| Nacos | 2.4.3 (服务端) |
| MyBatis-Plus | 3.5.7 |
| Druid | 1.2.23 |
| RocketMQ Spring | 2.3.1 |
| jjwt | 0.12.6 |
| Knife4j | 4.5.0 |
| LangChain4j | 1.12.2 |
| LangGraph4j | 1.8.11 |
| Lombok | 1.18.42 |

---

## 2. oj-common 公共模块

**包名**：`com.oj.common` — 不含任何 Entity/DTO/VO，仅基础设施代码。

```
com.oj.common/
├── constant/
│   ├── JwtClaimsConstant.java        # JWT Claims键
│   ├── MessageConstant.java          # 业务消息（清理无关的food-delivery消息）
│   ├── MqConstant.java               # RocketMQ Topic常量
│   ├── PasswordConstant.java         # 默认密码
│   ├── RedisConstant.java            # Redis Key前缀
│   └── StatusConstant.java           # 状态码
├── context/
│   └── BaseContext.java              # ThreadLocal当前用户ID
├── enumeration/
│   ├── ActivityType.java
│   └── OperationType.java
├── exception/
│   ├── BaseException.java
│   ├── AccountLockedException.java
│   ├── AccountNotFoundException.java
│   ├── LoginFailedException.java
│   ├── ParameterMissingException.java
│   ├── PasswordErrorException.java
│   └── UserNotLoginException.java
├── result/
│   ├── Result.java
│   └── PageResult.java
├── utils/
│   ├── JwtUtil.java                  # JWT创建/解析
│   ├── AliOssUtil.java
│   ├── HttpClientUtil.java
│   └── MarkdownUtil.java
├── properties/
│   ├── JwtProperties.java            # admin/user密钥+TTL+tokenName
│   ├── AliOssProperties.java
│   └── AiProperties.java
├── json/
│   └── JacksonObjectMapper.java
├── annotation/
│   ├── AutoFill.java
│   └── ClearCache.java
├── aspect/
│   └── AutoFillAspect.java
└── handler/
    └── GlobalExceptionHandler.java
```

**新增常量**：
- `MessageConstant.FEIGN_CALL_FAILED` = "服务调用失败"
- `MessageConstant.USER_INFO_HEADER_MISSING` = "用户信息头缺失"
- `RedisConstant.LOGIN_USER_KEY` = "login:user:" (用户Redis token前缀)

---

## 3. oj-common-api Feign接口模块

**包名**：`com.oj.api` — 依赖 oj-common。

```
com.oj.api/
├── UserClient.java                   # 用户服务Feign
├── ProblemClient.java                # 题目服务Feign
├── ContestClient.java                # 竞赛服务Feign
├── JudgeClient.java                  # 判题服务Feign
├── dto/                              # Feign专用的轻量DTO（非业务DTO）
│   ├── UserFeignDTO.java             # id, username, nickname, role, status
│   ├── ProblemFeignDTO.java          # id, title, difficulty, timeLimitMs, memoryLimitMb, content, templateCode
│   ├── TestCaseFeignDTO.java         # id, problemId, inputData, outputData, isSample
│   ├── ContestProblemFeignDTO.java   # contestId, problemId, score
│   ├── WorkspaceActivityFeignDTO.java # userId, activityType, title, description, targetId
│   └── SubmissionCountDTO.java       # count, status
├── fallback/
│   ├── UserClientFallbackFactory.java
│   ├── ProblemClientFallbackFactory.java
│   ├── ContestClientFallbackFactory.java
│   └── JudgeClientFallbackFactory.java
└── config/
    └── FeignConfig.java              # Feign请求拦截器（传递用户信息头）
```

### Feign接口方法

**UserClient** (`@FeignClient("oj-user-service")`):
- `GET /internal/user/{id}` → `Result<UserFeignDTO> getUserById(Long id)`
- `POST /internal/user/batch` → `Result<List<UserFeignDTO>> getUsersByIds(List<Long> ids)`
- `PUT /internal/user/{id}/solved-count` → `Result<Void> updateUserSolvedCount(Long id)`
- `GET /internal/user/username` → `Result<UserFeignDTO> getUserByUsername(String username)`

**ProblemClient** (`@FeignClient("oj-problem-service")`):
- `GET /internal/problem/{id}` → `Result<ProblemFeignDTO> getProblemById(Integer id)`
- `GET /internal/problem/{problemId}/test-cases` → `Result<List<TestCaseFeignDTO>> getTestCasesByProblemId(Integer problemId)`
- `PUT /internal/problem/{id}/acceptance` → `Result<Void> updateProblemAcceptance(Integer id)`
- `GET /internal/problem/count` → `Result<Long> countProblems()`

**ContestClient** (`@FeignClient("oj-contest-service")`):
- `GET /internal/contest/problem` → `Result<ContestProblemFeignDTO> getContestProblem(Integer contestId, Integer problemId)`
- `POST /internal/contest/rank` → `Result<Void> updateRankOnAccepted(Integer contestId, Long userId, Integer problemId, Integer score)`
- `POST /internal/contest/workspace/activity` → `Result<Void> recordWorkspaceActivity(WorkspaceActivityFeignDTO dto)`

**JudgeClient** (`@FeignClient("oj-judge-service")`):
- `GET /internal/judge/submission/count` → `Result<Long> countSubmissions(Map<String,Object> params)`
- `GET /internal/judge/user/{userId}/submission-count` → `Result<Long> getUserSubmissionCount(Long userId)`

### Feign头传播（FeignConfig）

```java
@Bean
public RequestInterceptor feignRequestInterceptor() {
    return template -> {
        // 1. 优先从HTTP请求上下文获取
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String userId = request.getHeader("X-User-Id");
            String userRole = request.getHeader("X-User-Role");
            if (userId != null) template.header("X-User-Id", userId);
            if (userRole != null) template.header("X-User-Role", userRole);
        }
        // 2. MQ消费者场景：从BaseContext获取
        if (template.headers().get("X-User-Id") == null || template.headers().get("X-User-Id").isEmpty()) {
            Long currentId = BaseContext.getCurrentId();
            if (currentId != null) {
                template.header("X-User-Id", String.valueOf(currentId));
            }
        }
    };
}
```

---

## 4. oj-gateway 网关模块

**包名**：`com.oj.gateway`，端口 8080，**Reactive**（不引入 spring-boot-starter-web）。

### 目录结构

```
com.oj.gateway/
├── GatewayApplication.java
├── config/
│   └── CorsConfig.java
└── filter/
    └── AuthGlobalFilter.java        # JWT + Redis 全局鉴权过滤器
```

### 路由规则

前端请求 `/api/{role}/{domain}/...` → Gateway 去掉 `/api` → 按 `{role}/{domain}/...` 路由：

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ===== 用户服务 =====
        - id: user-admin
          uri: lb://oj-user-service
          predicates:
            - Path=/admin/user/**,/admin/common/**
        - id: user-user
          uri: lb://oj-user-service
          predicates:
            - Path=/user/userLogin/**,/user/userInfo/**

        # ===== 题目服务 =====
        - id: problem-admin
          uri: lb://oj-problem-service
          predicates:
            - Path=/admin/problem/**,/admin/problemTypes/**,/admin/test/**,/admin/knowledge/**
        - id: problem-user
          uri: lb://oj-problem-service
          predicates:
            - Path=/user/problem/**

        # ===== 竞赛服务 =====
        - id: contest-admin
          uri: lb://oj-contest-service
          predicates:
            - Path=/admin/Contest/**,/admin/group/**,/admin/workSpace/**,/admin/report/**
        - id: contest-user
          uri: lb://oj-contest-service
          predicates:
            - Path=/user/contest/**,/user/group/**,/user/comment/**

        # ===== 判题服务 =====
        - id: judge-admin
          uri: lb://oj-judge-service
          predicates:
            - Path=/admin/submission/**,/admin/malicious/**
        - id: judge-user
          uri: lb://oj-judge-service
          predicates:
            - Path=/user/judge/**,/user/submission/**

        # ===== AI服务 =====
        - id: ai-user
          uri: lb://oj-ai-service
          predicates:
            - Path=/user/ai/**,/user/agent/**,/user/ai-test/**

        # ===== WebSocket =====
        - id: judge-websocket
          uri: lb:ws://oj-judge-service
          predicates:
            - Path=/ws/**
```

**关键**：所有路由 StripPrefix=0（不去掉任何前缀），因为微服务的Controller路径保持 `/admin/xxx` 和 `/user/xxx` 不变。

### 鉴权过滤器 AuthGlobalFilter

```
1. 读取请求路径
2. 白名单放行（不鉴权）：
   - /admin/user/login
   - /user/userLogin/login
   - /user/userLogin/get-captcha
   - /ws/**
   - /doc.html, /webjars/**, /v3/api-docs/**
   - /internal/**  （Feign内部调用）
3. /admin/** 路径 → JWT鉴权：
   - 读取 header "token"
   - JwtUtil.parseJWT(adminSecretKey, token) → 取 EMP_ID
   - 设置请求头 X-User-Id, X-User-Role=admin
4. /user/** 路径 → Redis鉴权：
   - 读取 header "authorization"
   - Redis HASH查询 LOGIN_USER_KEY + token → 取 userId
   - 刷新token TTL (30min)
   - 设置请求头 X-User-Id, X-User-Role=user
5. 鉴权失败 → 返回 401 {"code":0,"msg":"用户未登录"}
```

### 去掉 /api 前缀

在 Gateway 的 `application.yml` 中配置全局 filter：
```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - RewritePath=/api/(?<segment>.*), /$\{segment}
```

这会自动将所有 `/api/xxx` 请求重写为 `/xxx`，再交给路由规则匹配。

---

## 5. 各微服务详细结构

### 5.1 oj-user-service (端口 8081)

**数据库**：`oj_user`（独立库）
**数据表**：`users`, `user_attendances`, `captcha`

```
com.oj.user/
├── UserApplication.java
├── controller/
│   ├── admin/UserController.java         # /admin/user/**
│   └── user/
│       ├── LoginController.java          # /user/userLogin/**
│       └── UserProfileController.java    # /user/userInfo/**
├── service/
│   ├── UserService.java
│   ├── CaptchaService.java
│   └── impl/
│       ├── UserServiceImpl.java
│       └── CaptchaServiceImpl.java
├── mapper/UserMapper.java
├── entity/
│   ├── User.java
│   ├── UserAttendance.java
│   └── Captcha.java
├── dto/
│   ├── UserDTO.java
│   ├── UserLoginDTO.java
│   └── UserQueryDTO.java
├── vo/UserLoginVo.java
├── interceptor/UserInfoInterceptor.java  # 读X-User-Id/X-User-Role → BaseContext
├── controller/internal/
│   └── UserInternalController.java       # Feign端点 /internal/user/**
└── config/ (Redis, OSS, WebMvc, MybatisPlus)
```

**重构要点**：
- `UserServiceImpl` 中 `WorkSpaceService` 依赖 → `ContestClient.recordWorkspaceActivity()` Feign调用
- `UserServiceImpl` 中 `SubMissionMapper` 依赖 → `JudgeClient.getUserSubmissionCount()` Feign调用
- `UserServiceImpl.userlogin()` 中 Redis token 存储逻辑不变

### 5.2 oj-problem-service (端口 8082)

**数据库**：`oj_problem`（独立库）
**数据表**：`problems`, `problem_types`, `problem_types_rel`, `test_cases`, `problem_groups`, `group_types_rel`, `group_problems`

```
com.oj.problem/
├── ProblemApplication.java
├── controller/
│   ├── admin/
│   │   ├── ProblemController.java        # /admin/problem/**
│   │   ├── ProblemTypesController.java   # /admin/problemTypes/**  (注意:原路径/admin/type需保持一致)
│   │   ├── TestCaseController.java       # /admin/test/**
│   │   ├── KnowledgeController.java      # /admin/knowledge/**
│   │   └── CommonController.java         # /admin/common/**
│   ├── user/
│   │   └── ProblemUserController.java    # /user/problem/**
│   └── internal/
│       └── ProblemInternalController.java # Feign端点 /internal/problem/**
├── service/ (ProblemService, ProblemTypeService, TestCaseService, GroupService, KnowledgeImportService, KnowledgeRetrievalService)
├── mapper/ (ProblemMapper, ProblemTypeMapper, ProblemTypeRelMapper, TestCaseMapper, ProblemGroupMapper, GroupProblemMapper, GroupTypeRelMapper)
├── entity/ (Problem, ProblemType, ProblemTypesRel, TestCase, ProblemGroup, GroupProblems, GroupTypesRel)
├── dto/ (ProblemDTO, ProblemQueryDTO, ProblemTypeDTO, ProblemTypeQueryDTO, GroupDTO, GroupQueryDTO)
├── vo/ (ProblemVO, ProblemAcceptanceVO, GroupVO, ProblemDataVO)
├── interceptor/UserInfoInterceptor.java
└── config/ (Redis, OSS, EmbeddingModel, InMemoryEmbeddingStore, WebMvc, MybatisPlus)
```

**重构要点**：
- `KnowledgeController` 原本在 oj-web 中管理知识库导入，迁移到 problem-service
- `CommonController` 文件上传功能迁移到 problem-service（或 user-service，取决于头像上传归属）
- 新增 `ProblemInternalController` 暴露 Feign 端点

### 5.3 oj-contest-service (端口 8083)

**数据库**：`oj_contest`（独立库）
**数据表**：`contests`, `contest_problems`, `contest_participants`, `solution_comment`, `workspace`

```
com.oj.contest/
├── ContestApplication.java
├── controller/
│   ├── admin/
│   │   ├── ContestController.java        # /admin/Contest/**
│   │   ├── GroupController.java          # /admin/group/**
│   │   ├── WorkSpaceController.java      # /admin/workSpace/**
│   │   └── ReportController.java         # /admin/report/**
│   ├── user/
│   │   ├── UserContestController.java    # /user/contest/**
│   │   ├── SolutionCommentController.java # /user/comment/**
│   │   └── GroupUserController.java      # /user/group/**
│   └── internal/
│       └── ContestInternalController.java # Feign端点 /internal/contest/**
├── service/ (ContestService, UserContestService, GroupService, SolutionCommentService, WorkSpaceService, ReportService)
├── mapper/ (ContestMapper, ContestProblemMapper, ContestParticipantMapper, CommentMapper, WorkSpaceMapper)
├── entity/ (Contest, ContestProblem, ContestParticipant, SolutionComment, Workspace)
├── dto/ (ContestDTO, ContestQueryDTO, GroupDTO, GroupQueryDTO, CommentDTO)
├── vo/ (ContestVO, ContestRankVO, GroupVO, SolutionVO, ScrollResult, WorkSpaceVO, WorkDataVO, ProblemDataVO, ContestDataVO, UserTrendVO, ProblemTrendVO, RecordTrendVO, ProblemAcceptanceVO)
├── feign/ (UserClient, ProblemClient, JudgeClient — 来自oj-common-api)
├── interceptor/UserInfoInterceptor.java
└── config/ (Redis, WebMvc, MybatisPlus, ExcelExport)
```

**重构要点**（最复杂的重构区域）：
- `UserContestServiceImpl` 直接使用 `UserMapper` → `UserClient.getUsersByIds()` Feign调用
- `ContestServiceImpl` 直接使用 `ProblemMapper` → `ProblemClient.getProblemById()` Feign调用
- `ReportServiceImpl` 直接使用 `UserMapper/ProblemMapper/SubMissionMapper` → 全部改为Feign调用
- `WorkSpaceServiceImpl` 直接使用 `SubMissionMapper/UserMapper/ProblemMapper` → 全部改为Feign调用
- `SolutionCommentServiceImpl` 的 `CommentMapper.xml` LEFT JOIN user → 拆为：先查评论，再 `UserClient.getUsersByIds()` 获取用户名，Java层组装
- `ContestInternalController` 暴露 Feign 端点给 judge-service 调用

### 5.4 oj-judge-service (端口 8084)

**数据库**：`oj_judge`（独立库）
**数据表**：`submissions`, `malicious_code_log`

```
com.oj.judge/
├── JudgeApplication.java
├── controller/
│   ├── admin/
│   │   ├── SubmissionAdminController.java # /admin/submission/**
│   │   └── MaliciousCodeController.java  # /admin/malicious/**
│   ├── user/
│   │   ├── JudgeController.java          # /user/judge/**
│   │   └── SubmissionController.java     # /user/submission/**
│   └── internal/
│       └── JudgeInternalController.java  # Feign端点 /internal/judge/**
├── service/ (JudgeService, SubmissionService, EnhancedMaliciousCodeDetector, MaliciousCodeDetector)
├── mapper/ (SubMissionMapper, MaliciousCodeLogMapper)
├── mq/
│   ├── JudgeTaskConsumer.java
│   ├── JudgeTaskDeadLetterConsumer.java
│   ├── DatabaseUpdateConsumer.java
│   └── DatabaseUpdateDeadLetterConsumer.java
├── websocket/
│   ├── WebSocketServer.java
│   ├── WebSocketConfiguration.java
│   ├── WebSocketTask.java
│   └── dto/JudgeNotifyMessage.java
├── entity/ (Submission, MaliciousCodeLog)
├── dto/ (JudgeSubmitDTO, JudgeRunDTO, JudgeTaskMessage, DatabaseUpdateMessage, MaliciousCodeDetectionResult, SubmissionQueryDTO, PageQueryDTO)
├── vo/ (JudgeResultVO, SubmissionVO)
├── feign/ (ProblemClient, UserClient, ContestClient — 来自oj-common-api)
├── config/ (Judge0Client, Judge0Config, JudgeMetrics, RedisConfiguration, RedisLuaConfig, ThreadPoolConfig, MaliciousCode*, MybatisPlus)
├── interceptor/UserInfoInterceptor.java
└── utils/JudgePerformanceMonitor.java
```

**重构要点**（判题流程核心）：
- `JudgeServiceImpl.submit()`: `problemMapper.selectById()` → `ProblemClient.getProblemById()`，`testCaseMapper.selectByProblemId()` → `ProblemClient.getTestCasesByProblemId()`
- `JudgeServiceImpl.run()`: 同上
- `JudgeTaskConsumer.onMessage()`: `testCaseMapper.selectByProblemId()` → `ProblemClient.getTestCasesByProblemId()`
- **DatabaseUpdateConsumer（最关键的改造）**：
  - `subMissionMapper.insert()` → 本地（不变）
  - `problemMapper.updateById()` → `ProblemClient.updateProblemAcceptance()`
  - `userMapper.updateById()` → `UserClient.updateUserSolvedCount()`
  - `contestProblemMapper.selectOne()` → `ContestClient.getContestProblem()`
  - `userContestService.updateRankOnAccepted()` → `ContestClient.updateRankOnAccepted()`
  - `webSocketServer.sendToAllClient()` → 本地（不变）
  - **MQ消费者无HTTP上下文**：在处理消息前手动 `BaseContext.setCurrentId(msg.getUserId())`，Feign拦截器会从BaseContext读取

### 5.5 oj-ai-service (端口 8086)

**数据库**：无独立数据库（InMemoryEmbeddingStore，状态存Redis）

```
com.oj.ai/
├── AiApplication.java
├── controller/user/
│   ├── AiJudgeController.java           # /user/ai/**
│   ├── AiTestController.java            # /user/ai-test/**
│   └── AgentController.java             # /user/agent/**
├── service/
│   ├── AiJudgeService.java
│   ├── RAGService.java
│   ├── KnowledgeImportService.java
│   ├── KnowledgeRetrievalService.java
│   ├── DialogMemoryService.java
│   ├── agent/
│   │   ├── AgentService.java
│   │   ├── LangGraphAgentOrchestrator.java
│   │   ├── specialized/ (RouterAgent, SolutionAgent, CodeJudgeAgent, LearningAgent, KnowledgeAgent, SupervisorAgent)
│   │   └── memory/ (LongTermMemoryService, RedisChatMemoryStore)
│   └── impl/ (AiJudgeServiceImpl, RAGServiceImpl, KnowledgeImport*, KnowledgeRetrieval*, DialogMemory*)
├── tools/ (AiJudgeTool, SolutionGeneratorTool, KnowledgeRetrievalTool, LearningAnalyzerTool)
├── dto/ (AiJudgeDTO, AiChatDTO, AgentRequestDTO)
├── vo/ (JudgeResultVO)
├── feign/ (ProblemClient, JudgeClient, UserClient)
├── config/ (AgentStudioConfig, McpClientConfiguration, MarkdownConfig, EmbeddingModel, EmbeddingStore, LangChain4j)
├── interceptor/UserInfoInterceptor.java
└── resources/knowledge/ (PDF知识库文件)
```

**重构要点**：
- 所有 `ProblemMapper/TestCaseMapper` → `ProblemClient` Feign调用
- `AiJudgeController` 中的 `JudgeService.submit()` → `JudgeClient` Feign调用或重构为纯AI分析
- Agent Tool 内部：`@Tool` 方法的 DB 访问全部改为 Feign 调用
- SSE流式端点通过 Gateway 正常工作（Gateway 支持chunked transfer）
- 添加 Caffeine 缓存对 Feign 结果进行短期缓存（problemId → ProblemFeignDTO, 5min TTL）

---

## 6. 鉴权流程：端到端

```
前端请求 /api/user/problem/1
  → Vite代理 /api → http://localhost:8080/api/user/problem/1
  → Gateway RewritePath: /api/user/problem/1 → /user/problem/1
  → Gateway AuthGlobalFilter:
     ├─ 路径 /user/** → Redis鉴权
     ├─ 读取 authorization 头 → Redis查询 LOGIN_USER_KEY+token
     ├─ 验证通过 → 设置 X-User-Id: 123, X-User-Role: user
     └─ 转发到 oj-problem-service
  → Problem-service UserInfoInterceptor:
     ├─ 读取 X-User-Id, X-User-Role
     ├─ BaseContext.setCurrentId(123)
     └─ Controller正常使用 BaseContext.getCurrentId()
```

### UserInfoInterceptor（每个微服务通用）

```java
@Component
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        if (userId != null) {
            BaseContext.setCurrentId(Long.valueOf(userId));
        }
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}
```

注册到 WebMvcConfig：
```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(userInfoInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/internal/**"); // Feign内部调用不走拦截器
}
```

---

## 7. 数据库拆分 DDL

各微服务的 DDL 放在各自的 `src/main/resources/ddl/` 目录下：

| 微服务 | 数据库 | DDL文件 | 数据表 |
|--------|--------|---------|--------|
| oj-user-service | oj_user | `ddl/oj_user.sql` | users, user_attendances, captcha |
| oj-problem-service | oj_problem | `ddl/oj_problem.sql` | problems, problem_types, problem_types_rel, test_cases, problem_groups, group_types_rel, group_problems |
| oj-contest-service | oj_contest | `ddl/oj_contest.sql` | contests, contest_problems, contest_participants, solution_comment, workspace |
| oj-judge-service | oj_judge | `ddl/oj_judge.sql` | submissions, malicious_code_log |

DDL 从原项目 `init.sql` 和各升级SQL中提取，字段保持一致。外键约束移除（跨库无法使用外键），应用层保证一致性。

---

## 8. RocketMQ Topic归属

所有4个Topic和Consumer均在 **oj-judge-service** 中，不变：

| Topic | 生产者 | 消费者 |
|-------|--------|--------|
| judge-task-topic | JudgeServiceImpl | JudgeTaskConsumer |
| database-update-topic | JudgeTaskConsumer.sendDatabaseUpdate | DatabaseUpdateConsumer |
| judge-task-dead-letter-topic | DLQ自动 | JudgeTaskDeadLetterConsumer |
| database-update-dead-letter-topic | DLQ自动 | DatabaseUpdateDeadLetterConsumer |

**DatabaseUpdateConsumer 改造后**：通过Feign调用其他服务更新数据，而非直接Mapper调用。如果Feign调用失败，MQ重试机制保证最终一致性。

---

## 9. WebSocket代理

Gateway 配置 `lb:ws://oj-judge-service` 代理 WebSocket。前端连接 `ws://gateway:8080/ws/{sid}`。WebSocket 路径 `/ws/**` 在 Gateway 鉴权白名单中（sid本身携带用户标识）。

---

## 10. Nacos配置管理

### 各服务 bootstrap.yml（本地）

```yaml
server:
  port: 808x
spring:
  application:
    name: oj-xxx-service
  profiles:
    active: dev
  cloud:
    nacos:
      server-addr: 192.168.141.129:8848
      discovery:
        namespace: public
      config:
        namespace: public
        file-extension: yml
        shared-configs:
          - data-id: oj-common-dev.yml
            refresh: true
```

### Nacos共享配置 (oj-common-dev.yml)

```yaml
sky:
  jwt:
    admin-secret-key: itcast-sky-take-out-admin-secret-key-2024
    admin-ttl: 7200000
    admin-token-name: token
    user-secret-key: itheima-sky-take-out-user-secret-key-2024
    user-ttl: 7200000
    user-token-name: authentication
  redis:
    host: 192.168.141.128
    port: 6378
    password: qwer1234
    database: 0
  alioss:
    endpoint: oss-cn-beijing.aliyuncs.com
    access-key-id: xxx
    access-key-secret: xxx
    bucket-name: xxx

spring:
  data:
    redis:
      host: ${sky.redis.host}
      port: ${sky.redis.port}
      password: ${sky.redis.password}
      database: ${sky.redis.database}
      client-type: jedis
```

### 各服务独立配置 (oj-xxx-service-dev.yml)

包含各自的 MySQL 连接（不同数据库名）、RocketMQ（仅judge-service）、Judge0（仅judge-service）、AI配置（仅ai-service）等。

---

## 11. 前端改动

### 用户端 (`E:\vue-project1\`)

- `vite.config.js`: proxy `/api` → `http://localhost:8080`（Gateway端口，可能不变）
- API路径、Token header 名均不变
- WebSocket URL不变（Gateway代理）

### 管理端 (`E:\vue-project1\vue-Element\`)

- `vite.config.js`: proxy `/api` → `http://localhost:8080`
- API路径不变
- 注意：管理端 request.js 用 `token` 头，用户端用 `authorization` 头，Gateway区分处理

---

## 12. 实施步骤

### Step 1: 创建项目骨架
1. 创建 `E:\vue-project1\oj-microservice\` 目录和父POM
2. 创建 oj-common 模块，迁移 oj-config 所有代码
3. 创建 oj-common-api 模块，定义所有Feign接口+DTO+Fallback
4. `mvn clean compile` 验证编译

### Step 2: 创建Gateway
1. 创建 oj-gateway 模块，配置路由、鉴权过滤器、CORS
2. 配置 bootstrap.yml 连接 Nacos
3. `mvn clean compile` 验证

### Step 3: 创建各微服务骨架
1. 依次创建6个微服务模块
2. 每个模块创建 Application.java + bootstrap.yml + 基本包结构
3. `mvn clean compile` 验证全量编译

### Step 4: 迁移User Service
1. 迁移 entity/dto/vo/mapper/service/controller
2. 创建 UserInfoInterceptor 替换原JWT拦截器
3. 创建 UserInternalController 暴露Feign端点
4. 重构 UserServiceImpl 中的跨服务依赖为Feign调用
5. 创建 oj_user 数据库，执行DDL

### Step 5: 迁移Problem Service
1. 迁移所有题目相关代码
2. 创建 ProblemInternalController
3. 创建 oj_problem 数据库，执行DDL

### Step 6: 迁移Judge Service
1. 迁移判题/MQ/WebSocket代码
2. 重构 JudgeServiceImpl/JudgeTaskConsumer/DatabaseUpdateConsumer 中的跨服务Mapper为Feign调用
3. 创建 oj_judge 数据库，执行DDL

### Step 7: 迁移Contest Service
1. 迁移竞赛/报告/题解/工作区代码
2. 重构 ReportServiceImpl/WorkSpaceServiceImpl/UserContestServiceImpl/ContestServiceImpl 中的跨服务Mapper为Feign调用
3. CommentMapper.xml 的 LEFT JOIN user 改为 Java层Feign组装
4. 创建 oj_contest 数据库，执行DDL

### Step 8: 迁移AI Service
1. 迁移所有AI/Agent/RAG代码
2. 重构所有Tool中的直接DB访问为Feign调用
3. 处理 AiJudgeController 对 JudgeService 的依赖

### Step 9: 集成测试
1. 更新前端vite配置
2. 全链路测试

### Step 10: 编写AGENTS.md

---

## 13. 验证方案

1. **编译验证**：`mvn clean compile` 全量编译通过
2. **服务注册**：启动Nacos后，各服务注册成功
3. **登录验证**：
   - 管理端 `/admin/user/login` → JWT token → 访问管理端API
   - 用户端 `/user/userLogin/login` → Redis token → 访问用户端API
4. **判题验证**：提交代码 → MQ异步 → Judge0判题 → WebSocket推送结果
5. **AI验证**：AI判题/Agent对话 SSE流式响应正常
6. **竞赛验证**：创建竞赛 → 参赛 → 排名更新 → 报告生成
7. **跨服务验证**：Feign调用链路正常，降级逻辑生效

---

## 14. 关键文件映射（原→新）

| 原文件 | 新位置 |
|--------|--------|
| `oj-config/constant/*.java` | `oj-common/constant/*.java` |
| `oj-config/context/BaseContext.java` | `oj-common/context/BaseContext.java` |
| `oj-config/exception/*.java` | `oj-common/exception/*.java` |
| `oj-config/utils/JwtUtil.java` | `oj-common/utils/JwtUtil.java` |
| `oj-config/properties/JwtProperties.java` | `oj-common/properties/JwtProperties.java` |
| `oj-pojo/entity/User.java` | `oj-user-service/entity/User.java` |
| `oj-pojo/entity/Problem.java` | `oj-problem-service/entity/Problem.java` |
| `oj-pojo/entity/Contest.java` | `oj-contest-service/entity/Contest.java` |
| `oj-pojo/entity/Submission.java` | `oj-judge-service/entity/Submission.java` |
| `oj-pojo/dto/*` | 按业务归属分发到各服务 dto/ |
| `oj-pojo/vo/*` | 按业务归属分发到各服务 vo/ |
| `oj-web/controller/admin/UserController.java` | `oj-user-service/controller/admin/` |
| `oj-web/controller/admin/ProblemController.java` | `oj-problem-service/controller/admin/` |
| `oj-web/controller/admin/ContestController.java` | `oj-contest-service/controller/admin/` |
| `oj-web/controller/User/JudgeController.java` | `oj-judge-service/controller/user/` |
| `oj-web/controller/User/AiJudgeController.java` | `oj-ai-service/controller/user/` |
| `oj-web/interceptor/JwtToken*Interceptor.java` | **删除** → Gateway AuthGlobalFilter |
| `oj-web/config/WebMvcConfiguration.java` | 各服务独立的 WebMvcConfig |
| `oj-web/config/Judge0Client.java` | `oj-judge-service/config/` |
| `oj-web/mq/*.java` | `oj-judge-service/mq/` |
| `oj-web/websocket/*.java` | `oj-judge-service/websocket/` |
| `oj-web/service/agent/*.java` | `oj-ai-service/service/agent/` |
| `oj-web/service/tools/*.java` | `oj-ai-service/tools/` |
| `oj-web/resources/mapper/*.xml` | 按业务归属分发到各服务 |
| `oj-web/resources/init.sql` | 拆分为4个DDL文件到各服务 resources/ddl/ |
