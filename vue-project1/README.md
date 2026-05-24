# OJ在线判题系统 - 项目总结文档

## 目录

- [一、项目概述](#一项目概述)
- [二、技术架构](#二技术架构)
- [三、核心功能实现](#三核心功能实现)
- [四、核心流程图](#四核心流程图)
- [五、数据库设计](#五数据库设计)
- [六、API接口设计](#六api接口设计)
- [七、关键技术点](#七关键技术点)
- [八、项目亮点](#八项目亮点)
- [九、部署说明](#九部署说明)

---

## 一、项目概述

### 1.1 项目简介

本项目是一个功能完善的**在线判题系统（Online Judge）**，采用前后端分离架构，支持代码提交、自动判题、AI辅助判题、竞赛管理、题组管理等功能。系统集成了先进的AI技术，实现了AI判题、错误分析、智能提示等创新功能。

### 1.2 项目结构

```
d:\vue\vue-project1\
├── src/                              # 用户端源码
│   ├── api/                          # API接口封装
│   ├── assets/                       # 静态资源
│   ├── components/                   # 公共组件
│   ├── router/                       # 路由配置
│   ├── store/                        # 状态管理
│   ├── utils/                        # 工具函数
│   └── views/                        # 页面组件
│       ├── HomePage.vue              # 首页
│       ├── ProblemList.vue           # 题目列表
│       ├── ProblemDetailView.vue     # 题目详情
│       ├── ContestList.vue           # 比赛列表
│       ├── ContestDetail.vue         # 比赛详情
│       ├── GroupList.vue             # 题组列表
│       ├── GroupDetail.vue           # 题组详情
│       ├── SubmissionList.vue        # 提交记录
│       ├── ProfileView.vue           # 个人主页
│       └── Login.vue                 # 登录注册
│
├── vue-Element/                      # 管理端源码
│   ├── src/
│   │   ├── api/                      # API接口封装
│   │   ├── components/               # 公共组件
│   │   ├── router/                   # 路由配置
│   │   └── views/                    # 页面组件
│   │       ├── Dashboard.vue         # 工作台
│   │       ├── Problems.vue          # 题目管理
│   │       ├── CreateProblem.vue     # 创建题目
│   │       ├── Users.vue             # 用户管理
│   │       ├── Contests.vue          # 比赛管理
│   │       ├── Groups.vue            # 题组管理
│   │       ├── Submissions.vue       # 提交记录
│   │       ├── Categories.vue        # 分类管理
│   │       ├── Statistics.vue        # 数据统计
│   │       └── Knowledge.vue         # 知识库管理
│   │
│   └── sky-take-out-old/             # 后端服务
│       ├── sky-common/               # 公共模块
│       │   └── src/main/java/com/sky/
│       │       ├── constant/         # 常量定义
│       │       ├── context/          # 上下文
│       │       ├── enumeration/      # 枚举类
│       │       ├── exception/        # 异常类
│       │       ├── properties/       # 配置属性
│       │       ├── result/           # 统一返回
│       │       └── utils/            # 工具类
│       │
│       ├── sky-pojo/                 # 实体模块
│       │   └── src/main/java/com/sky/
│       │       ├── dto/              # 数据传输对象
│       │       ├── entity/           # 实体类
│       │       └── vo/               # 视图对象
│       │
│       └── sky-server/               # 服务模块
│           └── src/main/java/com/sky/
│               ├── config/           # 配置类
│               ├── controller/       # 控制器
│               │   ├── admin/        # 管理端接口
│               │   └── User/         # 用户端接口
│               ├── mapper/           # 数据访问层
│               ├── mq/               # 消息队列消费者
│               ├── service/          # 服务层
│               └── websocket/        # WebSocket服务
│
├── package.json                      # 用户端依赖配置
├── vite.config.js                    # Vite配置
└── PROJECT_SUMMARY.md                # 本文档
```

### 1.3 功能模块概览

| 模块 | 用户端 | 管理端 | 说明 |
|------|:------:|:------:|------|
| 题目浏览 | ✅ | ✅ | 题目列表、详情、搜索筛选 |
| 代码编辑 | ✅ | - | Monaco编辑器、多语言支持 |
| 代码判题 | ✅ | - | Judge0引擎、AI判题 |
| 提交记录 | ✅ | ✅ | 历史提交、状态筛选 |
| 比赛系统 | ✅ | ✅ | 比赛列表、参赛、排名 |
| 题组系统 | ✅ | ✅ | 题目集合、学习路径 |
| 讨论区 | ✅ | - | 题目讨论、评论回复 |
| 题解系统 | ✅ | - | 题解发布、Markdown渲染 |
| AI辅助 | ✅ | - | AI判题、错误分析、智能提示 |
| 用户管理 | - | ✅ | 用户列表、状态管理 |
| 数据统计 | - | ✅ | 可视化图表、运营数据 |
| 知识库 | - | ✅ | PDF导入、向量检索 |

---

## 二、技术架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端展示层                                       │
├────────────────────────────────┬────────────────────────────────────────────┤
│         用户端 (Vue 3)          │            管理端 (Vue 3)                   │
│  ┌──────────────────────────┐  │  ┌──────────────────────────────────────┐  │
│  │  Monaco Editor           │  │  │  ECharts 数据可视化                   │  │
│  │  WebSocket Client        │  │  │  Element Plus UI                     │  │
│  │  Element Plus UI         │  │  │  Axios HTTP Client                   │  │
│  │  Pinia State Management  │  │  │  Vue Router                          │  │
│  └──────────────────────────┘  │  └──────────────────────────────────────┘  │
└────────────────────────────────┴────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            后端服务层 (Spring Boot 3)                         │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │   Controller    │  │    Service      │  │    Mapper       │              │
│  │   控制器层       │  │    服务层        │  │    数据访问层    │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │   Judge0        │  │   Spring AI     │  │   WebSocket     │              │
│  │   判题引擎       │  │   AI服务         │  │   实时推送       │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │   JWT Token     │  │   Redis Lua     │  │   RocketMQ      │              │
│  │   身份认证       │  │   限流防重       │  │   消息队列       │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              数据存储层                                       │
├─────────────────┬─────────────────┬─────────────────┬───────────────────────┤
│     MySQL       │     Redis       │    RocketMQ     │    Aliyun OSS         │
│     数据库       │   缓存/向量      │    消息队列      │     对象存储           │
└─────────────────┴─────────────────┴─────────────────┴───────────────────────┘
```

### 2.2 技术栈详情

#### 用户端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.2.38 | 渐进式JavaScript框架 |
| Vue Router | 4.6.4 | 单页面应用路由管理 |
| Pinia | 3.0.4 | 新一代状态管理工具 |
| Element Plus | 2.13.1 | Vue 3 UI组件库 |
| Monaco Editor | 0.55.1 | 代码编辑器（VS Code同款） |
| Axios | 1.13.2 | HTTP请求库 |
| Marked | 17.0.3 | Markdown解析器 |
| Highlight.js | 11.11.1 | 代码语法高亮 |
| Vite | 3.0.9 | 下一代前端构建工具 |

#### 管理端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.2.38 | 渐进式JavaScript框架 |
| Vue Router | 4.6.4 | 路由管理 |
| Element Plus | 2.4.4 | UI组件库 |
| ECharts | 6.0.0 | 数据可视化图表库 |
| Axios | 1.13.1 | HTTP请求库 |
| Highlight.js | 11.11.1 | 代码语法高亮 |

#### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.6 | 核心框架 |
| Java | 17 | 编程语言 |
| MyBatis Plus | 3.5.7 | ORM框架，简化数据库操作 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 7.0+ | 缓存、限流、向量存储 |
| RocketMQ | 2.3.1 | 消息队列，异步处理 |
| Spring AI | 1.0.0-M5 | AI集成框架 |
| Judge0 | - | 代码执行引擎 |
| WebSocket | - | 实时双向通信 |
| JWT | 0.12.6 | JSON Web Token认证 |
| Knife4j | 4.5.0 | API文档生成 |
| Druid | 1.2.23 | 数据库连接池 |
| Aliyun OSS | 3.10.2 | 对象存储服务 |

---

## 三、核心功能实现

### 3.1 判题系统

#### 3.1.1 判题流程概述

系统支持两种判题方式：
1. **Judge0判题**：传统测试用例判题，适用于有标准测试用例的题目
2. **AI判题**：AI分析代码正确性，适用于无测试用例或需要智能分析的题目

#### 3.1.2 Judge0判题实现

**核心代码位置**：`sky-server/src/main/java/com/sky/service/impl/JudgeServiceImpl.java`

```java
public JudgeResultVO judge(JudgeSubmitDTO judgeSubmitDTO) {
    // 1. 获取题目和测试用例
    Problem problem = problemMapper.selectById(judgeSubmitDTO.getProblemId());
    List<TestCase> testCases = testCaseMapper.selectByProblemId(judgeSubmitDTO.getProblemId());
    
    // 2. 构建Judge0请求
    Judge0Request request = buildJudge0Request(judgeSubmitDTO, testCases);
    
    // 3. 调用Judge0 API执行代码
    Judge0Response response = judge0Client.submit(request);
    
    // 4. 解析执行结果
    return parseJudgeResult(response);
}
```

**支持的编程语言**：
- Java
- Python 3
- C++
- JavaScript
- C
- Go
- Rust

**判题状态定义**：

| 状态 | 说明 |
|------|------|
| Pending | 等待判题 |
| Judging | 正在判题 |
| Accepted | 通过 |
| Wrong Answer | 答案错误 |
| Time Limit Exceeded | 时间超限 |
| Memory Limit Exceeded | 内存超限 |
| Runtime Error | 运行时错误 |
| Compile Error | 编译错误 |

#### 3.1.3 AI判题实现

**核心代码位置**：`sky-server/src/main/java/com/sky/service/impl/AiJudgeServiceImpl.java`

```java
public void judgeByAiStream(JudgeTaskMessage message, WebSocketSession session) {
    // 1. 获取题目信息
    Problem problem = problemMapper.selectById(message.getProblemId());
    
    // 2. 构建AI提示词
    String prompt = buildAiJudgePrompt(problem, message.getCode(), message.getLanguage());
    
    // 3. 流式调用AI
    Flux<String> response = aiClient.streamChat(prompt);
    
    // 4. 实时推送AI分析结果
    response.subscribe(chunk -> {
        sendToWebSocket(session, chunk);
    });
    
    // 5. 解析判题结果并保存
    JudgeResult result = parseAiJudgeResult(response);
    saveSubmission(message, result);
}
```

**AI判题提示词模板**：

```
你是一个专业的代码评审专家。请分析以下代码是否正确解决了给定的问题。

【问题描述】
{problemContent}

【代码】
语言：{language}
代码：
{code}

请从以下几个方面分析：
1. 代码逻辑是否正确
2. 边界条件处理
3. 时间复杂度分析
4. 可能存在的问题

最后给出判题结果：Accepted 或 Wrong Answer
```

### 3.2 实时推送系统

#### 3.2.1 WebSocket实现

**核心代码位置**：`sky-server/src/main/java/com/sky/websocket/WebSocketServer.java`

```java
@ServerEndpoint("/ws/{userId}")
@Component
public class WebSocketServer {
    
    private static ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        sessions.put(userId, session);
        log.info("WebSocket连接建立: {}", userId);
    }
    
    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        sessions.remove(userId);
        log.info("WebSocket连接关闭: {}", userId);
    }
    
    public void sendToUser(String userId, String message) {
        Session session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        }
    }
    
    public void sendToAll(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(message);
            }
        });
    }
}
```

#### 3.2.2 消息格式

```json
{
    "type": "JUDGE_RESULT",
    "data": {
        "submissionId": 12345,
        "status": "Accepted",
        "runtime": 120,
        "memory": 10240,
        "testCasesPassed": 10,
        "totalTestCases": 10
    },
    "timestamp": "2024-01-15T10:30:00Z"
}
```

### 3.3 限流与防重系统

#### 3.3.1 Redis Lua脚本实现

**核心代码位置**：`sky-server/src/main/resources/lua/submit_and_update.lua`

```lua
-- 提交限流脚本
local key = KEYS[1]           -- 限流key: submit_limit:{userId}
local limit = tonumber(ARGV[1]) -- 限制次数
local window = tonumber(ARGV[2]) -- 时间窗口(秒)
local current = redis.call('GET', key)

if current and tonumber(current) >= limit then
    return 0  -- 超过限制
end

redis.call('INCR', key)
if tonumber(current) == 0 then
    redis.call('EXPIRE', key, window)
end

return 1  -- 允许提交
```

**调用方式**：

```java
public boolean tryAcquire(Long userId) {
    String key = "submit_limit:" + userId;
    Long result = redisTemplate.execute(
        submitScript,
        Collections.singletonList(key),
        "10",   // 60秒内最多10次提交
        "60"    // 时间窗口60秒
    );
    return result != null && result == 1;
}
```

#### 3.3.2 防重复提交

```lua
-- 防重复提交脚本
local pendingKey = KEYS[1]    -- pending:{userId}:{problemId}

if redis.call('EXISTS', pendingKey) == 1 then
    return 0  -- 已有任务在执行
end

redis.call('SET', pendingKey, '1')
redis.call('EXPIRE', pendingKey, 300)  -- 5分钟过期
return 1
```

### 3.4 AI辅助功能

#### 3.4.1 功能列表

| 功能 | 接口 | 说明 |
|------|------|------|
| AI判题 | POST /user/ai/judge/submit | 无测试用例时AI分析代码 |
| 错误分析 | POST /user/ai/analyze-error/submit | 分析代码错误原因 |
| 获取提示 | POST /user/ai/hint/submit | 获取解题思路引导 |
| AI对话 | POST /user/ai/chat/submit | 与AI自由讨论 |

#### 3.4.2 错误分析实现

```java
public String analyzeError(String code, String language, String errorMessage) {
    String prompt = String.format("""
        请分析以下代码的错误原因：
        
        【代码】
        语言：%s
        代码：
        %s
        
        【错误信息】
        %s
        
        请详细分析：
        1. 错误原因
        2. 如何修复
        3. 类似问题的避免方法
        """, language, code, errorMessage);
    
    return aiClient.chat(prompt);
}
```

#### 3.4.3 智能提示实现

```java
public String getHint(Long problemId, String currentCode) {
    Problem problem = problemMapper.selectById(problemId);
    
    String prompt = String.format("""
        用户正在解决以下问题，请给出适当的提示（不要直接给出答案）：
        
        【问题描述】
        %s
        
        【用户当前代码】
        %s
        
        请给出：
        1. 思路提示
        2. 可能需要的算法/数据结构
        3. 边界条件提醒
        """, problem.getContent(), currentCode);
    
    return aiClient.chat(prompt);
}
```

### 3.5 RAG知识库系统

#### 3.5.1 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      RAG知识库系统                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │  PDF文档    │ -> │  文档解析    │ -> │  文本分片    │     │
│  └─────────────┘    └─────────────┘    └─────────────┘     │
│                                               │             │
│                                               ▼             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │  AI回答     │ <- │  上下文增强  │ <- │  向量检索    │     │
│  └─────────────┘    └─────────────┘    └─────────────┘     │
│                           │                                 │
│                           ▼                                 │
│                    ┌─────────────┐                         │
│                    │  向量存储    │                         │
│                    │  (Redis)    │                         │
│                    └─────────────┘                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 3.5.2 知识导入实现

**核心代码位置**：`sky-server/src/main/java/com/sky/service/impl/KnowledgeImportServiceImpl.java`

```java
public void importPdf(String filePath, String category) {
    // 1. 解析PDF文档
    PdfReader reader = new PdfReader(filePath);
    String content = extractText(reader);
    
    // 2. 文本分片
    List<String> chunks = splitText(content, 500, 50);
    
    // 3. 生成向量嵌入并存储
    for (String chunk : chunks) {
        float[] embedding = aiClient.embed(chunk);
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setContent(chunk);
        doc.setEmbedding(embedding);
        doc.setCategory(category);
        vectorStore.add(doc);
    }
}
```

#### 3.5.3 知识检索实现

**核心代码位置**：`sky-server/src/main/java/com/sky/service/impl/KnowledgeRetrievalServiceImpl.java`

```java
public List<KnowledgeDocument> search(String query, int topK) {
    // 1. 将查询转换为向量
    float[] queryEmbedding = aiClient.embed(query);
    
    // 2. 向量相似度搜索
    List<KnowledgeDocument> results = vectorStore.similaritySearch(
        queryEmbedding, 
        topK,
        0.7  // 相似度阈值
    );
    
    return results;
}
```

### 3.6 消息队列异步处理

#### 3.6.1 判题任务消费者

**核心代码位置**：`sky-server/src/main/java/com/sky/mq/JudgeTaskConsumer.java`

```java
@Component
@RocketMQMessageListener(
    topic = "judge-task",
    consumerGroup = "judge-consumer"
)
public class JudgeTaskConsumer implements RocketMQListener<JudgeTaskMessage> {
    
    @Autowired
    private JudgeService judgeService;
    
    @Autowired
    private AiJudgeService aiJudgeService;
    
    @Autowired
    private WebSocketServer webSocketServer;
    
    @Override
    public void onMessage(JudgeTaskMessage message) {
        try {
            // 1. 判断判题类型
            Problem problem = problemMapper.selectById(message.getProblemId());
            
            JudgeResultVO result;
            if (problem.hasTestCases()) {
                // Judge0判题
                result = judgeService.judge(message);
            } else {
                // AI判题
                result = aiJudgeService.judge(message);
            }
            
            // 2. 保存提交记录
            saveSubmission(message, result);
            
            // 3. 更新题目统计
            updateProblemStats(message.getProblemId(), result);
            
            // 4. WebSocket推送结果
            webSocketServer.sendToUser(
                message.getUserId().toString(),
                JSON.toJSONString(result)
            );
            
        } catch (Exception e) {
            log.error("判题任务处理失败", e);
        }
    }
}
```

---

## 四、核心流程图

### 4.1 判题流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              判题流程                                        │
└─────────────────────────────────────────────────────────────────────────────┘

用户提交代码
      │
      ▼
┌─────────────┐
│  语法检查    │ ◄── AI快速检测语法错误（可选）
└─────────────┘
      │
      ▼
┌─────────────┐     否
│  限流检查    │ ──────────────► 返回"提交过于频繁"
└─────────────┘
      │ 是
      ▼
┌─────────────┐     是
│  防重检查    │ ──────────────► 返回"已有任务在执行"
└─────────────┘
      │ 否
      ▼
┌─────────────┐
│ 生成提交ID   │
│ 创建提交记录  │
└─────────────┘
      │
      ▼
┌─────────────┐
│ 发送到MQ     │
└─────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│                     RocketMQ消息队列                         │
└─────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────┐
│ 消费判题任务  │
└─────────────┘
      │
      ▼
┌─────────────┐
│ 检查测试用例  │
└─────────────┘
      │
      ├────────────────────┐
      │ 有测试用例          │ 无测试用例
      ▼                    ▼
┌─────────────┐     ┌─────────────┐
│ Judge0判题   │     │  AI判题     │
└─────────────┘     └─────────────┘
      │                    │
      └────────┬───────────┘
               ▼
        ┌─────────────┐
        │ 保存结果     │
        └─────────────┘
               │
               ▼
        ┌─────────────┐
        │ 更新统计     │
        └─────────────┘
               │
               ▼
        ┌─────────────┐
        │ WebSocket推送│
        └─────────────┘
               │
               ▼
           用户收到结果
```

### 4.2 AI辅助流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            AI辅助功能流程                                    │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────┐
                    │    用户请求      │
                    └─────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│   AI判题      │   │   错误分析     │   │   获取提示     │
└───────────────┘   └───────────────┘   └───────────────┘
        │                   │                   │
        ▼                   ▼                   ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ 构建判题提示词 │   │ 构建分析提示词 │   │ 构建提示提示词 │
│               │   │               │   │               │
│ - 问题描述    │   │ - 代码        │   │ - 问题描述    │
│ - 代码       │   │ - 错误信息     │   │ - 当前代码    │
│ - 语言       │   │ - 语言        │   │ - 进度       │
└───────────────┘   └───────────────┘   └───────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            ▼
                    ┌───────────────┐
                    │  Spring AI    │
                    │  调用大模型     │
                    └───────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  流式响应      │
                    │  SSE推送      │
                    └───────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  前端实时展示   │
                    └───────────────┘
```

### 4.3 用户认证流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            用户认证流程                                      │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────┐
│  用户登录    │
└─────────────┘
      │
      ▼
┌─────────────┐
│ 验证用户名   │
│ 验证密码     │
└─────────────┘
      │
      ├─────────────┐
      │ 验证失败     │ 验证成功
      ▼             ▼
┌─────────────┐ ┌─────────────┐
│ 返回错误信息 │ │ 生成JWT Token│
└─────────────┘ └─────────────┘
                      │
                      ▼
                ┌─────────────┐
                │ 返回Token    │
                │ 用户信息     │
                └─────────────┘
                      │
                      ▼
                ┌─────────────┐
                │ 前端存储Token│
                │ localStorage │
                └─────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                     后续请求                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  请求 ──► 拦截器 ──► 验证Token ──► 解析用户ID ──► 放行请求    │
│                           │                                 │
│                           ▼                                 │
│                     Token无效 ──► 返回401                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.4 RAG知识库流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          RAG知识库问答流程                                   │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              知识导入                                        │
└─────────────────────────────────────────────────────────────────────────────┘

PDF文档 ──► 文档解析 ──► 文本分片 ──► 向量嵌入 ──► 存储到Redis
                                              │
                                              ▼
                                        向量数据库
                                     (Redis Vector Store)


┌─────────────────────────────────────────────────────────────────────────────┐
│                              知识检索                                        │
└─────────────────────────────────────────────────────────────────────────────┘

用户提问
    │
    ▼
文本向量化
    │
    ▼
向量相似度搜索 ──► 返回Top-K相关文档
    │
    ▼
构建增强上下文
    │
    ├─── 系统提示词
    ├─── 检索到的知识
    └─── 用户问题
    │
    ▼
调用AI生成回答
    │
    ▼
返回答案给用户
```

### 4.5 比赛系统流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            比赛系统流程                                      │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              比赛创建                                        │
└─────────────────────────────────────────────────────────────────────────────┘

管理员创建比赛
      │
      ├─── 设置比赛信息（标题、时间、类型）
      ├─── 添加比赛题目
      └─── 设置题目分值
      │
      ▼
比赛创建成功


┌─────────────────────────────────────────────────────────────────────────────┐
│                              比赛进行                                        │
└─────────────────────────────────────────────────────────────────────────────┘

用户参加比赛
      │
      ▼
进入比赛页面
      │
      ├─── 查看题目列表
      ├─── 选择题目作答
      └─── 提交代码
      │
      ▼
判题系统判题
      │
      ├─── 更新比赛排名
      └─── 更新个人得分
      │
      ▼
实时排名展示


┌─────────────────────────────────────────────────────────────────────────────┐
│                              比赛结束                                        │
└─────────────────────────────────────────────────────────────────────────────┘

比赛时间结束
      │
      ├─── 计算最终排名
      ├─── 更新用户Rating
      └─── 生成比赛报告
      │
      ▼
比赛结果公示
```

---

## 五、数据库设计

### 5.1 核心数据表

#### 5.1.1 用户表 (users)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名，唯一 |
| password_hash | VARCHAR(255) | 密码哈希值 |
| nick_name | VARCHAR(50) | 昵称 |
| email | VARCHAR(100) | 邮箱 |
| avatar_url | VARCHAR(500) | 头像URL |
| role | VARCHAR(20) | 角色：student/teacher/admin |
| status | TINYINT | 状态：1启用/0禁用 |
| points | INT | 积分 |
| rating | INT | 竞赛评分 |
| daily_question_streak | INT | 连续刷题天数 |
| total_submissions | INT | 总提交次数 |
| last_login_time | DATETIME | 上次登录时间 |
| vip_expire_time | DATETIME | VIP过期时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 5.1.2 题目表 (problems)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| title | VARCHAR(200) | 题目标题 |
| content | TEXT | 题目描述（Markdown） |
| difficulty | VARCHAR(20) | 难度：Easy/Medium/Hard |
| acceptance | DECIMAL(5,2) | 通过率 |
| problem_type | VARCHAR(50) | 类型：Algorithm/Database/Shell/Concurrency |
| template_code | TEXT | 代码模板（JSON格式） |
| solution | TEXT | 参考答案 |
| hints | TEXT | 提示（JSON数组） |
| time_limit | INT | 时间限制（毫秒） |
| memory_limit | INT | 内存限制（KB） |
| db_schema | TEXT | 数据库架构（SQL题） |
| db_init_data | TEXT | 数据库初始数据 |
| status | TINYINT | 状态：1上架/0下架 |
| submit_count | INT | 提交次数 |
| accepted_count | INT | 通过次数 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 5.1.3 提交记录表 (submissions)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 用户ID |
| problem_id | BIGINT | 题目ID |
| code | TEXT | 提交代码 |
| language | VARCHAR(20) | 编程语言 |
| status | VARCHAR(50) | 判题状态 |
| runtime_ms | INT | 运行时间（毫秒） |
| memory_kb | INT | 内存消耗（KB） |
| test_cases_passed | INT | 通过测试用例数 |
| total_test_cases | INT | 总测试用例数 |
| error_message | TEXT | 错误信息 |
| ai_analysis | TEXT | AI分析结果 |
| created_at | DATETIME | 提交时间 |

#### 5.1.4 测试用例表 (test_cases)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| problem_id | BIGINT | 题目ID |
| input | TEXT | 输入数据 |
| expected_output | TEXT | 期望输出 |
| is_example | TINYINT | 是否示例用例 |
| sort_order | INT | 排序 |
| created_at | DATETIME | 创建时间 |

#### 5.1.5 比赛表 (contests)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| title | VARCHAR(200) | 比赛标题 |
| description | TEXT | 比赛描述 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| type | VARCHAR(50) | 类型：Weekly/Biweekly/Mock Interview/Company |
| status | TINYINT | 状态：0未开始/1进行中/2已结束 |
| creator_id | BIGINT | 创建者ID |
| created_at | DATETIME | 创建时间 |

#### 5.1.6 比赛题目关联表 (contest_problems)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| contest_id | BIGINT | 比赛ID |
| problem_id | BIGINT | 题目ID |
| score | INT | 题目分值 |
| sort_order | INT | 排序 |

#### 5.1.7 题组表 (problem_groups)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| title | VARCHAR(200) | 题组标题 |
| description | TEXT | 题组描述 |
| creator_id | BIGINT | 创建者ID |
| is_public | TINYINT | 是否公开 |
| view_count | INT | 浏览次数 |
| like_count | INT | 点赞数 |
| created_at | DATETIME | 创建时间 |

#### 5.1.8 题组题目关联表 (group_problems)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| group_id | BIGINT | 题组ID |
| problem_id | BIGINT | 题目ID |
| sort_order | INT | 排序 |

### 5.2 ER图

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   users     │       │  problems   │       │ test_cases  │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id (PK)     │       │ id (PK)     │◄──────│ problem_id  │
│ username    │       │ title       │       │ input       │
│ nick_name   │       │ content     │       │ expected_   │
│ role        │       │ difficulty  │       │   output    │
│ points      │       │ template_   │       └─────────────┘
│ rating      │       │   code      │
└─────────────┘       └─────────────┘
      │                     │
      │                     │
      ▼                     ▼
┌─────────────┐       ┌─────────────┐
│ submissions │       │contest_     │
├─────────────┤       │  problems   │
│ id (PK)     │       ├─────────────┤
│ user_id(FK) │       │ contest_id  │
│ problem_id  │       │ problem_id  │
│ code        │       │ score       │
│ language    │       └─────────────┘
│ status      │             ▲
└─────────────┘             │
                            │
                      ┌─────────────┐
                      │  contests   │
                      ├─────────────┤
                      │ id (PK)     │
                      │ title       │
                      │ start_time  │
                      │ end_time    │
                      └─────────────┘
```

---

## 六、API接口设计

### 6.1 用户端接口

#### 6.1.1 认证模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /user/login | 用户登录 |
| POST | /user/register | 用户注册 |
| GET | /user/userInfo | 获取个人信息 |
| PUT | /user/userInfo | 更新个人信息 |
| GET | /user/userInfo/sign | 用户签到 |
| GET | /user/userInfo/sign/count | 获取签到天数 |

#### 6.1.2 题目模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /user/problem/page | 分页查询题目 |
| GET | /user/problem/{id} | 获取题目详情 |
| GET | /user/problem/types | 获取题目类型列表 |

#### 6.1.3 判题模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /user/judge/submit | 提交判题 |
| POST | /user/judge/run | 运行代码（不保存） |

#### 6.1.4 AI模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /user/ai/judge/submit | AI判题 |
| POST | /user/ai/analyze-error/submit | 错误分析 |
| POST | /user/ai/hint/submit | 获取提示 |
| POST | /user/ai/chat/submit | AI对话 |

#### 6.1.5 提交记录模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /user/submission/page | 分页查询提交记录 |
| GET | /user/submission/{id} | 获取提交详情 |

#### 6.1.6 比赛模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /user/contest/page | 分页查询比赛 |
| GET | /user/contest/{id} | 获取比赛详情 |

#### 6.1.7 题组模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /user/group/page | 分页查询题组 |
| GET | /user/group/{id} | 获取题组详情 |

### 6.2 管理端接口

#### 6.2.1 题目管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /admin/problem | 添加题目 |
| GET | /admin/problem/page | 分页查询 |
| PUT | /admin/problem | 编辑题目 |
| DELETE | /admin/problem | 删除题目 |
| PUT | /admin/problem/status/{id} | 上架/下架 |

#### 6.2.2 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/user/page | 分页查询用户 |
| PUT | /admin/user/status/{id} | 启用/禁用用户 |

#### 6.2.3 比赛管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /admin/contest | 创建比赛 |
| GET | /admin/contest/page | 分页查询 |
| PUT | /admin/contest | 编辑比赛 |
| DELETE | /admin/contest | 删除比赛 |

#### 6.2.4 知识库管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /admin/knowledge/import/pdf | 导入PDF |
| POST | /admin/knowledge/import/directory | 批量导入目录 |
| DELETE | /admin/knowledge/clear | 清空知识库 |

### 6.3 接口响应格式

#### 成功响应

```json
{
    "code": 200,
    "msg": "success",
    "data": {
        // 返回数据
    }
}
```

#### 分页响应

```json
{
    "code": 200,
    "msg": "success",
    "data": {
        "records": [],
        "total": 100,
        "page": 1,
        "pageSize": 10
    }
}
```

#### 错误响应

```json
{
    "code": 500,
    "msg": "错误信息",
    "data": null
}
```

---

## 七、关键技术点

### 7.1 Redis应用

| 场景 | 实现方式 | 说明 |
|------|---------|------|
| 限流 | Lua脚本 + INCR | 60秒内提交次数限制 |
| 防重 | SETNX | 同一用户同一题目同时只能有一个判题任务 |
| 缓存 | String/Hash | 题目详情、用户信息缓存 |
| 向量存储 | VectorStore | RAG知识库向量存储 |
| 分布式锁 | SETNX + EXPIRE | 防止重复操作 |

### 7.2 消息队列应用

| 场景 | Topic | 说明 |
|------|-------|------|
| 判题任务 | judge-task | 异步处理判题请求 |
| 数据更新 | database-update | 异步更新统计数据 |

### 7.3 WebSocket应用

| 场景 | 消息类型 | 说明 |
|------|---------|------|
| 判题结果推送 | JUDGE_RESULT | 实时推送判题结果 |
| 系统通知 | NOTIFICATION | 系统公告推送 |

### 7.4 安全机制

| 机制 | 实现方式 |
|------|---------|
| 身份认证 | JWT Token |
| 密码加密 | BCrypt |
| 接口权限 | 拦截器 + 角色判断 |
| SQL注入防护 | MyBatis Plus参数化查询 |
| XSS防护 | 输入过滤 + 输出编码 |

---

## 八、项目亮点

### 8.1 技术亮点

1. **AI深度集成**
   - AI判题：无测试用例时AI分析代码正确性
   - AI错误分析：智能分析代码错误原因
   - AI提示系统：循序渐进引导用户思考
   - RAG知识库：向量检索增强的AI问答

2. **实时判题系统**
   - WebSocket实时推送判题结果
   - HTTP轮询降级方案
   - SSE流式响应

3. **高并发设计**
   - Redis Lua脚本限流
   - 消息队列异步处理
   - 防重复提交机制

4. **完善的用户体系**
   - 角色权限管理
   - 积分系统
   - Rating系统
   - 签到系统

### 8.2 业务亮点

1. **多题型支持**
   - 算法题
   - 数据库题（SQL）
   - Shell题
   - 并发题

2. **多语言支持**
   - Java
   - Python
   - C++
   - JavaScript
   - Go
   - Rust

3. **完整的比赛系统**
   - 周赛
   - 双周赛
   - 模拟面试
   - 企业专场

4. **知识库系统**
   - PDF文档导入
   - 向量检索
   - RAG增强问答

---

## 九、部署说明

### 9.1 环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Node.js | 16+ |
| MySQL | 8.0+ |
| Redis | 7.0+ |
| RocketMQ | 5.0+ |

### 9.2 配置文件

#### 后端配置 (application.yml)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oj_system
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
  ai:
    openai:
      api-key: your_api_key
      base-url: https://api.openai.com

judge0:
  url: http://localhost:2358

rocketmq:
  name-server: localhost:9876
```

#### 前端配置 (vite.config.js)

```javascript
export default defineConfig({
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

### 9.3 启动步骤

#### 后端启动

```bash
# 1. 创建数据库
mysql -u root -p < init.sql

# 2. 启动Redis
redis-server

# 3. 启动RocketMQ
mqnamesrv
mqbroker -n localhost:9876

# 4. 启动Judge0
docker-compose up -d

# 5. 启动后端服务
mvn spring-boot:run
```

#### 前端启动

```bash
# 用户端
cd vue-project1
npm install
npm run dev

# 管理端
cd vue-Element
npm install
npm run dev
```

### 9.4 访问地址

| 服务 | 地址 |
|------|------|
| 用户端 | http://localhost:5174 |
| 管理端 | http://localhost:5173 |
| 后端API | http://localhost:8080 |
| API文档 | http://localhost:8080/doc.html |

---

## 十、总结

本项目是一个功能完善、技术先进的在线判题系统，主要特点包括：

1. **技术先进**：采用Spring Boot 3 + Vue 3 + AI技术栈，紧跟技术发展趋势
2. **功能完整**：涵盖题目管理、判题系统、比赛系统、用户系统等核心功能
3. **创新性强**：AI判题、RAG知识库等功能具有创新性
4. **架构合理**：前后端分离、消息队列异步处理、WebSocket实时推送
5. **可扩展性好**：模块化设计，易于扩展新功能

项目适合作为毕业设计、简历项目或学习实践使用。

---

**文档版本**：v1.0  
**更新日期**：2024年1月  
**作者**：项目开发者
