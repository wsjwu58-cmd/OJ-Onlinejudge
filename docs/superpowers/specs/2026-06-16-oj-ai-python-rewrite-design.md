# OJ AI 模块 Python 重写设计文档

> **日期**: 2026-06-16
> **状态**: 设计中
> **目标**: 用 Python (FastAPI + LangChain v1.0 + LangGraph v1.0) 重写 oj-ai-service，完全替代现有 Java 实现

---

## 1. 背景与目标

### 1.1 当前架构回顾

现有 `oj-ai-service` 基于 Spring Boot 3.4.2 + LangChain4j 1.12.2 + LangGraph4j，提供以下能力：

| 功能模块 | 当前实现 | 问题 |
|----------|---------|------|
| **LLM 调用** | SiliconFlow API (Qwen3-Coder-30B-A3B-Instruct) | 正常运行 |
| **向量检索** | InMemoryEmbeddingStore（启动时加载，无持久化） | 重启丢失数据 |
| **Agent 编排** | AiServices + LangGraph4j 多Agent图 | 复杂但可用 |
| **知识导入** | PDF → ApachePdfBoxDocumentParser → 分块 → 入库 | 仅 oj-problem-service 支持 |
| **会话记忆** | Redis（dialog 1h, chat 7d, long-term 50条） | 正常运行 |
| **流式响应** | SSE 提交令牌→轮询模式 | 前端已适配 |
| **MCP 工具** | Bing Search（默认关闭） | 功能备用 |

### 1.2 重写动机

- LangChain4j Java 生态更新滞后于 Python LangChain 生态
- Python 在 AI/LLM 领域具有更丰富的库和多模型支持
- LangChain v1.0 和 LangGraph v1.0 提供了更稳定的 API 面
- 便于未来接入更多模型、本地部署（Ollama）、Agent 扩展

### 1.3 范围

- **完全替代** oj-ai-service，含知识导入功能（原 oj-problem-service 的知识导入一并迁移）
- 项目内新增 `oj-ai-python/` 目录，复用现有 docker-compose
- 注册到 Nacos，通过 Gateway 与存量 Java 微服务通信
- 前端接口保持完全兼容，不做任何改动

---

## 2. 项目结构

```
oj-ai-python/
├── pyproject.toml
├── Dockerfile
├── .env.example
├── src/
│   ├── main.py                   # FastAPI 入口，生命周期管理
│   ├── config.py                 # 配置加载（环境变量 + Nacos）
│   ├── deps.py                   # 依赖注入（LLM/Embedding/Redis 单例）
│   ├── api/
│   │   ├── router.py             # 路由汇总
│   │   ├── user/                 # 用户端接口
│   │   │   ├── agent.py          # /user/agent/chat, /user/agent/chat/stream
│   │   │   └── ai_judge.py       # /user/ai/* (judge/syntax-check/analyze-error/chat/hint)
│   │   └── admin/                # 管理端接口
│   │       └── knowledge.py      # /admin/knowledge/* (import/clear/stats)
│   ├── core/
│   │   ├── llm.py                # ChatModel / EmbeddingModel 工厂
│   │   ├── rag/
│   │   │   ├── retriever.py      # 向量检索器
│   │   │   ├── importer.py       # PDF/TXT 知识导入 pipeline
│   │   │   └── store.py          # FAISS 内存索引 + Redis 持久化
│   │   ├── memory/
│   │   │   ├── dialog.py         # 对话记忆（ai:dialog:{uid}:{pid}，1h TTL）
│   │   │   ├── chat.py           # Agent 聊天记忆（agent:chat:{id}，7d TTL）
│   │   │   └── user_context.py   # 长期用户画像（agent:user:{uid}:*）
│   │   └── mcp/
│   │       └── client.py         # MCP 客户端连接器（Bing Search 等）
│   ├── agent/
│   │   ├── graph.py              # LangGraph StateGraph 构建与编译
│   │   ├── state.py              # AgentState TypedDict 定义
│   │   ├── nodes/
│   │   │   ├── router.py         # 意图路由节点
│   │   │   ├── solution.py       # 解题 Agent 节点
│   │   │   ├── code_judge.py     # 判题 Agent 节点
│   │   │   ├── learning.py       # 学习分析 Agent 节点
│   │   │   ├── knowledge.py      # 知识检索 Agent 节点
│   │   │   └── supervisor.py     # 监督汇总 Agent 节点
│   │   └── tools/
│   │       ├── solution.py       # get_problem_detail, get_test_cases
│   │       ├── judge.py          # analyze_code, check_syntax
│   │       ├── learning.py       # get_submission_stats, get_user_progress
│   │       └── retrieval.py      # search_knowledge, search_problem_knowledge
│   └── client/                   # Java 微服务 HTTP 客户端（通过 Gateway）
│       ├── gateway.py            # 基础请求封装（认证头 X-User-Id/X-User-Role）
│       ├── problem.py            # 题目/测试用例查询
│       ├── judge.py              # 提交记录/AC代码查询
│       └── user.py               # 用户信息服务
└── tests/
    ├── test_rag.py
    ├── test_agent.py
    └── test_api.py
```

---

## 3. 核心依赖

| Python 包 | 版本 | 用途 | 替代的 Java 依赖 |
|-----------|------|------|-----------------|
| `fastapi` | >=0.115 | Web 框架 | Spring Boot |
| `uvicorn[standard]` | >=0.34 | ASGI 服务器 | 内嵌 Tomcat |
| `sse-starlette` | >=2.1 | SSE 流式响应 | spring-boot-starter-webflux |
| `langchain` | >=1.0.0 | LLM/RAG 框架 | langchain4j |
| `langgraph` | >=1.0.0 | Agent 编排 | langgraph4j-core |
| `langchain-openai` | >=0.3 | OpenAI 兼容 LLM（SiliconFlow） | langchain4j-open-ai |
| `langchain-community` | >=0.3 | TextSplitter, PDFLoader | langchain4j-easy-rag |
| `langchain-mcp-adapters` | >=0.1 | MCP 工具桥接 | langchain4j-mcp |
| `redis` | >=5.2 | Redis 异步客户端 | spring-boot-starter-data-redis |
| `httpx` | >=0.28 | 异步 HTTP（调 Java 服务） | OpenFeign |
| `pymupdf` | >=1.25 | PDF 文本提取 | ApachePdfBoxDocumentParser |
| `nacos-sdk-python` | >=2.0 | Nacos 注册/配置 | spring-cloud-starter-alibaba-nacos |
| `pydantic` | >=2.10 | 数据校验 | Jakarta Validation |
| `pydantic-settings` | >=2.7 | 配置管理 | @ConfigurationProperties |

---

## 4. 架构与数据流

### 4.1 拓扑

```
用户浏览器 ──SSE──▶ Gateway (:8080) ──HTTP──▶ oj-ai-python (:8086)
                                                    │
                                    ┌───────────────┼───────────────┐
                                    ▼               ▼               ▼
                              Redis (:6378)  SiliconFlow API   Java 微服务
                              - dialog        - LLM chat       (通过Gateway)
                              - chat memory   - embedding      - User (:8081)
                              - vec snapshot                  - Problem (:8082)
                              - SSE tokens                    - Contest (:8083)
                                                              - Judge (:8084)
```

### 4.2 三条核心数据流

**判题流**：
```
POST /user/ai/judge/submit → 生成token存Redis → 返回{token}
GET  /user/ai/judge/stream/{token} → 读token获取参数 →
   1. httpx → Gateway → Problem服务获取题目详情/测试用例
   2. RAG Retriever检索相关知识
   3. 构建Prompt → langchain-openai异步流式调用SiliconFlow
   4. astream_events() → SSE逐token推送
   5. 结束时dialog memory写入Redis
```

**RAG 对话流**：
```
POST /user/ai/chat/submit → 生成token → 返回{token}
GET  /user/ai/chat/stream/{token} →
   1. 加载对话历史 (ai:dialog:{uid}:{pid})
   2. query嵌入 → FAISS.similarity_search(k=3, threshold=0.5)
   3. 知识注入system prompt
   4. LLM流式生成 → SSE
   5. 保存对话到Redis
```

**Agent 流**：
```
POST /user/agent/chat →
   1. LangGraph StateGraph启动
   2. RouterAgent分类意图 → solution/code/learning/knowledge
   3. 对应Agent调用@tool → 获取外部数据
   4. SupervisorAgent汇总 → 生成最终响应
   5. 流式返回前端
```

---

## 5. RAG Pipeline

### 5.1 知识导入流程

```
PDF上传 → pymupdf提取文本
       → RecursiveCharacterTextSplitter(chunk_size=350, chunk_overlap=50)
       → SiliconFlowEmbeddings嵌入
       → FAISS.add_documents() + Redis持久化备份
```

### 5.2 检索流程

```
用户输入 → SiliconFlowEmbeddings嵌入
        → FAISS.similarity_search(k=3, score_threshold=0.5)
        → 拼接检索结果到system prompt
        → LLM生成增强回答
```

### 5.3 向量存储

- **主存储**: FAISS `InMemoryDocstore` (进程内, 毫秒级检索)
- **持久化**: Redis `SET ai:vector:snapshot` 保存 FAISS 索引二进制 + 文档元数据
- **恢复**: 启动时尝试从 Redis 加载，失败则从空库开始
- **管理 API**:
  - `POST /admin/knowledge/import/pdf` — 上传单个 PDF
  - `POST /admin/knowledge/import/dir` — 批量导入目录
  - `DELETE /admin/knowledge/clear` — 清空向量库
  - `GET /admin/knowledge/stats` — 统计信息

---

## 6. Agent 编排

### 6.1 LangGraph 图结构

```
        ┌──────────┐
        │  START   │
        └────┬─────┘
             │
        ┌────▼─────┐
        │  Router  │ (意图分类: solution/code/learning/knowledge)
        └─┬──┬──┬──┘
     ┌────┘  │  │  └────┐
     ▼       ▼  ▼       ▼
  Solution Code Learning Knowledge
     │       │   │        │
     └───┬───┴───┴────────┘
         │
    ┌────▼──────┐
    │ Supervisor│ (汇总结果, 生成最终响应)
    └────┬──────┘
         │
    ┌────▼─────┐
    │   END    │
    └──────────┘
```

### 6.2 AgentState 定义

```python
class AgentState(TypedDict):
    user_id: str
    session_id: str
    task: str              # 用户原始输入
    context: str           # 长期记忆上下文
    problem_id: str | None
    current_agent: str
    routing_result: str    # Router 分类结果
    solution_result: str
    code_result: str
    learning_result: str
    knowledge_result: str
    iteration_count: int
    confidence: float
    final_response: str
```

### 6.3 工具列表

| 工具函数 | 数据来源 | 功能 |
|----------|---------|------|
| `get_problem_detail(id)` | httpx → Problem服务 | 获取题目详情 |
| `get_test_cases(pid)` | httpx → Problem服务 | 获取测试用例 |
| `get_submission_stats(uid)` | httpx → Judge服务 | 用户提交统计 |
| `get_ac_submissions(pid)` | httpx → Judge服务 | 已AC提交代码 |
| `get_user_profile(uid)` | httpx → User服务 | 用户信息 |
| `search_knowledge(query)` | FAISS检索 | 知识库检索 |
| MCP工具集 | MCP Server | 外部工具(如Bing) |

---

## 7. API 接口

### 7.1 用户端（与现有前端完全兼容）

| 方法 | 路径 | 功能 | Token模式 |
|------|------|------|----------|
| `POST` | `/user/ai/judge/submit` | AI判题提交 | 返回{token} |
| `GET` | `/user/ai/judge/stream/{token}` | SSE判题流 | 5min TTL |
| `POST` | `/user/ai/syntax-check/submit` | 语法检查提交 | 返回{token} |
| `GET` | `/user/ai/syntax-check/stream/{token}` | SSE语法检查流 | 5min TTL |
| `POST` | `/user/ai/analyze-error/submit` | 错误分析提交 | 返回{token} |
| `GET` | `/user/ai/analyze-error/stream/{token}` | SSE错误分析流 | 5min TTL |
| `POST` | `/user/ai/chat/submit` | RAG对话提交 | 返回{token} |
| `GET` | `/user/ai/chat/stream/{token}` | SSE对话流 | 5min TTL |
| `POST` | `/user/ai/hint/submit` | 编程提示提交 | 返回{token} |
| `GET` | `/user/ai/hint/stream/{token}` | SSE提示流 | 5min TTL |
| `POST` | `/user/agent/chat` | Agent对话(同步) | 无 |
| `POST` | `/user/agent/chat/stream` | Agent流式对话 | 直接SSE |

### 7.2 管理端

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/admin/knowledge/import/pdf` | 上传PDF导入知识库 |
| `POST` | `/admin/knowledge/import/dir` | 批量导入目录 |
| `DELETE` | `/admin/knowledge/clear` | 清空知识库 |
| `GET` | `/admin/knowledge/stats` | 知识库统计 |

### 7.3 Token 轮询模式

```python
# submit 端：生成token，参数存Redis
token = uuid4().hex
await redis.setex(f"ai:token:{token}", 300, json.dumps(request_data))
return {"code": 200, "data": {"token": token}}

# stream 端：读参数，流式推送
request_data = json.loads(await redis.get(f"ai:token:{token}"))
async for chunk in stream_llm_response(request_data):
    yield {"event": "message", "data": json.dumps(chunk)}
```

---

## 8. Redis Key 设计

| Key 模式 | 类型 | 内容 | TTL |
|----------|------|------|-----|
| `ai:dialog:{user_id}:{problem_id}` | List(JSON) | 对话历史 [{role, content, time}] | 1h |
| `agent:chat:{memory_id}` | Hash | Agent聊天记忆 | 7d |
| `agent:user:{user_id}:history` | List | 最近50条交互历史 | 永久 |
| `agent:user:{user_id}:profile` | Hash | 用户画像(偏好/弱点) | 永久 |
| `agent:user:{user_id}:session` | Hash | 会话摘要 | 24h |
| `ai:token:{token}` | String(JSON) | SSE提交参数 | 5min |
| `ai:vector:snapshot` | String(binary) | FAISS索引快照 | 永久 |

---

## 9. 配置

### 9.1 环境变量 (`.env`)

```bash
# 服务
SERVICE_HOST=0.0.0.0
SERVICE_PORT=8086
SERVICE_NAME=oj-ai-service

# SiliconFlow (OpenAI兼容)
SILICONFLOW_API_KEY=sk-lkwtccrympupzlgcncgvjmnodewoopvohmcmxazaykqvkobi
SILICONFLOW_BASE_URL=https://api.siliconflow.cn/v1
LLM_MODEL=Qwen/Qwen3-Coder-30B-A3B-Instruct
EMBEDDING_MODEL=BAAI/bge-large-zh-v1.5
LLM_TEMPERATURE=0.7
LLM_MAX_TOKENS=4096

# Redis
REDIS_HOST=192.168.141.128
REDIS_PORT=6378
REDIS_PASSWORD=qwer1234

# Nacos
NACOS_SERVER=192.168.141.129:8848
NACOS_NAMESPACE=public
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos

# Java Gateway
GATEWAY_URL=http://localhost:8080

# Agent
AGENT_USE_LANGGRAPH=true
MCP_BING_ENABLED=false
```

### 9.2 Dockerfile

```dockerfile
FROM python:3.12-slim
WORKDIR /app
RUN pip install uv
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen
COPY src/ ./src/
EXPOSE 8086
CMD ["uv", "run", "uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8086"]
```

### 9.3 Nacos 注册

在 FastAPI `lifespan` 中完成注册/注销：

```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动时注册到 Nacos
    nacos_client.add_naming_instance(SERVICE_NAME, LOCAL_IP, SERVICE_PORT)
    # 从 Redis 恢复 FAISS 索引
    await restore_vector_store()
    yield
    # 关闭时注销
    nacos_client.remove_naming_instance(SERVICE_NAME, LOCAL_IP, SERVICE_PORT)
    await redis.close()
```

---

## 10. 编码规范

- 核心代码（service/agent/node/tool）使用**简要中文注释**标明主要功能
- 遵循 FastAPI 最佳实践：依赖注入、Pydantic 模型、类型注解
- 异步优先：所有 I/O 操作使用 `async/await`
- 错误处理：统一 `Result[T]` 响应体与 Java 端保持一致

```json
// 统一响应格式
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

---

## 11. 里程碑

| 阶段 | 内容 | 预计产出 |
|------|------|---------|
| **P1** | 项目骨架 + 配置 + Nacos注册 + Redis连接 | main.py, config.py, deps.py 可启动 |
| **P2** | LLM工厂 + RAG pipeline + 知识导入 | core/ 模块完成 |
| **P3** | AI判题 + 对话 + 提示 API (SSE) | api/user/ai_judge.py 完成 |
| **P4** | Agent编排 (LangGraph) + 工具定义 | agent/ 模块完成 |
| **P5** | MCP集成 + 管理端API + 测试 | 全部功能完成 |
| **P6** | Dockerfile + docker-compose集成 | 可部署运行 |
