# OJ-Onlinejudge 系统设计文档

> **版本**: v1.0  
> **架构**: Spring Cloud 微服务  
> **日期**: 2026-05-28

---

## 1. 系统概述

OJ-Onlinejudge 是一个在线判题系统，支持代码提交与自动判题、AI 智能辅助、竞赛管理、Hack 机制、题单管理、RAG 知识库等功能。系统采用 Spring Cloud 微服务架构，前端双端分离（用户端 + 管理端），代码沙箱使用开源 Judge0。

### 1.1 核心功能

| 功能模块 | 说明 |
|----------|------|
| 在线判题 | 支持 Java/C++/Python 多语言提交，Judge0 沙箱自动编译运行判题 |
| 竞赛系统 | 周赛/双周赛/模拟面试/企业赛，实时排名（Redis ZSet） |
| Hack 机制 | 选手互相挑战：锁定题目→查看AC代码→构造数据→三方比对判题 |
| AI 智能辅助 | LangChain4j/LangGraph4j 多 Agent 编排，智能答疑、代码审查、学情分析 |
| 知识库(RAG) | 文档上传→向量化→语义检索，辅助 AI 回答编程问题 |
| 题单管理 | 按算法类型组织题目集合，支持分类检索 |
| 题解社区 | 用户发布题解/评论，支持点赞和浏览统计 |

### 1.2 用户角色

| 角色 | 权限 |
|------|------|
| student | 做题、参赛、查看题解、使用 AI 助手、签到 |
| teacher | 学生权限 + 创建管理题目、题单、竞赛 |
| admin | 全部权限 + 用户管理、系统配置 |

---

## 2. 系统架构

### 2.1 整体架构图

```
┌──────────────────────────────────────────────────────────────┐
│                         Frontend                              │
│   vue-project1 (用户端)              vue-Element (管理端)       │
│   Vue 3 + Element Plus +            Vue 3 + Element Plus +    │
│   Monaco Editor + Pinia             ECharts + Pinia           │
└──────────────┬───────────────────────────────┬────────────────┘
               │           /api                 │
               ▼                               ▼
┌──────────────────────────────────────────────────────────────┐
│                     oj-gateway :8080                          │
│         Spring Cloud Gateway (路由/JWT鉴权/CORS/限流)          │
└─────┬─────────┬──────────┬──────────┬──────────┬─────────────┘
      │         │          │          │          │
      ▼         ▼          ▼          ▼          ▼
┌──────────┐┌──────────┐┌──────────┐┌──────────┐┌─────────────┐
│   user   ││ problem  ││ contest  ││  judge   ││     ai      │
│  :8081   ││  :8082   ││  :8083   ││  :8084   ││    :8086    │
│ 用户服务  ││题目/题单  ││竞赛/报告  ││判题/提交  ││ AI智能辅助   │
└────┬─────┘└────┬─────┘└────┬─────┘└────┬─────┘└──────┬──────┘
     │           │           │           │             │
     └───────────┴───────────┴───────────┘             │
                 │                                     │
     ┌───────────┴────────────┐              ┌─────────┴────────┐
     │   Nacos + Sentinel     │              │ LangChain4j       │
     │   服务注册/配置/熔断     │              │ LangGraph4j       │
     │   192.168.141.129      │              │ SiliconFlow API   │
     └────────────────────────┘              └──────────────────┘
                 │
     ┌───────────┴────────────┐
     │ RocketMQ + Redis       │
     │ Judge0 (判题沙箱)       │
     │ Skywalking (链路追踪)   │
     │ 192.168.141.128        │
     └────────────────────────┘
```

### 2.2 技术选型

#### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.4.2 | 应用框架 |
| Spring Cloud | 2024.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.3.2 | 微服务治理 |
| Nacos | 2.4.3 | 服务注册 & 配置中心 |
| Sentinel | — | 流量控制 & 熔断降级 |
| OpenFeign | — | 服务间同步调用 |
| RocketMQ | 2.3.1 | 异步判题消息分发 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| Druid | 1.2.23 | 数据库连接池 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.0 (Jedis) | 缓存 / 排行榜 / 限流 |
| JJWT | 0.12.6 | JWT 令牌 |
| LangChain4j | 1.12.2 | AI Agent 框架 |
| LangGraph4j | 1.8.11 | Agent 编排 (StateGraph) |
| Knife4j | 4.5.0 | API 文档 |
| Judge0 | — | 代码编译执行沙箱 |
| Skywalking | — | 分布式链路追踪 |

#### 前端技术栈

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

### 2.3 微服务模块划分

| 服务模块 | 端口 | 数据库 | 核心职责 |
|----------|:---:|--------|----------|
| oj-gateway | 8080 | — | 路由转发、JWT 鉴权、CORS、Sentinel 限流 |
| oj-user-service | 8081 | oj_user_db | 登录注册、JWT 签发、用户管理、签到、验证码 |
| oj-problem-service | 8082 | oj_problem_db | 题目 CRUD、题单管理、测试用例、标签分类、RAG 知识库 |
| oj-contest-service | 8083 | oj_contest_db | 竞赛管理、参赛排名、Hack 机制、题解评论、工作台 |
| oj-judge-service | 8084 | oj_judge_db | 代码提交、Judge0 判题、RocketMQ 异步处理、WebSocket 推送 |
| oj-ai-service | 8086 | — | AI Agent 编排、RAG 检索、学情分析、代码审查 |
| oj-common | — | — | 公共模块（Result、异常、JWT、常量、枚举） |
| oj-common-api | — | — | Feign 接口定义 + Sentinel Fallback 降级 |

---

## 3. 数据库设计

### 3.1 用户服务 (oj_user_db)

#### 3.1.1 用户表 `user`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | UNIQUE NOT NULL | 用户名 |
| password_hash | VARCHAR(255) | NOT NULL | 密码哈希值 |
| nickname | VARCHAR(50) | — | 昵称 |
| email | VARCHAR(100) | — | 邮箱 |
| avatar_url | VARCHAR(500) | — | 头像URL (OSS) |
| role | VARCHAR(20) | NOT NULL DEFAULT 'student' | 角色: student/teacher/admin |
| status | INT | NOT NULL DEFAULT 1 | 状态: 1-启用, 0-禁用 |
| points | INT | DEFAULT 0 | 积分 |
| rating | INT | DEFAULT 0 | 竞赛评分 |
| daily_question_streak | INT | DEFAULT 0 | 连续刷题天数 |
| total_submissions | INT | DEFAULT 0 | 总提交次数 |
| last_login_time | DATETIME | — | 上次登录时间 |
| vip_expire_time | DATETIME | — | VIP过期时间 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

#### 3.1.2 用户签到表 `user_attendance`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 签到ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| date | DATE | NOT NULL | 签到日期 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

> 索引: `idx_user_date (user_id, date)`

---

### 3.2 题目服务 (oj_problem_db)

#### 3.2.1 题目表 `problems`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK AUTO_INCREMENT | 题目ID |
| title | VARCHAR(200) | NOT NULL | 题目标题 |
| content | TEXT | — | 题目描述 (Markdown) |
| difficulty | VARCHAR(20) | DEFAULT 'Easy' | 难度: Easy/Medium/Hard |
| acceptance | DECIMAL(5,2) | DEFAULT 0.00 | 通过率(%) |
| frequency | VARCHAR(20) | DEFAULT 'Low' | 出现频率: Low/Medium/High |
| problem_type | VARCHAR(50) | DEFAULT 'Algorithm' | 大类: Algorithm/Database/Shell/Concurrency |
| time_limit_ms | INT | DEFAULT 1000 | 时间限制(毫秒) |
| memory_limit_mb | INT | DEFAULT 256 | 内存限制(MB) |
| likes_count | INT | DEFAULT 0 | 点赞数 |
| dislikes_count | INT | DEFAULT 0 | 点踩数 |
| status | INT | DEFAULT 1 | 状态: 1-上架, 0-下架 |
| template_code | JSON | — | 代码模板 (按语言组织) |
| db_schema | TEXT | — | 数据库题表结构SQL |
| db_init_data | TEXT | — | 数据库题初始化数据 |
| validator_path | VARCHAR(512) | — | Hack C++校验器源码路径 |
| validator_exe_path | VARCHAR(512) | — | 校验器编译产物路径 |
| validator_src_hash | VARCHAR(64) | — | 校验器源码 SHA-256 |
| reference_path | VARCHAR(512) | — | 标准解答文件路径 |
| reference_language | VARCHAR(32) | DEFAULT 'C++' | 标准解答语言 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

#### 3.2.2 题目类型表 `problem_types`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK AUTO_INCREMENT | 类型ID |
| name | VARCHAR(50) | NOT NULL | 类型名称 (如: 动态规划、贪心、DFS) |
| description | VARCHAR(200) | — | 类型描述 |
| is_active | INT | DEFAULT 1 | 是否激活 |
| sort_order | INT | DEFAULT 0 | 排序权重 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

#### 3.2.3 题目类型关联表 `problem_types_rel`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 关联ID |
| problem_id | INT | NOT NULL | 题目ID |
| type_id | INT | NOT NULL | 类型ID |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

> 索引: `idx_problem_id (problem_id)`, `idx_type_id (type_id)`

#### 3.2.4 测试用例表 `test_cases`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK AUTO_INCREMENT | 用例ID |
| problem_id | INT | NOT NULL | 题目ID |
| input_data | TEXT | — | 输入数据 |
| output_data | TEXT | — | 期望输出 |
| is_sample | TINYINT(1) | DEFAULT 0 | 是否样例 |
| order_num | SMALLINT | DEFAULT 0 | 顺序编号 |
| time_limit_ms | INT | — | 单独时间限制(可选) |
| memory_limit_mb | INT | — | 单独内存限制(可选) |
| score_weight | DOUBLE | DEFAULT 1.00 | 分数权重 |
| source_hack_id | BIGINT | — | 来源Hack记录ID (NULL=原始用例) |
| status | INT | DEFAULT 1 | 状态 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

> 索引: `idx_problem_id (problem_id)`

#### 3.2.5 题单(题组)表 `problem_groups`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK AUTO_INCREMENT | 题单ID |
| title | VARCHAR(200) | NOT NULL | 题单标题 |
| description | TEXT | — | 题单描述 |
| creator_id | BIGINT | — | 创建者ID |
| difficulty_range | VARCHAR(50) | DEFAULT 'All' | 难度范围 |
| estimated_duration_minutes | INT | — | 预计完成时长(分钟) |
| is_public | TINYINT(1) | DEFAULT 1 | 是否公开 |
| status | INT | DEFAULT 1 | 状态: 1-启用, 0-禁用 |
| view_count | INT | DEFAULT 0 | 浏览次数 |
| like_count | INT | DEFAULT 0 | 点赞数 |
| join_count | INT | DEFAULT 0 | 练习人数 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

#### 3.2.6 题单类型关联表 `group_types_rel`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 关联ID |
| group_id | INT | NOT NULL | 题单ID |
| type_id | INT | NOT NULL | 类型ID |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

> 索引: `idx_group_id (group_id)`, `idx_type_id (type_id)`

#### 3.2.7 题单题目关联表 `group_problems`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK AUTO_INCREMENT | 关联ID |
| group_id | INT | NOT NULL | 题单ID |
| problem_id | INT | NOT NULL | 题目ID |
| sort_order | INT | DEFAULT 0 | 题序 |
| score | INT | DEFAULT 10 | 分数 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

> 索引: `idx_group_id (group_id)`, `idx_problem_id (problem_id)`

---

### 3.3 竞赛服务 (oj_contest_db)

#### 3.3.1 竞赛表 `contests`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK AUTO_INCREMENT | 竞赛ID |
| title | VARCHAR(255) | NOT NULL | 竞赛名称 |
| description | TEXT | — | 竞赛描述 |
| start_time | DATETIME | NOT NULL | 开始时间 |
| end_time | DATETIME | NOT NULL | 结束时间 |
| type | VARCHAR(50) | DEFAULT 'Weekly Contest' | 类型: Weekly/Biweekly/Mock Interview/Company |
| status | VARCHAR(20) | DEFAULT 'Upcoming' | 状态: Upcoming/Running/Ended |
| created_by | BIGINT | — | 创建者ID |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

#### 3.3.2 竞赛题目关联表 `contest_problems`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK AUTO_INCREMENT | 关联ID |
| contest_id | INT | NOT NULL | 竞赛ID |
| problem_id | INT | NOT NULL | 题目ID |
| score | INT | DEFAULT 100 | 该题分数 |
| sort_order | INT | DEFAULT 0 | 题序 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

> 索引: `idx_contest_id (contest_id)`, `idx_problem_id (problem_id)`

#### 3.3.3 竞赛参赛表 `contest_participants`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 记录ID |
| contest_id | INT | NOT NULL | 竞赛ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| score | INT | DEFAULT 0 | 比赛总得分 |
| solved_count | INT | DEFAULT 0 | 通过题数 |
| registered_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 报名时间 |

> 唯一索引: `uk_contest_user (contest_id, user_id)`

#### 3.3.4 Hack记录表 `hack_records`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | Hack记录ID |
| contest_id | INT | NOT NULL | 竞赛ID |
| problem_id | INT | NOT NULL | 题目ID |
| hacker_id | BIGINT | NOT NULL | 发起者用户ID |
| target_user_id | BIGINT | NOT NULL | 目标用户ID |
| target_submission_id | BIGINT | NOT NULL | 目标提交记录ID |
| hack_input | MEDIUMTEXT | NOT NULL | Hack 测试数据(输入) |
| hack_output | MEDIUMTEXT | — | 标准解答输出结果 |
| status | VARCHAR(32) | DEFAULT 'Pending' | Pending/Validating/HackSuccess/HackFailed/InvalidData/SystemError |
| error_info | TEXT | — | 错误信息 |
| target_result | VARCHAR(32) | — | 目标代码运行结果 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

> 索引: `idx_contest_hacker`, `idx_contest_target`, `idx_contest_problem`

#### 3.3.5 题解评论表 `solution_comment`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| type | INT | NOT NULL DEFAULT 1 | 类型: 1-题解, 2-评论 |
| problem_id | BIGINT | NOT NULL | 题目ID |
| user_id | BIGINT | NOT NULL | 发布者ID |
| parent_id | BIGINT | DEFAULT 0 | 父级ID |
| reply_to_user_id | BIGINT | — | 被回复用户ID |
| title | VARCHAR(255) | — | 标题 |
| content | TEXT | — | 正文 |
| like_count | INT | DEFAULT 0 | 点赞数 |
| comment_count | INT | DEFAULT 0 | 评论数 |
| view_count | INT | DEFAULT 0 | 浏览量 |
| status | INT | DEFAULT 1 | 状态: 0-隐藏, 1-正常, 2-置顶 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

> 索引: `idx_problem_id`, `idx_user_id`

---

### 3.4 判题服务 (oj_judge_db)

#### 3.4.1 提交记录表 `submissions`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 提交ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| problem_id | INT | NOT NULL | 题目ID |
| contest_id | INT | — | 竞赛ID (普通提交为空) |
| code | TEXT | NOT NULL | 提交代码 |
| language | VARCHAR(50) | NOT NULL | 编程语言 |
| status | VARCHAR(50) | NOT NULL | 判题状态: Pending/Accepted/Wrong Answer/Compile Error/Time Limit Exceeded/Memory Limit Exceeded/Runtime Error/System Error |
| runtime_ms | INT | — | 运行时间(毫秒) |
| memory_kb | INT | — | 内存使用(KB) |
| test_cases_passed | INT | — | 通过用例数 |
| test_cases_total | INT | — | 总用例数 |
| error_info | TEXT | — | 错误信息 |
| ip_address | VARCHAR(50) | — | 提交IP |
| submit_time | DATETIME | NOT NULL | 提交时间 |

> 索引: `idx_user_id`, `idx_problem_id`, `idx_contest_id`, `idx_status`, `idx_submit_time`

#### 3.4.2 恶意代码日志表 `malicious_code_log`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 日志ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| code | TEXT | NOT NULL | 检测到的代码 |
| language | VARCHAR(50) | NOT NULL | 编程语言 |
| detection_type | VARCHAR(100) | NOT NULL | 检测类型 |
| risk_level | VARCHAR(20) | NOT NULL | 风险等级: LOW/MEDIUM/HIGH/CRITICAL |
| description | TEXT | — | 检测描述 |
| problem_id | INT | — | 相关题目ID |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 检测时间 |

> 索引: `idx_user_id`, `idx_language`, `idx_create_time`, `idx_risk_level`

---

### 3.5 Redis 数据结构设计

| Key Pattern | 类型 | TTL | 说明 |
|-------------|------|-----|------|
| `contest:rank:{contestId}` | ZSet | 比赛结束后24h | 实时排行榜 (score → userId) |
| `contest:{cid}:user:{uid}:solved_count` | String | 比赛期间 | 用户解题数 |
| `contest:ac:{cid}:{uid}:{pid}` | String | 比赛期间 | 用户AC标记 |
| `contest:lock:{cid}:{uid}:{pid}` | String | 比赛结束 | 题目锁定标记 |
| `contest:hack:{cid}:{hacker}:{target}:{pid}` | String | 比赛期间 | Hack去重标记 |
| `contest:hack:pending:{hackId}` | String | 5min | Hack判题令牌 |
| `judge:rate-limit:{userId}:{problemId}` | String | 10s | 判题限流 |
| `ai:session:{sessionId}` | String | 1h | AI对话会话上下文 |
| `ai:memory:{userId}` | List | 永久 | AI长期记忆向量索引 |
| `knowledge:embedding:{docId}` | Hash | 永久 | 知识库文档向量存储 |

---

## 4. 前端设计

### 4.1 用户端 (vue-project1)

#### 4.1.1 页面结构

```
├── /                         # 首页 (题目列表、推荐)
├── /login                    # 登录页
├── /register                 # 注册页
├── /problems                 # 题库页 (筛选/排序/分页)
├── /problem/:id              # 题目详情页 (描述+代码编辑器+提交)
├── /problem/:id/solutions    # 题解列表页
├── /groups                   # 题单列表页
├── /group/:id                # 题单详情页
├── /contests                 # 竞赛列表页
├── /contest/:id              # 竞赛详情页 (题目列表+排名)
├── /contest/:id/problem/:pid # 竞赛做题页 (含Hack操作)
├── /status                   # 提交记录页
├── /profile                  # 个人中心 (统计/提交/收藏)
├── /ai-assistant             # AI助手对话页 (流式对话)
└── /ai-learning              # 学情分析页
```

#### 4.1.2 核心组件

| 组件 | 功能 |
|------|------|
| MonacoEditor | 代码编辑器 (支持多语言语法高亮、代码提示) |
| AiChatPanel | AI 流式对话面板 (SSE) |
| ProblemCard | 题目卡片组件 |
| CodeSubmitDialog | 代码提交弹窗 |
| RankingTable | 竞赛排名表 |
| HackPanel | Hack 面板 (锁定/查看代码/提交Hack) |
| TimerCountdown | 竞赛倒计时 |

#### 4.1.3 API 封装 (src/api/)

| 文件 | 对应后端服务 | 说明 |
|------|-------------|------|
| auth.js | user-service | 登录/注册/JWT刷新 |
| user.js | user-service | 用户信息/签到/统计 |
| problems.js | problem-service | 题目列表/详情/搜索 |
| groups.js | problem-service | 题单列表/详情 |
| contests.js | contest-service | 竞赛/排名/Hack |
| submissions.js | judge-service | 提交/判题结果 |
| solutions.js | contest-service | 题解/评论 |
| ai.js | ai-service | AI对话/代码审查/学情分析 |
| knowledge.js | problem-service | 知识库检索 |

### 4.2 管理端 (vue-Element)

#### 4.2.1 页面结构

```
├── /admin/login              # 管理员登录
├── /admin/dashboard          # 工作台 (统计图表)
├── /admin/problems           # 题目管理 (CRUD + 测试用例编辑)
├── /admin/problems/create    # 创建/编辑题目
├── /admin/problem-types      # 题目类型管理
├── /admin/groups             # 题单管理
├── /admin/contests           # 竞赛管理
├── /admin/contests/create    # 创建竞赛
├── /admin/users              # 用户管理
├── /admin/submissions        # 提交记录管理
├── /admin/knowledge          # 知识库文档管理 (上传/索引/删除)
├── /admin/reports            # 数据报告
└── /admin/hack-records       # Hack记录查阅
```

#### 4.2.2 核心功能

| 功能 | 说明 |
|------|------|
| 题目CRUD | 创建/编辑题目，Markdown编辑器编辑题目描述，管理测试用例，上传Validator和标准解答 |
| 题单管理 | 创建题单，拖拽排序题目，设置难度范围和标签 |
| 竞赛管理 | 创建比赛，选择题目，设置时间，查看参赛情况和排名 |
| 知识库管理 | 上传PDF/Markdown文档 → 后端自动切片向量化 → 触发RAG索引构建 |
| 数据报告 | ECharts 可视化展示用户增长、提交趋势、题目通过率 |
| 用户管理 | 查看/禁用用户，修改角色，重置密码 |

---

## 5. AI 智能辅助设计

### 5.1 整体架构

```
用户提问 → AgentController / AiJudgeController
                │
    ┌───────────┼───────────┐
    ▼           ▼           ▼
  Agent        RAG        AI Judge
  Service      Service     Service
    │           │           │
    ▼           ▼           ▼
┌───────────────────────────────────────┐
│         LangChain4j / LangGraph4j     │
│  ┌─────────────────────────────────┐  │
│  │        Agent Orchestrator        │  │
│  │  ┌───────┐ ┌───────┐ ┌───────┐  │  │
│  │  │Router │ │Solution│ │CodeJdg│  │  │
│  │  │ Agent │ │ Agent  │ │ Agent │  │  │
│  │  └───────┘ └───────┘ └───────┘  │  │
│  │  ┌───────┐ ┌───────┐             │  │
│  │  │Learn  │ │Knowl  │             │  │
│  │  │ Agent │ │ Agent │             │  │
│  │  └───────┘ └───────┘             │  │
│  └─────────────────────────────────┘  │
│  ┌─────────────────────────────────┐  │
│  │         RAG Pipeline             │  │
│  │  Embedding → VectorStore → Retrv│  │
│  └─────────────────────────────────┘  │
└───────────────────────────────────────┘
                │
                ▼
      SiliconFlow Model API
   (Qwen3-Coder-30B / bge-large-zh)
```

### 5.2 Agent 设计

| Agent | 职责 | 触发场景 |
|-------|------|----------|
| RouterAgent | 意图识别与路由分发 | 用户问题 → 判断应调用哪个专业Agent |
| SolutionAgent | 题解生成 | "生成这道题的解题思路" |
| CodeJudgeAgent | AI代码审查 | "帮我检查这段代码" / 分析错误 |
| LearningAgent | 学情分析 | "分析我的学习情况" |
| KnowledgeAgent | 知识检索 | "什么是动态规划?" / "如何优化DFS?" |
| SupervisorAgent | 多Agent协调 | LangGraph4j StateGraph 调度 |

### 5.3 RAG 知识库

```
文档上传流程:
  PDF/MD文档 → DocumentSplitter(分片) → EmbeddingModel(向量化)
  → InMemoryEmbeddingStore / Redis Vector Store → 语义检索

检索流程:
  用户问题 → QueryEmbedding → 向量相似度搜索 → Top-K相关片段
  → 拼入Prompt上下文 → LLM生成回答
```

**文档类型支持**: PDF, Markdown, TXT  
**分片策略**: 段落级分片，最大512 tokens，重叠64 tokens  
**向量模型**: BAAI/bge-large-zh-v1.5 (1024维)  
**存储方案**: 初期 InMemoryEmbeddingStore，后续迁移 Redis Stack (RediSearch)

### 5.4 对话记忆管理

| 组件 | 存储 | 说明 |
|------|------|------|
| DialogMemoryService | Redis | 短期对话上下文 (最近10轮)，TTL=1h |
| LongTermMemoryService | Redis Vector | 长期记忆: 用户知识薄弱点、常犯错误 |
| RedisChatMemoryStore | Redis | LangChain4j ChatMemory 实现 |

---

## 6. 核心业务流程

### 6.1 判题流程

```
用户提交代码 → Gateway JWT鉴权 → JudgeController
  │
  ├─ Redis Lua 限流检查 (每人每题10s内仅1次)
  │
  ├─ 保存 submission 记录 (status=Pending)
  │
  ├─ 发送 RocketMQ 消息 (JudgeTaskTopic)
  │     │
  │     ▼
  │  JudgeTaskConsumer (judge-service)
  │     ├─ 通过 Feign 拉取 test_cases
  │     ├─ 遍历测试用例:
  │     │   └─ Judge0.submitAndWait(code, lang, stdin, expectedOutput)
  │     │        ├─ Compile Error → 中断
  │     │        ├─ Runtime Error / TLE / MLE → 记录
  │     │        └─ 输出比对 (严格比对, 忽略末尾空白)
  │     ├─ 汇总结果 → 发送 DatabaseUpdateTopic
  │     │
  │     ▼
  │  DatabaseUpdateConsumer (judge-service)
  │     ├─ 更新 submission 记录
  │     └─ 通过 WebSocket 推送结果给用户
  │
  └─ 返回 submissionId 给前端 (前端轮询或WebSocket获取结果)
```

### 6.2 竞赛排名流程

```
比赛开始:
  1. 初始化 Redis ZSet: contest:rank:{cid} = {}
  2. 用户提交通过 → ZINCRBY contest:rank:{cid} score userId
  3. 前端定时轮询 /contest/{cid}/rank 获取排名

比赛结束:
  1. ZREVRANGE 获取最终排名
  2. 持久化到 contest_participants 表
  3. TTL=24h 后清除 Redis 排名数据
```

### 6.3 Hack 流程

```
AC选手锁题 → 查看对手AC代码 → 构造Hack数据 → 提交Hack
  │
  ▼
[judge-service] Hack判题流程:
  ① SHA-256 校验 Validator 源码 → 复用或重新编译
  ② Judge0 沙箱运行 C++ Validator (exit 0 = 数据合法)
  ③ 目标代码 + Hack数据 → Judge0 运行 (记录 targetStatus)
  ④ 标准解答 + Hack数据 → Judge0 运行 (记录 referenceOutput)
  ⑤ 判定: targetStatus != AC && referenceStatus == AC → HackSuccess
  ⑥ MQ 回传结果
  │
  ▼
[contest-service] 处理结果:
  ⑦ 写 hack_records 记录
  ⑧ Lua 原子更新: hacker加分 + hacked扣分 + 清除AC标记
  ⑨ Feign 通知 problem-service 添加测试用例
  ⑩ WebSocket 广播 Hack 结果
```

### 6.4 JWT 鉴权流程

```
客户端请求 → Gateway AuthGlobalFilter
  ├─ 白名单路径放行 (/api/user/login, /api/user/register, /swagger*, /v3/api-docs*)
  ├─ 其他路径: 解析 Authorization Header
  │   ├─ 无 Token → 401
  │   └─ 有 Token → JwtUtil.parseJWT()
  │       ├─ 解析成功 → 设置 X-User-Id / X-User-Role 请求头 → 路由
  │       └─ 过期/无效 → 401
  │
  ▼
各微服务: UserInfoInterceptor
  └─ 从请求头读取 X-User-Id / X-User-Role → 存入 BaseContext (ThreadLocal)
```

---

## 7. API 设计概要

### 7.1 Gateway 路由映射

| 路径前缀 | 目标服务 | 说明 |
|----------|----------|------|
| `/api/user/**`, `/api/login/**` | oj-user-service | 用户认证与信息 |
| `/api/problem/**`, `/api/knowledge/**` | oj-problem-service | 题目与知识库 |
| `/api/contest/**`, `/api/group/**`, `/api/comment/**` | oj-contest-service | 竞赛与题解 |
| `/api/judge/**`, `/api/submission/**` | oj-judge-service | 判题与提交 |
| `/api/ai/**`, `/api/agent/**` | oj-ai-service | AI 智能辅助 |
| `/internal/**` | 各服务 | Feign 内部调用 (不经 Gateway) |

### 7.2 用户端核心 API

#### 用户认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/login` | 用户名+密码登录，返回JWT |
| POST | `/api/user/register` | 用户注册 |
| GET | `/api/user/profile` | 获取当前用户信息 |
| PUT | `/api/user/profile` | 更新用户信息 |

#### 题目
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/problem/list` | 分页查询题目列表 (支持筛选) |
| GET | `/api/problem/{id}` | 获取题目详情 |
| GET | `/api/group/list` | 题单列表 |
| GET | `/api/group/{id}` | 题单详情含题目列表 |

#### 判题
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/judge/submit` | 提交代码判题 |
| GET | `/api/submission/{id}` | 查询判题结果 |
| GET | `/api/submission/my-list` | 我的提交记录 |

#### 竞赛
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/contest/list` | 竞赛列表 |
| GET | `/api/contest/{id}` | 竞赛详情 |
| GET | `/api/contest/{id}/rank` | 实时排名 |
| POST | `/api/contest/{id}/register` | 报名参赛 |
| POST | `/api/contest/{id}/problem/{pid}/lock` | 锁定题目 (Hack前置) |
| GET | `/api/contest/{id}/problem/{pid}/ac-submissions` | 获取AC代码列表 |
| POST | `/api/contest/{id}/hack` | 提交Hack |

#### AI 助手
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/agent/chat` | AI 智能对话 |
| POST | `/api/agent/chat/stream` | AI 流式对话 (SSE) |
| GET | `/api/agent/solution/{problemId}` | 生成题解 |
| POST | `/api/agent/judge` | AI 代码审查 |
| GET | `/api/agent/learning/{userId}` | 学情分析 |
| POST | `/api/ai/chat/submit` | 知识库问答 |

### 7.3 管理端核心 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/problem/create` | 创建题目 |
| PUT | `/api/admin/problem/{id}` | 编辑题目 |
| DELETE | `/api/admin/problem/{id}` | 删除题目 |
| POST | `/api/admin/test-case/add` | 添加测试用例 |
| POST | `/api/admin/contest/create` | 创建竞赛 |
| POST | `/api/admin/group/create` | 创建题单 |
| GET | `/api/admin/users` | 用户列表 |
| PUT | `/api/admin/user/{id}/status` | 禁用/启用用户 |
| POST | `/api/admin/knowledge/upload` | 上传文档到知识库 |
| DELETE | `/api/admin/knowledge/{id}` | 删除知识库文档 |

---

## 8. 消息队列设计

### 8.1 Topic 定义

| Topic | Consumer Group | 说明 |
|-------|---------------|------|
| `judge-task-topic` | `judge-task-consumer-group` | 判题任务分发 |
| `judge-task-dead-letter-topic` | `judge-task-dead-letter-consumer-group` | 判题死信兜底 |
| `judge-result-topic` | `judge-result-consumer-group` | 判题结果回写 |
| `hack-task-topic` | `hack-task-consumer-group` | Hack 任务分发 |
| `hack-task-dead-letter-topic` | `hack-task-dead-letter-consumer-group` | Hack 死信兜底 |
| `hack-result-topic` | `hack-result-consumer-group` | Hack 结果回写 |

### 8.2 消息结构

#### JudgeTaskMessage
```json
{
  "submissionId": 12345,
  "userId": 100,
  "problemId": 42,
  "contestId": null,
  "code": "print('hello')",
  "language": "Python",
  "timeLimitMs": 1000,
  "memoryLimitMb": 256
}
```

#### HackTaskMessage (精简: 只传ID和路径，代码按需Feign拉取)
```json
{
  "hackId": 200,
  "contestId": 5,
  "problemId": 42,
  "hackerId": 100,
  "targetUserId": 101,
  "targetSubmissionId": 9001,
  "targetLanguage": "C++",
  "validatorPath": "hack-data/contest-5/problem-42/validator.cpp",
  "validatorExePath": "hack-data/contest-5/problem-42/validator.exe",
  "validatorSrcHash": "abc123...",
  "referencePath": "hack-data/contest-5/problem-42/reference.cpp",
  "referenceLanguage": "C++",
  "hackInput": "1 2",
  "timeLimitMs": 1000,
  "memoryLimitMb": 256
}
```

---

## 9. 部署架构

### 9.1 基础设施清单

| 组件 | 地址 | 端口 | 说明 |
|------|------|------|------|
| MySQL | localhost | 3306 | 主数据库 (3个库) |
| Redis | 192.168.141.128 | 6378 | 缓存 / 限流 / 排行榜 |
| RocketMQ | 192.168.141.128 | 9876 | 消息队列 |
| Judge0 | 192.168.141.128 | 2358 | 代码沙箱 |
| Skywalking OAP | 192.168.141.128 | 11800 | 链路追踪 |
| Nacos | 192.168.141.129 | 8848 | 服务注册 + 配置中心 |
| Sentinel | 192.168.141.129 | 8858 | 流量控制 / 熔断 |
| AI API | api.siliconflow.cn | — | LLM 推理服务 |

### 9.2 服务部署

| 服务 | 端口 | 实例数 | 资源需求 |
|------|:---:|:---:|----------|
| oj-gateway | 8080 | 2 | 1C/512M |
| oj-user-service | 8081 | 1 | 1C/512M |
| oj-problem-service | 8082 | 1 | 1C/512M |
| oj-contest-service | 8083 | 1 | 1C/512M |
| oj-judge-service | 8084 | 2+ | 2C/1G (高负载) |
| oj-ai-service | 8086 | 1 | 1C/1G |
| vue-project1 (用户端) | 5174 | 1 | Nginx 静态托管 |
| vue-Element (管理端) | 5173 | 1 | Nginx 静态托管 |

---

## 10. 架构约定

### 10.1 Controller 分层

```
controller/
├── admin/     # 管理端接口 (@AdminRoleCheck)
├── user/      # 用户端接口 (@UserRoleCheck)
└── internal/  # 内部Feign接口 (不经Gateway鉴权, 路径 /internal/**)
```

### 10.2 服务间通信

- **同步**: OpenFeign + LoadBalancer (接口定义在 `oj-common-api`)
- **异步**: RocketMQ 6个 Topic
- **实时推送**: WebSocket (oj-judge-service, 路径 `/ws/{sid}`)

### 10.3 用户信息传播链

```
Gateway AuthGlobalFilter
  → 解析 JWT → X-User-Id / X-User-Role 请求头
    → 各服务 UserInfoInterceptor
      → BaseContext.setCurrentUser()
        → FeignRequestInterceptor (传播到下游Feign调用)
```

### 10.4 统一返回体

```java
// 普通响应
Result<T> { int code; String msg; T data; }

// 分页响应
PageResult { int total; int pageNo; int pageSize; List<T> records; }
```

---

## 11. 版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | 2026-05-28 | 初始版本：系统架构、数据库设计、前端设计、AI助手、业务流程 |
