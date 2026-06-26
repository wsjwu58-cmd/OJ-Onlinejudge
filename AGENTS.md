# OJ-Microservice 开发规范

## 项目概述
在线判题系统

## 目录结构
两个前端

用户端在vue-project1

管理端在vue-Element中

- oj-*/ — 微服务模块 (gateway / user / problem / contest / judge)
- oj-ai-python/ — Python AI 智能服务（替代原 Java oj-ai-service）

## 核心原则
1. 根据文档进行功能实现
2. 配置一致：端口、数据库、中间件地址等均与原项目保持一致
3. 功能完整：Python AI 服务必须完全覆盖原 Java 版全部功能，不得删减

## 技术栈
后端：Spring Boot 3.4.2 + Spring Cloud + Nacos + Sentinel + OpenFeign + MyBatis-Plus + RocketMQ + JWT + Redis
AI 服务：Python 3.12 + FastAPI + LangChain v1.0 + LangGraph v1.0 + LangChain OpenAI + FAISS + Redis + SSE
前端：Vue 3 + Vite + Element Plus + Pinia + Axios + Monaco Editor
数据库：MySQL 8.0 + Redis 7.0
LLM：SiliconFlow API（Qwen3-Coder-30B-A3B-Instruct / BAAI/bge-large-zh-v1.5）
中间件：Nacos/Sentinel(192.168.141.129) | RocketMQ/Judge0/Skywalking(192.168.141.128)

## 开发约定
### 通用约定
- 同步调用用 Feign（定义在 oj-common-api），异步用 RocketMQ
- Controller 分 admin/user/internal 三层，internal 供 Feign 内部调用
- 统一返回体 Result<T>，Gateway 层统一 JWT 鉴权，X-User-Id/X-User-Role 头传播用户信息
- 前端 API 统一指向 Gateway，用户端 /api/user，管理端 /api/admin
- 各服务配置由 Nacos 统一管理，本地仅保留 bootstrap.yml
- 禁止引入 Lombok 以外的注解处理器

### Python AI 服务约定
- 端口 **8086**，注册到 Nacos (`192.168.141.129:8848`)，通过 Gateway 与 Java 微服务通信
- 框架：FastAPI + `uvicorn`，异步优先（httpx 调用 Java 服务，redis-py 异步操作）
- LLM 调用：langchain-openai 兼容 SiliconFlow API，流式用 `astream_events()` → SSE
- Agent 编排：langgraph StateGraph（Router → Solution/Code/Learning/Knowledge → Supervisor）
- RAG Pipeline：pymupdf 解析 PDF → RecursiveCharacterTextSplitter(350/50) → FAISS 内存索引 + Redis 持久化快照 → 检索 top-K (threshold=0.5)
- 向量模型：`OpenAIEmbeddings(model="BAAI/bge-large-zh-v1.5")`，InMemory 为主，Redis 备份
- 流式响应：保持 token 提交→SSE 轮询模式，token 存 Redis (`ai:token:{token}`，TTL 5min)
- 会话记忆：Redis 存储（dialog 1h，chat memory 7d，long-term 永久）
- MCP 工具：langchain-mcp-adapters 桥接，Bing Search 默认关闭
- 依赖注入：FastAPI `Depends()` 管理 LLM/Embedding/Redis 单例
- 核心代码加简要中文注释标明功能，不冗余

### API 路由（Python AI 服务）

**用户端**（前端不变，路径兼容原 Java 版）：

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/user/ai/judge/submit` | AI判题提交→返回token |
| `GET` | `/user/ai/judge/stream/{token}` | SSE流式判题结果 |
| `POST` | `/user/ai/syntax-check/submit` | 语法检查提交 |
| `GET` | `/user/ai/syntax-check/stream/{token}` | SSE语法检查结果 |
| `POST` | `/user/ai/analyze-error/submit` | 错误分析提交 |
| `GET` | `/user/ai/analyze-error/stream/{token}` | SSE错误分析结果 |
| `POST` | `/user/ai/chat/submit` | RAG对话提交 |
| `GET` | `/user/ai/chat/stream/{token}` | SSE对话流 |
| `POST` | `/user/ai/hint/submit` | 编程提示提交 |
| `GET` | `/user/ai/hint/stream/{token}` | SSE提示流 |
| `POST` | `/user/agent/chat` | Agent对话(同步) |
| `POST` | `/user/agent/chat/stream` | Agent流式对话 |

**管理端**（知识库管理，替代原 oj-problem-service 的 KnowledgeController）：

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/admin/knowledge/import/pdf` | 上传PDF导入知识库 |
| `POST` | `/admin/knowledge/import/dir` | 批量导入目录 |
| `DELETE` | `/admin/knowledge/clear` | 清空知识库 |
| `GET` | `/admin/knowledge/stats` | 知识库统计 |

### Redis Key 规范（Python AI 服务）

| Key 模式 | 类型 | 内容 | TTL |
|----------|------|------|-----|
| `ai:dialog:{uid}:{pid}` | List(JSON) | 对话历史 [{role,content,time}] | 1h |
| `agent:chat:{memory_id}` | Hash | Agent 聊天记忆 | 7d |
| `agent:user:{uid}:history` | List | 最近50条交互历史 | 永久 |
| `agent:user:{uid}:profile` | Hash | 用户画像(偏好/弱点) | 永久 |
| `agent:user:{uid}:session` | Hash | 会话摘要 | 24h |
| `ai:token:{token}` | String(JSON) | SSE 提交令牌参数 | 5min |
| `ai:vector:snapshot` | String(binary) | FAISS 索引持久化快照 | 永久 |

