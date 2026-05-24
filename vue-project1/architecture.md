# OJ项目架构与流程分析

## 1. 整体架构图

```mermaid
flowchart TD
    subgraph 前端层
        User_Web[用户前端] --> Vue3
        Admin_Web[管理后台前端] --> Vue3_Element
    end

    subgraph 后端层
        SpringBoot[Spring Boot应用]
        subgraph 核心服务
            JudgeService[判题服务]
            UserService[用户服务]
            ProblemService[题目服务]
            ContestService[竞赛服务]
            GroupService[小组服务]
            AIService[AI服务]
        end
        subgraph 配置与工具
            JwtUtil[JWT工具]
            RedisConfig[Redis配置]
            RocketMQConfig[RocketMQ配置]
            Judge0Config[Judge0配置]
        end
    end

    subgraph 中间件层
        Redis[Redis缓存]
        RocketMQ[RocketMQ消息队列]
        Judge0[Judge0判题引擎]
    end

    subgraph 数据层
        MySQL[MySQL数据库]
        FileStorage[文件存储]
    end

    User_Web -->|HTTP请求| SpringBoot
    Admin_Web -->|HTTP请求| SpringBoot
    
    SpringBoot --> JudgeService
    SpringBoot --> UserService
    SpringBoot --> ProblemService
    SpringBoot --> ContestService
    SpringBoot --> GroupService
    SpringBoot --> AIService
    
    JudgeService -->|消息队列| RocketMQ
    RocketMQ -->|消费消息| JudgeService
    JudgeService -->|调用| Judge0
    
    UserService -->|缓存| Redis
    ProblemService -->|缓存| Redis
    
    UserService --> MySQL
    ProblemService --> MySQL
    ContestService --> MySQL
    GroupService --> MySQL
    
    AIService -->|API调用| OpenAI
    
    SpringBoot -->|文件上传| FileStorage
```

## 2. 核心功能流程图

### 2.1 用户登录流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Frontend as 前端
    participant Backend as 后端API
    participant Redis as Redis缓存
    participant MySQL as 数据库

    User->>Frontend: 输入用户名密码
    Frontend->>Backend: POST /user/userLogin/login
    Backend->>Backend: 验证验证码
    Backend->>MySQL: 查询用户信息
    alt 用户存在且密码正确
        MySQL-->>Backend: 返回用户信息
        Backend->>Backend: 生成JWT令牌
        Backend->>Redis: 存储用户会话信息
        Backend-->>Frontend: 返回token和用户信息
        Frontend->>Frontend: 存储token到localStorage
        Frontend-->>User: 登录成功
    else 用户不存在或密码错误
        MySQL-->>Backend: 返回空
        Backend-->>Frontend: 返回登录失败
        Frontend-->>User: 登录失败
    end
```

### 2.2 代码提交判题流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Frontend as 前端
    participant Backend as 后端API
    participant RocketMQ as 消息队列
    participant JudgeService as 判题服务
    participant Judge0 as Judge0引擎
    participant MySQL as 数据库

    User->>Frontend: 编写代码并提交
    Frontend->>Backend: POST /user/judge/submit
    Backend->>MySQL: 保存提交记录
    Backend->>RocketMQ: 发送判题任务
    RocketMQ-->>JudgeService: 消费判题任务
    JudgeService->>Judge0: 执行代码判题
    Judge0-->>JudgeService: 返回判题结果
    JudgeService->>MySQL: 更新提交记录状态
    JudgeService-->>User: 通过WebSocket推送结果
    Frontend->>Backend: 查询判题结果
    Backend->>MySQL: 获取判题结果
    Backend-->>Frontend: 返回判题结果
    Frontend-->>User: 显示判题结果
```

### 2.3 竞赛参与流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Frontend as 前端
    participant Backend as 后端API
    participant MySQL as 数据库

    User->>Frontend: 进入竞赛页面
    Frontend->>Backend: GET /contest/{id}
    Backend->>MySQL: 查询竞赛信息
    Backend-->>Frontend: 返回竞赛详情
    Frontend-->>User: 显示竞赛信息
    
    User->>Frontend: 点击参与竞赛
    Frontend->>Backend: POST /contest/join
    Backend->>MySQL: 记录参赛信息
    Backend-->>Frontend: 返回参与成功
    
    User->>Frontend: 查看竞赛题目
    Frontend->>Backend: GET /contest/{id}/problems
    Backend->>MySQL: 查询竞赛题目
    Backend-->>Frontend: 返回题目列表
    
    User->>Frontend: 提交竞赛题目答案
    Frontend->>Backend: POST /contest/submit
    Backend->>MySQL: 保存提交记录
    Backend->>Backend: 计算竞赛得分
    Backend-->>Frontend: 返回提交结果
    
    User->>Frontend: 查看竞赛排名
    Frontend->>Backend: GET /contest/{id}/rank
    Backend->>MySQL: 计算排名
    Backend-->>Frontend: 返回排名信息
    Frontend-->>User: 显示竞赛排名
```

## 3. 系统模块关系图

```mermaid
flowchart TD
    subgraph 用户模块
        UserAuth[用户认证]
        UserProfile[用户信息管理]
        UserAttendance[用户签到]
    end

    subgraph 题目模块
        ProblemManage[题目管理]
        ProblemType[题目分类]
        TestCase[测试用例]
    end

    subgraph 判题模块
        JudgeSubmit[代码提交]
        JudgeRun[代码运行]
        JudgeResult[判题结果]
    end

    subgraph 竞赛模块
        ContestManage[竞赛管理]
        ContestProblem[竞赛题目]
        ContestRank[竞赛排名]
    end

    subgraph 小组模块
        GroupManage[小组管理]
        GroupProblems[小组题目]
    end

    subgraph AI模块
        AIChat[AI聊天]
        AIJudge[AI辅助判题]
        KnowledgeBase[知识库]
    end

    UserAuth --> UserProfile
    UserProfile --> UserAttendance
    
    ProblemManage --> ProblemType
    ProblemManage --> TestCase
    
    JudgeSubmit --> JudgeRun
    JudgeRun --> JudgeResult
    
    ContestManage --> ContestProblem
    ContestProblem --> ContestRank
    
    GroupManage --> GroupProblems
    
    AIChat --> KnowledgeBase
    AIJudge --> KnowledgeBase
    
    UserAuth --> JudgeSubmit
    ProblemManage --> JudgeSubmit
    ContestProblem --> JudgeSubmit
    GroupProblems --> JudgeSubmit
```

## 4. 数据流向图

```mermaid
flowchart LR
    subgraph 输入层
        UserInput[用户输入]
        AdminInput[管理员输入]
    end

    subgraph 处理层
        API_Gateway[API网关]
        Service_Layer[服务层]
        MQ_Broker[消息队列]
        Cache_Layer[缓存层]
    end

    subgraph 执行层
        Judge_Engine[判题引擎]
        AI_Service[AI服务]
    end

    subgraph 存储层
        Database[数据库]
        File_Storage[文件存储]
    end

    UserInput -->|HTTP请求| API_Gateway
    AdminInput -->|HTTP请求| API_Gateway
    
    API_Gateway -->|路由| Service_Layer
    Service_Layer -->|缓存操作| Cache_Layer
    Service_Layer -->|消息发送| MQ_Broker
    MQ_Broker -->|消息消费| Service_Layer
    
    Service_Layer -->|调用| Judge_Engine
    Service_Layer -->|调用| AI_Service
    
    Service_Layer -->|CRUD操作| Database
    Service_Layer -->|文件操作| File_Storage
    
    Cache_Layer -->|数据同步| Database
    Judge_Engine -->|结果返回| Service_Layer
    AI_Service -->|结果返回| Service_Layer
    
    Service_Layer -->|响应| API_Gateway
    API_Gateway -->|HTTP响应| UserInput
    API_Gateway -->|HTTP响应| AdminInput
```

## 5. 技术栈分析

| 类别 | 技术 | 用途 | 来源 |
|------|------|------|------|
| 前端框架 | Vue 3 | 用户前端界面 | d:\vue\vue-project1\src |
| 前端框架 | Vue 3 + Element Plus | 管理后台界面 | d:\vue\vue-project1\vue-Element\src |
| 后端框架 | Spring Boot | 后端应用框架 | d:\vue\vue-project1\vue-Element\oj-project\oj-web |
| 数据库 | MySQL | 数据存储 | application.yml |
| 缓存 | Redis | 缓存和会话管理 | application.yml |
| 消息队列 | RocketMQ | 判题任务队列 | application.yml |
| 判题引擎 | Judge0 | 代码判题 | Judge0Config.java |
| AI服务 | OpenAI API | AI辅助功能 | application.yml |
| 认证 | JWT | 用户认证 | JwtUtil.java |
| 文档 | Swagger/Knife4j | API文档 | application.yml |

## 6. 核心API分析

### 6.1 用户相关API
- `POST /user/userLogin/login` - 用户登录
- `POST /user/userRegister` - 用户注册
- `GET /user/userInfo` - 获取用户信息
- `PUT /user/userInfo` - 更新用户信息
- `GET /user/userInfo/sign` - 用户签到
- `GET /user/userInfo/sign/count` - 获取签到统计

### 6.2 题目相关API
- `GET /user/problem/type` - 获取题目列表
- `GET /user/problem/alltype` - 获取题目分类
- `GET /user/problem/{id}` - 获取题目详情

### 6.3 判题相关API
- `POST /user/judge/submit` - 提交代码判题
- `POST /user/judge/run` - 运行代码

### 6.4 竞赛相关API
- `GET /contest/{id}` - 获取竞赛详情
- `POST /contest/join` - 参与竞赛
- `GET /contest/{id}/problems` - 获取竞赛题目
- `POST /contest/submit` - 提交竞赛题目
- `GET /contest/{id}/rank` - 获取竞赛排名

### 6.5 小组相关API
- `GET /group` - 获取小组列表
- `GET /group/{id}` - 获取小组详情

## 7. 系统特点

1. **模块化设计**：后端采用分层架构，各模块职责清晰
2. **异步判题**：使用RocketMQ实现判题任务的异步处理
3. **缓存优化**：使用Redis缓存热点数据，提升性能
4. **AI集成**：集成OpenAI API，提供智能辅助功能
5. **前后端分离**：前端使用Vue 3，后端使用Spring Boot，通过API交互
6. **安全性**：使用JWT进行用户认证，保护API安全
7. **可扩展性**：模块化设计使得系统易于扩展和维护

## 8. 部署架构

```mermaid
flowchart TD
    subgraph 前端部署
        User_Frontend[用户前端] --> Nginx
        Admin_Frontend[管理后台前端] --> Nginx
    end

    subgraph 后端部署
        SpringBoot_App[Spring Boot应用] --> Tomcat
        Tomcat --> Load_Balancer[负载均衡]
    end

    subgraph 中间件部署
        Redis_Server[Redis服务器]
        RocketMQ_Server[RocketMQ服务器]
        Judge0_Server[Judge0服务器]
    end

    subgraph 数据存储
        MySQL_Server[MySQL服务器]
        File_Server[文件服务器]
    end

    User -->|HTTP| Nginx
    Admin -->|HTTP| Nginx
    Nginx -->|反向代理| Load_Balancer
    Load_Balancer -->|分发请求| Tomcat
    SpringBoot_App -->|缓存| Redis_Server
    SpringBoot_App -->|消息| RocketMQ_Server
    SpringBoot_App -->|判题| Judge0_Server
    SpringBoot_App -->|数据| MySQL_Server
    SpringBoot_App -->|文件| File_Server
```