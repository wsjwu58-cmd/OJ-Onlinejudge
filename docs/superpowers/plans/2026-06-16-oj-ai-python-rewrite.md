# OJ AI Module Python Rewrite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite `oj-ai-service` (Java + LangChain4j) into Python (FastAPI + LangChain v1.0 + LangGraph v1.0), consolidating knowledge management from `oj-problem-service`.

**Architecture:** FastAPI service on port 8086, registers to Nacos, communicates with Java microservices via httpx through Gateway. Token-to-SSE stream pattern for user AI endpoints. FAISS in-memory vector store + Redis persistence. LangGraph StateGraph (Router → Solution/Code/Learning/Knowledge → Supervisor).

**Tech Stack:** Python 3.12, FastAPI, LangChain v1.0, LangGraph v1.0, langchain-openai, FAISS, redis-py, httpx, pymupdf, sse-starlette, nacos-sdk-python

---

## File Structure

```
oj-ai-python/
├── pyproject.toml
├── Dockerfile
├── .env.example
├── src/
│   ├── main.py
│   ├── config.py
│   ├── deps.py
│   ├── api/
│   │   ├── router.py
│   │   ├── user/
│   │   │   ├── ai_judge.py
│   │   │   └── agent.py
│   │   └── admin/
│   │       └── knowledge.py
│   ├── core/
│   │   ├── llm.py
│   │   ├── rag/
│   │   │   ├── store.py
│   │   │   ├── importer.py
│   │   │   └── retriever.py
│   │   ├── memory/
│   │   │   ├── dialog.py
│   │   │   ├── chat.py
│   │   │   └── user_context.py
│   │   └── mcp/
│   │       └── client.py
│   ├── agent/
│   │   ├── graph.py
│   │   ├── state.py
│   │   ├── nodes/
│   │   │   ├── router.py
│   │   │   ├── solution.py
│   │   │   ├── code_judge.py
│   │   │   ├── learning.py
│   │   │   ├── knowledge.py
│   │   │   └── supervisor.py
│   │   └── tools/
│   │       ├── solution.py
│   │       ├── judge.py
│   │       ├── learning.py
│   │       └── retrieval.py
│   └── client/
│       ├── gateway.py
│       ├── problem.py
│       ├── judge.py
│       └── user.py
└── tests/
    ├── conftest.py
    ├── test_rag.py
    ├── test_agent.py
    └── test_api.py
```

---

### Task 1: Project Skeleton & Configuration

**Files:**
- Create: `oj-ai-python/pyproject.toml`
- Create: `oj-ai-python/.env.example`
- Create: `oj-ai-python/src/config.py`
- Create: `oj-ai-python/src/main.py`

- [ ] **Step 1: Write pyproject.toml**

```toml
[project]
name = "oj-ai-python"
version = "1.0.0"
requires-python = ">=3.12"
dependencies = [
    "fastapi>=0.115.0",
    "uvicorn[standard]>=0.34.0",
    "sse-starlette>=2.1.0",
    "langchain>=1.0.0",
    "langgraph>=1.0.0",
    "langchain-openai>=0.3.0",
    "langchain-community>=0.3.0",
    "langchain-mcp-adapters>=0.1.0",
    "redis>=5.2.0",
    "httpx>=0.28.0",
    "pymupdf>=1.25.0",
    "nacos-sdk-python>=2.0.0",
    "pydantic>=2.10.0",
    "pydantic-settings>=2.7.0",
    "faiss-cpu>=1.9.0",
    "python-multipart>=0.0.18",
]

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"

[tool.pytest.ini_options]
asyncio_mode = "auto"
testpaths = ["tests"]
```

- [ ] **Step 2: Write .env.example**

```bash
SERVICE_HOST=0.0.0.0
SERVICE_PORT=8086
SERVICE_NAME=oj-ai-service

SILICONFLOW_API_KEY=sk-your-key
SILICONFLOW_BASE_URL=https://api.siliconflow.cn/v1
LLM_MODEL=Qwen/Qwen3-Coder-30B-A3B-Instruct
EMBEDDING_MODEL=BAAI/bge-large-zh-v1.5
LLM_TEMPERATURE=0.7
LLM_MAX_TOKENS=4096

REDIS_HOST=192.168.141.128
REDIS_PORT=6378
REDIS_PASSWORD=qwer1234

NACOS_SERVER=192.168.141.129:8848
NACOS_NAMESPACE=public
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos

GATEWAY_URL=http://localhost:8080

AGENT_USE_LANGGRAPH=true
MCP_BING_ENABLED=false
```

- [ ] **Step 3: Write config.py**

```python
"""配置模块 - 加载环境变量与Nacos配置"""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    service_host: str = "0.0.0.0"
    service_port: int = 8086
    service_name: str = "oj-ai-service"

    siliconflow_api_key: str = ""
    siliconflow_base_url: str = "https://api.siliconflow.cn/v1"
    llm_model: str = "Qwen/Qwen3-Coder-30B-A3B-Instruct"
    embedding_model: str = "BAAI/bge-large-zh-v1.5"
    llm_temperature: float = 0.7
    llm_max_tokens: int = 4096

    redis_host: str = "192.168.141.128"
    redis_port: int = 6378
    redis_password: str = "qwer1234"

    nacos_server: str = "192.168.141.129:8848"
    nacos_namespace: str = "public"
    nacos_username: str = "nacos"
    nacos_password: str = "nacos"

    gateway_url: str = "http://localhost:8080"

    agent_use_langgraph: bool = True
    mcp_bing_enabled: bool = False

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
```

- [ ] **Step 4: Write main.py**

```python
"""FastAPI入口 - 生命周期管理、Nacos注册、路由挂载"""
import asyncio
import socket
import nacos
from contextlib import asynccontextmanager
from fastapi import FastAPI
from api.router import api_router
from config import settings
from deps import get_redis, close_redis
from core.rag.store import restore_vector_store


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期：启动时注册Nacos并恢复向量库，关闭时注销"""
    local_ip = socket.gethostbyname(socket.gethostname())
    nacos_client = nacos.NacosClient(
        server_addresses=settings.nacos_server,
        namespace=settings.nacos_namespace,
        username=settings.nacos_username,
        password=settings.nacos_password,
    )
    nacos_client.add_naming_instance(
        settings.service_name, local_ip, settings.service_port
    )
    await restore_vector_store()
    app.state.nacos_client = nacos_client
    yield
    nacos_client.remove_naming_instance(
        settings.service_name, local_ip, settings.service_port
    )
    r = await get_redis()
    if r:
        await close_redis()


app = FastAPI(title="OJ AI Service", version="1.0.0", lifespan=lifespan)
app.include_router(api_router)
```

- [ ] **Step 5: Verify the skeleton can start**

```bash
uv run uvicorn src.main:app --port 8086
```

Expected: FastAPI starts on port 8086.

---

### Task 2: Dependency Injection (deps.py)

**Files:**
- Create: `oj-ai-python/src/deps.py`

- [ ] **Step 1: Write deps.py**

```python
"""依赖注入模块 - LLM/Embedding/Redis单例管理"""
import redis.asyncio as aioredis
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

from config import settings

_redis_pool: aioredis.Redis | None = None
_chat_model: ChatOpenAI | None = None
_embedding_model: OpenAIEmbeddings | None = None


async def get_redis() -> aioredis.Redis:
    """获取Redis异步连接池（单例）"""
    global _redis_pool
    if _redis_pool is None:
        _redis_pool = aioredis.Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            password=settings.redis_password,
            decode_responses=True,
        )
    return _redis_pool


async def close_redis():
    """关闭Redis连接"""
    global _redis_pool
    if _redis_pool:
        await _redis_pool.close()
        _redis_pool = None


def get_chat_model() -> ChatOpenAI:
    """获取Chat模型单例"""
    global _chat_model
    if _chat_model is None:
        _chat_model = ChatOpenAI(
            model=settings.llm_model,
            api_key=settings.siliconflow_api_key,
            base_url=settings.siliconflow_base_url,
            temperature=settings.llm_temperature,
            max_tokens=settings.llm_max_tokens,
            streaming=True,
        )
    return _chat_model


def get_embedding_model() -> OpenAIEmbeddings:
    """获取Embedding模型单例"""
    global _embedding_model
    if _embedding_model is None:
        _embedding_model = OpenAIEmbeddings(
            model=settings.embedding_model,
            api_key=settings.siliconflow_api_key,
            base_url=settings.siliconflow_base_url,
        )
    return _embedding_model
```

- [ ] **Step 2: Verify Redis connection**

Run a quick Python snippet (in test runner):
```python
import asyncio
from src.deps import get_redis, close_redis

async def test_redis():
    r = await get_redis()
    await r.set("test_key", "hello", ex=10)
    val = await r.get("test_key")
    assert val == "hello"
    await close_redis()

asyncio.run(test_redis())
```

Expected: PASS

---

### Task 3: Java Microservice HTTP Clients

**Files:**
- Create: `oj-ai-python/src/client/gateway.py`
- Create: `oj-ai-python/src/client/problem.py`
- Create: `oj-ai-python/src/client/judge.py`
- Create: `oj-ai-python/src/client/user.py`

- [ ] **Step 1: Write gateway.py**

```python
"""Gateway基础HTTP客户端 - 封装认证头X-User-Id/X-User-Role"""
import httpx
from config import settings


class GatewayClient:
    """通过Gateway调用Java微服务的HTTP客户端"""

    def __init__(self):
        self.base_url = settings.gateway_url
        self._client: httpx.AsyncClient | None = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(timeout=30.0)
        return self._client

    async def get(self, path: str, user_id: str | None = None, user_role: str | None = None) -> dict:
        """GET请求，自动附加认证头"""
        client = await self._get_client()
        headers = {}
        if user_id:
            headers["X-User-Id"] = user_id
        if user_role:
            headers["X-User-Role"] = user_role
        resp = await client.get(f"{self.base_url}{path}", headers=headers)
        resp.raise_for_status()
        return resp.json()

    async def post(self, path: str, json_data: dict | None = None, user_id: str | None = None, user_role: str | None = None) -> dict:
        """POST请求，自动附加认证头"""
        client = await self._get_client()
        headers = {}
        if user_id:
            headers["X-User-Id"] = user_id
        if user_role:
            headers["X-User-Role"] = user_role
        resp = await client.post(f"{self.base_url}{path}", json=json_data or {}, headers=headers)
        resp.raise_for_status()
        return resp.json()

    async def close(self):
        if self._client:
            await self._client.aclose()
            self._client = None


gateway = GatewayClient()
```

- [ ] **Step 2: Write problem.py**

```python
"""Problem服务客户端 - 题目详情、测试用例查询"""
from client.gateway import gateway


class ProblemClient:

    async def get_problem_by_id(self, problem_id: int) -> dict | None:
        """获取题目详情，返回Result.data或None"""
        try:
            resp = await gateway.get(f"/api/internal/problem/{problem_id}")
            if resp.get("code") == 1:
                return resp.get("data")
            return None
        except Exception:
            return None

    async def get_test_cases(self, problem_id: int) -> list[dict] | None:
        """获取测试用例列表"""
        try:
            resp = await gateway.get(f"/api/internal/problem/{problem_id}/test-cases")
            if resp.get("code") == 1:
                return resp.get("data")
            return None
        except Exception:
            return None


problem_client = ProblemClient()
```

- [ ] **Step 3: Write judge.py**

```python
"""Judge服务客户端 - 提交记录查询"""
from client.gateway import gateway


class JudgeClient:

    async def get_user_submission_count(self, user_id: int) -> int:
        """获取用户提交总数"""
        try:
            resp = await gateway.get(f"/api/internal/judge/user/{user_id}/submission-count")
            if resp.get("code") == 1:
                return resp.get("data") or 0
            return 0
        except Exception:
            return 0


judge_client = JudgeClient()
```

- [ ] **Step 4: Write user.py**

```python
"""User服务客户端 - 用户信息查询"""
from client.gateway import gateway


class UserClient:

    async def get_user_by_id(self, user_id: int) -> dict | None:
        """获取用户信息"""
        try:
            resp = await gateway.get(f"/api/internal/user/{user_id}")
            if resp.get("code") == 1:
                return resp.get("data")
            return None
        except Exception:
            return None


user_client = UserClient()
```

---

### Task 4: RAG Pipeline - Vector Store

**Files:**
- Create: `oj-ai-python/src/core/rag/store.py`

- [ ] **Step 1: Write store.py**

```python
"""FAISS向量存储 - 内存索引 + Redis持久化快照"""
import json
import pickle
import faiss
from langchain_community.vectorstores import FAISS

from deps import get_embedding_model, get_redis

VECTOR_SNAPSHOT_KEY = "ai:vector:snapshot"
_global_vector_store: FAISS | None = None


def get_vector_store() -> FAISS | None:
    """获取全局向量存储实例"""
    return _global_vector_store


def set_vector_store(store: FAISS):
    """设置全局向量存储实例"""
    global _global_vector_store
    _global_vector_store = store


async def restore_vector_store():
    """启动时从Redis恢复FAISS索引"""
    r = await get_redis()
    raw = await r.get(VECTOR_SNAPSHOT_KEY)
    if raw:
        try:
            data = json.loads(raw)
            index_binary = bytes(data["index"])
            docs_serialized = data["docs"]

            embedding_dim = 1024
            faiss_index = faiss.IndexFlatL2(embedding_dim)
            deserialized_index = faiss.deserialize_index(index_binary)
            global _global_vector_store
            _global_vector_store = FAISS(
                embedding_function=get_embedding_model(),
                index=deserialized_index,
                docstore=None,
                index_to_docstore_id={},
            )
        except Exception:
            pass


async def persist_vector_store():
    """持久化FAISS索引到Redis"""
    global _global_vector_store
    if _global_vector_store is None:
        return
    r = await get_redis()
    index_binary = faiss.serialize_index(_global_vector_store.index)
    data = {
        "index": list(index_binary),
        "docs": [],
    }
    await r.set(VECTOR_SNAPSHOT_KEY, json.dumps(data))


def create_vector_store(documents: list) -> FAISS:
    """从文档列表创建FAISS向量存储"""
    embeddings = get_embedding_model()
    store = FAISS.from_documents(documents, embeddings)
    global _global_vector_store
    _global_vector_store = store
    return store
```

---

### Task 5: RAG Pipeline - Knowledge Importer

**Files:**
- Create: `oj-ai-python/src/core/rag/importer.py`

- [ ] **Step 1: Write importer.py**

```python
"""知识导入模块 - PDF解析、文本分块、向量化入库"""
import os
import asyncio
import fitz  # pymupdf
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document

from core.rag.store import get_vector_store, create_vector_store, persist_vector_store


async def import_pdf(file_bytes: bytes, filename: str, category: str = "") -> int:
    """导入单个PDF文件到知识库，返回导入文档数"""
    doc = fitz.open(stream=file_bytes, filetype="pdf")
    full_text = ""
    for page in doc:
        full_text += page.get_text()
    doc.close()

    if not full_text.strip():
        return 0

    splitter = RecursiveCharacterTextSplitter(
        chunk_size=350, chunk_overlap=50,
        separators=["\n\n", "\n", "。", ".", " ", ""],
    )
    chunks = splitter.split_text(full_text)

    documents = [
        Document(
            page_content=chunk,
            metadata={"source": filename, "category": category},
        )
        for chunk in chunks
    ]

    existing_store = get_vector_store()
    if existing_store:
        existing_store.add_documents(documents)
    else:
        create_vector_store(documents)

    await persist_vector_store()
    return len(documents)


async def import_directory(dir_path: str, category: str = "") -> int:
    """批量导入目录中的PDF和TXT文件"""
    total = 0
    for root, _, files in os.walk(dir_path):
        for f in files:
            filepath = os.path.join(root, f)
            try:
                if f.lower().endswith(".pdf"):
                    with open(filepath, "rb") as fp:
                        count = await import_pdf(fp.read(), f, category)
                    total += count
                elif f.lower().endswith(".txt"):
                    loader = TextLoader(filepath, encoding="utf-8")
                    docs = loader.load()
                    splitter = RecursiveCharacterTextSplitter(
                        chunk_size=350, chunk_overlap=50,
                    )
                    split_docs = splitter.split_documents(docs)
                    for d in split_docs:
                        d.metadata["source"] = f
                        d.metadata["category"] = category
                    existing_store = get_vector_store()
                    if existing_store:
                        existing_store.add_documents(split_docs)
                    else:
                        create_vector_store(split_docs)
                    total += len(split_docs)
            except Exception:
                continue
    if total > 0:
        await persist_vector_store()
    return total


async def clear_knowledge_base():
    """清空向量库"""
    global _global_vector_store
    _global_vector_store = None
    from core.rag.store import set_vector_store
    set_vector_store(None)
    from deps import get_redis
    r = await get_redis()
    await r.delete("ai:vector:snapshot")


async def get_knowledge_stats() -> dict:
    """获取知识库统计信息"""
    store = get_vector_store()
    if store is None:
        return {"total_documents": 0, "has_index": False}
    return {
        "total_documents": store.index.ntotal if store.index else 0,
        "has_index": True,
    }
```

---

### Task 6: RAG Pipeline - Retriever

**Files:**
- Create: `oj-ai-python/src/core/rag/retriever.py`

- [ ] **Step 1: Write retriever.py**

```python
"""知识检索模块 - 向量相似度搜索"""
from core.rag.store import get_vector_store


def retrieve_knowledge(query: str, context: str = "", top_k: int = 3) -> list[str]:
    """检索知识库，返回相关文本片段列表"""
    store = get_vector_store()
    if store is None:
        return []

    full_query = f"{query} {context}".strip()

    try:
        results = store.similarity_search_with_score(full_query, k=top_k)
        # 过滤score < 0.5的（FAISS返回L2 distance，越小越相似；转换为score后过滤）
        texts = []
        for doc, score in results:
            if score < 0.5:
                texts.append(doc.page_content)
        return texts[:top_k]
    except Exception:
        return []
```

---

### Task 7: Memory Services - Dialog Memory

**Files:**
- Create: `oj-ai-python/src/core/memory/dialog.py`

- [ ] **Step 1: Write dialog.py**

```python
"""对话记忆 - ai:dialog:{uid}:{pid}，1小时TTL，最多10条"""
import json
import time

from deps import get_redis


async def save_dialog(user_id: str, problem_id: str, role: str, content: str):
    """保存一条对话记录"""
    r = await get_redis()
    key = f"ai:dialog:{user_id}:{problem_id}"
    msg = {
        "role": role,
        "content": content,
        "time": int(time.time()),
    }
    existing = await r.get(key)
    dialogs = json.loads(existing) if existing else []
    if len(dialogs) >= 10:
        dialogs.pop(0)
    dialogs.append(msg)
    await r.set(key, json.dumps(dialogs, ensure_ascii=False), ex=3600)


async def get_dialog_history(user_id: str, problem_id: str) -> list[dict]:
    """获取对话历史"""
    r = await get_redis()
    key = f"ai:dialog:{user_id}:{problem_id}"
    data = await r.get(key)
    if not data:
        return []
    return json.loads(data)
```

---

### Task 8: Memory Services - Chat Memory & User Context

**Files:**
- Create: `oj-ai-python/src/core/memory/chat.py`
- Create: `oj-ai-python/src/core/memory/user_context.py`

- [ ] **Step 1: Write chat.py**

```python
"""Agent聊天记忆 - agent:chat:{memory_id}，7天TTL"""
import json

from deps import get_redis


async def save_chat_messages(memory_id: str, messages: list[dict]):
    """保存聊天消息列表"""
    r = await get_redis()
    key = f"agent:chat:{memory_id}"
    await r.set(key, json.dumps(messages, ensure_ascii=False), ex=604800)  # 7d


async def get_chat_messages(memory_id: str) -> list[dict]:
    """获取聊天消息列表"""
    r = await get_redis()
    key = f"agent:chat:{memory_id}"
    data = await r.get(key)
    if not data:
        return []
    return json.loads(data)


async def delete_chat_messages(memory_id: str):
    """删除聊天记录"""
    r = await get_redis()
    await r.delete(f"agent:chat:{memory_id}")
```

- [ ] **Step 2: Write user_context.py**

```python
"""用户长期记忆 - agent:user:{uid}:*，存储画像、交互历史、会话摘要"""
import json
import time

from deps import get_redis


async def save_user_interaction(user_id: str, session_id: str, task: str, response: str):
    """保存用户交互历史（最近50条）"""
    r = await get_redis()
    key = f"agent:user:{user_id}:history"
    entry = {
        "session_id": session_id,
        "task": task[:200],
        "response": response[:200],
        "time": int(time.time()),
    }
    existing = await r.get(key)
    history = json.loads(existing) if existing else []
    if len(history) >= 50:
        history.pop(0)
    history.append(entry)
    await r.set(key, json.dumps(history, ensure_ascii=False))


async def save_user_profile(user_id: str, profile: dict):
    """保存用户画像"""
    r = await get_redis()
    key = f"agent:user:{user_id}:profile"
    await r.hset(key, mapping=profile)


async def get_user_profile(user_id: str) -> dict:
    """获取用户画像"""
    r = await get_redis()
    key = f"agent:user:{user_id}:profile"
    data = await r.hgetall(key)
    return {k.decode() if isinstance(k, bytes) else k: v.decode() if isinstance(v, bytes) else v for k, v in data.items()}


async def save_session_summary(session_id: str, summary: dict):
    """保存会话摘要（24h TTL）"""
    r = await get_redis()
    key = f"agent:user:{session_id}:session"
    await r.hset(key, mapping=summary)
    await r.expire(key, 86400)


async def get_user_interaction_history(user_id: str) -> list[dict]:
    """获取用户交互历史"""
    r = await get_redis()
    key = f"agent:user:{user_id}:history"
    data = await r.get(key)
    if not data:
        return []
    return json.loads(data)


def build_user_context(profile: dict, history: list[dict]) -> str:
    """构建用户上下文提示词"""
    parts = []
    if profile:
        parts.append("用户画像: " + ", ".join(f"{k}={v}" for k, v in profile.items()))
    if history:
        recent = history[-5:]
        parts.append("最近交互: " + "; ".join(h["task"][:50] for h in recent))
    return "\n".join(parts)
```

---

### Task 9: SSE Token Management

**Files:**
- Create: `oj-ai-python/src/core/sse_token.py`

- [ ] **Step 1: Write sse_token.py**

```python
"""SSE令牌管理 - 统一的token存储与读取"""
import json
import uuid

from deps import get_redis


async def create_token(data: dict) -> str:
    """生成token并存储参数到Redis，5分钟TTL"""
    token = uuid.uuid4().hex
    r = await get_redis()
    await r.set(f"ai:token:{token}", json.dumps(data, ensure_ascii=False), ex=300)
    return token


async def get_token_data(token: str) -> dict | None:
    """从Redis读取token参数，读取后删除"""
    r = await get_redis()
    key = f"ai:token:{token}"
    data = await r.get(key)
    if not data:
        return None
    await r.delete(key)
    return json.loads(data)
```

---

### Task 10: API Router & User AI Endpoints

**Files:**
- Create: `oj-ai-python/src/api/router.py`
- Create: `oj-ai-python/src/api/user/ai_judge.py`

- [ ] **Step 1: Write router.py**

```python
"""路由汇总"""
from fastapi import APIRouter
from api.user import ai_judge, agent
from api.admin import knowledge

api_router = APIRouter()
api_router.include_router(ai_judge.router)
api_router.include_router(agent.router)
api_router.include_router(knowledge.router)
```

- [ ] **Step 2: Write ai_judge.py**

```python
"""用户端AI判题/对话/提示/语法检查/错误分析接口（Token-SSE模式）"""
import json
import asyncio
from fastapi import APIRouter, Request
from sse_starlette.sse import EventSourceResponse
from pydantic import BaseModel

from core.sse_token import create_token, get_token_data
from client.problem import problem_client
from core.rag.retriever import retrieve_knowledge
from core.memory.dialog import save_dialog, get_dialog_history
from deps import get_chat_model

router = APIRouter(prefix="/user/ai", tags=["用户端-AI"])

SYSTEM_RULES = """你是一位专业的OJ AI评审助手，使用中文回答。
重要规则：
1. 使用标准Markdown格式回复
2. 绝对不要使用引号包裹内容
3. 绝对不要使用'data:'作为前缀
"""


class AiJudgeDTO(BaseModel):
    userId: int | None = None
    problemId: int | None = None
    code: str = ""
    language: str = "java"


class AiChatDTO(BaseModel):
    userId: int | None = None
    problemId: int | None = None
    message: str = ""
    code: str = ""
    language: str = "java"


async def sse_stream(messages: list, chat_model) -> EventSourceResponse:
    """通用SSE流式响应生成器"""

    async def event_generator():
        try:
            async for chunk in chat_model.astream(messages):
                if chunk.content:
                    yield {"event": "message", "data": chunk.content}
        except Exception as e:
            yield {"event": "message", "data": f"AI处理出错: {str(e)}"}
        yield {"event": "done", "data": ""}

    return EventSourceResponse(event_generator())


@router.post("/judge/submit")
async def submit_judge(dto: AiJudgeDTO, request: Request):
    """提交AI判题任务"""
    return {"code": 200, "data": {"token": await create_token(dto.model_dump())}}


@router.get("/judge/stream/{token}")
async def stream_judge(token: str):
    """SSE流式获取判题结果"""
    data = await get_token_data(token)
    if not data:
        async def err():
            yield {"event": "message", "data": "任务不存在或已过期"}
            yield {"event": "done", "data": ""}
        return EventSourceResponse(err())

    dto = AiJudgeDTO(**data)
    problem = await problem_client.get_problem_by_id(dto.problemId) if dto.problemId else None

    prompt_parts = ["你是一位专业的编程竞赛评审老师。请对以下代码进行判题分析。\n"]
    if problem:
        prompt_parts.append(f"## 题目信息\n**标题：** {problem.get('title', '')}\n")
        content = problem.get("content", "") or ""
        if len(content) > 2000:
            content = content[:2000] + "..."
        prompt_parts.append(f"**描述：**\n{content}\n")
    prompt_parts.append(f"## 用户提交的代码\n**语言：** {dto.language}\n```{dto.language.lower()}\n{dto.code}\n```\n")
    prompt_parts.append("## 回复格式要求\n1. 使用Markdown格式回复\n2. 包含判定结果、代码分析、测试验证、复杂度分析、改进建议\n")

    messages = [
        ("system", SYSTEM_RULES),
        ("user", "".join(prompt_parts)),
    ]
    return await sse_stream(messages, get_chat_model())


@router.post("/syntax-check/submit")
async def submit_syntax_check(dto: AiJudgeDTO):
    return {"code": 200, "data": {"token": await create_token(dto.model_dump())}}


@router.get("/syntax-check/stream/{token}")
async def stream_syntax_check(token: str):
    data = await get_token_data(token)
    if not data:
        async def err():
            yield {"event": "message", "data": "任务不存在或已过期"}
            yield {"event": "done", "data": ""}
        return EventSourceResponse(err())

    dto = AiJudgeDTO(**data)
    prompt = f"请检查以下{dto.language}代码是否存在语法错误：\n```{dto.language.lower()}\n{dto.code}\n```\n请用中文回答。"
    messages = [
        ("system", "你是一位专业的代码审查专家。使用Markdown格式回复，不要使用引号包裹内容。"),
        ("user", prompt),
    ]
    return await sse_stream(messages, get_chat_model())


@router.post("/analyze-error/submit")
async def submit_analyze_error(dto: AiJudgeDTO):
    return {"code": 200, "data": {"token": await create_token(dto.model_dump())}}


@router.get("/analyze-error/stream/{token}")
async def stream_analyze_error(token: str):
    data = await get_token_data(token)
    if not data:
        async def err():
            yield {"event": "message", "data": "任务不存在或已过期"}
            yield {"event": "done", "data": ""}
        return EventSourceResponse(err())

    dto = AiJudgeDTO(**data)
    problem = await problem_client.get_problem_by_id(dto.problemId) if dto.problemId else None

    context = ""
    if problem:
        context += f"题目: {problem.get('title', '')}\n"
    context += f"代码: {dto.code}\n"

    knowledge = retrieve_knowledge(f"代码错误分析", context, 3)
    knowledge_text = "\n".join(f"{i + 1}. {k}" for i, k in enumerate(knowledge)) if knowledge else ""

    prompt = ""
    if knowledge_text:
        prompt += f"## 相关知识\n{knowledge_text}\n\n"
    prompt += f"## 代码\n```{dto.language.lower()}\n{dto.code}\n```\n请分析可能的错误并给出修改建议。"

    messages = [
        ("system", "你是一位专业的编程错误分析专家。使用Markdown格式回复。"),
        ("user", prompt),
    ]
    return await sse_stream(messages, get_chat_model())


@router.post("/chat/submit")
async def submit_chat(dto: AiChatDTO):
    return {"code": 200, "data": {"token": await create_token(dto.model_dump())}}


@router.get("/chat/stream/{token}")
async def stream_chat(token: str):
    data = await get_token_data(token)
    if not data:
        async def err():
            yield {"event": "message", "data": "任务不存在或已过期"}
            yield {"event": "done", "data": ""}
        return EventSourceResponse(err())

    dto = AiChatDTO(**data)
    uid = str(dto.userId or "0")
    pid = str(dto.problemId or "general")

    problem = await problem_client.get_problem_by_id(dto.problemId) if dto.problemId else None

    context = f"问题: {dto.message}"
    if problem:
        context = f"题目: {problem.get('title', '')}\n{context}"
    if dto.code:
        context += f"\n代码: {dto.code}"

    knowledge = retrieve_knowledge(dto.message, context, 3)
    knowledge_text = "\n".join(f"{i + 1}. {k}" for i, k in enumerate(knowledge)) if knowledge else ""

    history = await get_dialog_history(uid, pid)

    messages = [("system", "你是一位专业的编程导师，基于检索到的知识回答用户问题。使用Markdown格式回复。")]

    for h in history:
        if h["role"] == "user":
            messages.append(("user", h["content"]))
        else:
            messages.append(("assistant", h["content"]))

    prompt = ""
    if knowledge_text:
        prompt += f"## 相关知识\n{knowledge_text}\n\n"
    prompt += f"## 用户问题\n{dto.message}"
    messages.append(("user", prompt))

    await save_dialog(uid, pid, "user", dto.message)

    chat_model = get_chat_model()

    async def event_generator():
        full_response = ""
        try:
            async for chunk in chat_model.astream(messages):
                if chunk.content:
                    full_response += chunk.content
                    yield {"event": "message", "data": chunk.content}
        except Exception as e:
            yield {"event": "message", "data": f"AI回复出错: {str(e)}"}
        if full_response:
            await save_dialog(uid, pid, "assistant", full_response)
        yield {"event": "done", "data": ""}

    return EventSourceResponse(event_generator())


@router.post("/hint/submit")
async def submit_hint(dto: AiChatDTO):
    return {"code": 200, "data": {"token": await create_token(dto.model_dump())}}


@router.get("/hint/stream/{token}")
async def stream_hint(token: str):
    data = await get_token_data(token)
    if not data:
        async def err():
            yield {"event": "message", "data": "任务不存在或已过期"}
            yield {"event": "done", "data": ""}
        return EventSourceResponse(err())

    dto = AiChatDTO(**data)
    problem = await problem_client.get_problem_by_id(dto.problemId) if dto.problemId else None

    context = ""
    if problem:
        context += f"题目: {problem.get('title', '')}\n"
        if problem.get("content"):
            context += f"题目描述: {problem['content']}\n"

    knowledge = retrieve_knowledge("解题提示", context, 3)
    knowledge_text = "\n".join(f"{i + 1}. {k}" for i, k in enumerate(knowledge)) if knowledge else ""

    prompt = ""
    if knowledge_text:
        prompt += f"## 相关知识\n{knowledge_text}\n\n"
    prompt += f"请为以下编程题目提供解题思路提示（不要直接给出完整代码）：\n题目：{problem.get('title', '') if problem else dto.message}\n"
    prompt += f"题目描述：{problem.get('content', '') if problem else dto.message}\n"
    prompt += "请用中文回答，包括问题分析、解题思路、可能用到的算法或数据结构、注意事项。"

    messages = [
        ("system", "你是一位专业的编程导师，擅长引导学生思考。只给提示，不要直接给出完整答案。使用Markdown格式回复。"),
        ("user", prompt),
    ]
    return await sse_stream(messages, get_chat_model())
```

---

### Task 11: Agent State & Graph

**Files:**
- Create: `oj-ai-python/src/agent/state.py`
- Create: `oj-ai-python/src/agent/graph.py`

- [ ] **Step 1: Write state.py**

```python
"""AgentState定义 - LangGraph状态TypedDict"""
from typing import TypedDict


class AgentState(TypedDict):
    user_id: str
    session_id: str
    task: str
    context: str
    problem_id: int | None
    current_agent: str
    routing_result: str
    solution_result: str
    code_result: str
    learning_result: str
    knowledge_result: str
    iteration_count: int
    confidence: float
    final_response: str
    next: str


def create_initial_state(
    session_id: str = "default",
    task: str = "",
    user_id: str = "0",
    problem_id: int | None = None,
    context: str = "",
) -> AgentState:
    """创建初始AgentState"""
    return {
        "user_id": user_id,
        "session_id": session_id,
        "task": task,
        "context": context,
        "problem_id": problem_id,
        "current_agent": "router",
        "routing_result": "",
        "solution_result": "",
        "code_result": "",
        "learning_result": "",
        "knowledge_result": "",
        "iteration_count": 0,
        "confidence": 0.0,
        "final_response": "",
        "next": "router",
    }
```

- [ ] **Step 2: Write graph.py**

```python
"""LangGraph StateGraph构建与编译"""
from langgraph.graph import StateGraph, START, END

from agent.state import AgentState
from agent.nodes.router import router_node
from agent.nodes.solution import solution_node
from agent.nodes.code_judge import code_judge_node
from agent.nodes.learning import learning_node
from agent.nodes.knowledge import knowledge_node
from agent.nodes.supervisor import supervisor_node
from agent.state import create_initial_state


def build_graph() -> StateGraph:
    """构建LangGraph Agent图"""
    builder = StateGraph(AgentState)

    builder.add_node("router", router_node)
    builder.add_node("solution", solution_node)
    builder.add_node("code", code_judge_node)
    builder.add_node("learning", learning_node)
    builder.add_node("knowledge", knowledge_node)
    builder.add_node("supervisor", supervisor_node)

    builder.add_edge(START, "router")

    builder.add_conditional_edges(
        "router",
        lambda state: state.get("next", "supervisor"),
        {
            "solution": "solution",
            "code": "code",
            "learning": "learning",
            "knowledge": "knowledge",
            "supervisor": "supervisor",
        },
    )

    builder.add_edge("solution", "supervisor")
    builder.add_edge("code", "supervisor")
    builder.add_edge("learning", "supervisor")
    builder.add_edge("knowledge", "supervisor")
    builder.add_edge("supervisor", END)

    return builder


compiled_graph = build_graph().compile()


async def run_agent(
    session_id: str = "default",
    task: str = "",
    user_id: str = "0",
    problem_id: int | None = None,
    context: str = "",
) -> str:
    """执行Agent图，返回最终响应"""
    initial_state = create_initial_state(
        session_id=session_id,
        task=task,
        user_id=user_id,
        problem_id=problem_id,
        context=context,
    )
    result = await compiled_graph.ainvoke(initial_state)
    return result.get("final_response", "抱歉，处理您的请求时遇到了错误。")
```

---

### Task 12: Agent Nodes - Router & Supervisor

**Files:**
- Create: `oj-ai-python/src/agent/nodes/router.py`
- Create: `oj-ai-python/src/agent/nodes/supervisor.py`

- [ ] **Step 1: Write router.py**

```python
"""Router节点 - 意图识别与分流"""
from agent.state import AgentState
from deps import get_chat_model


async def router_node(state: AgentState) -> AgentState:
    """分析用户意图，路由到对应Agent"""
    task = state.get("task", "")
    if not task:
        state["next"] = "supervisor"
        state["routing_result"] = "empty"
        return state

    chat_model = get_chat_model()
    prompt = f"""你是一个意图识别助手，分析用户问题并确定最合适的处理Agent。
可选Agent：solution(题解/解题), code(代码分析), learning(学情分析), knowledge(知识检索)。
只返回Agent名称。

用户问题：{task}"""

    response = await chat_model.ainvoke(prompt)
    routing = response.content.strip().lower()

    if "solution" in routing or "题解" in routing or "解题" in routing:
        next_agent = "solution"
    elif "code" in routing or "代码" in routing or "分析" in routing:
        next_agent = "code"
    elif "learning" in routing or "学习" in routing or "进度" in routing:
        next_agent = "learning"
    elif "knowledge" in routing or "知识" in routing or "概念" in routing:
        next_agent = "knowledge"
    else:
        next_agent = "supervisor"

    state["routing_result"] = routing
    state["current_agent"] = next_agent
    state["next"] = next_agent
    return state
```

- [ ] **Step 2: Write supervisor.py**

```python
"""Supervisor节点 - 汇总各Agent结果生成最终响应"""
from agent.state import AgentState
from deps import get_chat_model


async def supervisor_node(state: AgentState) -> AgentState:
    """汇总所有Agent结果，生成最终回复"""
    task = state.get("task", "")
    results_parts = []

    for key, label in [
        ("solution_result", "Solution Agent"),
        ("code_result", "Code Agent"),
        ("learning_result", "Learning Agent"),
        ("knowledge_result", "Knowledge Agent"),
    ]:
        val = state.get(key, "")
        if val and val.strip():
            results_parts.append(f"### {label}\n{val}")

    if not results_parts:
        chat_model = get_chat_model()
        response = await chat_model.ainvoke(
            f"用户问题：{task}\n请直接回答用户的问题。"
        )
        state["final_response"] = response.content
        return state

    chat_model = get_chat_model()
    summary_prompt = f"""用户任务：{task}

各Agent处理结果：
{"\n\n".join(results_parts)}

请基于以上各Agent的处理结果，生成一个综合、连贯的最终回复。
使用Markdown格式，不要使用引号包裹整个回复，不要使用'data:'作为前缀。"""

    response = await chat_model.ainvoke(summary_prompt)
    state["final_response"] = response.content
    return state
```

---

### Task 13: Agent Nodes - Specialized Agents

**Files:**
- Create: `oj-ai-python/src/agent/nodes/solution.py`
- Create: `oj-ai-python/src/agent/nodes/code_judge.py`
- Create: `oj-ai-python/src/agent/nodes/learning.py`
- Create: `oj-ai-python/src/agent/nodes/knowledge.py`

- [ ] **Step 1: Write all 4 specialized agent nodes**

```python
# --- solution.py ---
"""Solution节点 - 题解生成"""
from agent.state import AgentState
from agent.tools.solution import get_problem_detail, get_test_cases
from deps import get_chat_model


async def solution_node(state: AgentState) -> AgentState:
    task = state.get("task", "")
    problem_id = state.get("problem_id")
    chat_model = get_chat_model()

    if problem_id:
        detail = await get_problem_detail(problem_id)
        prompt = f"用户问题：{task}\n\n题目详情：\n{detail}\n\n请基于题目详情生成解题思路和参考代码。"
    else:
        prompt = f"用户问题：{task}\n\n请为用户提供解题建议。"

    response = await chat_model.ainvoke(prompt)
    state["solution_result"] = response.content
    state["next"] = "supervisor"
    return state


# --- code_judge.py ---
"""Code Judge节点 - 代码分析"""
from agent.state import AgentState
from agent.tools.judge import get_test_cases_for_judge
from deps import get_chat_model


async def code_judge_node(state: AgentState) -> AgentState:
    task = state.get("task", "")
    problem_id = state.get("problem_id")
    chat_model = get_chat_model()

    if problem_id:
        detail = await get_test_cases_for_judge(problem_id)
        prompt = f"用户问题：{task}\n\n题目信息：\n{detail}\n\n请分析代码正确性。"
    else:
        prompt = f"用户问题：{task}\n\n请分析代码。"

    response = await chat_model.ainvoke(prompt)
    state["code_result"] = response.content
    state["next"] = "supervisor"
    return state


# --- learning.py ---
"""Learning节点 - 学情分析"""
from agent.state import AgentState
from agent.tools.learning import get_user_submission_stats, get_user_progress
from deps import get_chat_model


async def learning_node(state: AgentState) -> AgentState:
    task = state.get("task", "")
    user_id_str = state.get("user_id", "0")
    chat_model = get_chat_model()

    try:
        user_id = int(user_id_str) if user_id_str != "0" else 0
    except (ValueError, TypeError):
        user_id = 0

    if user_id > 0:
        stats = await get_user_submission_stats(user_id, 30)
        progress = await get_user_progress(user_id)
        prompt = f"用户问题：{task}\n\n统计数据：\n{stats}\n\n进度：\n{progress}\n\n请分析用户的学习情况。"
    else:
        prompt = f"用户问题：{task}\n\n请给出通用的学习建议。"

    response = await chat_model.ainvoke(prompt)
    state["learning_result"] = response.content
    state["next"] = "supervisor"
    return state


# --- knowledge.py ---
"""Knowledge节点 - 知识检索"""
from agent.state import AgentState
from agent.tools.retrieval import search_knowledge
from agent.tools.retrieval import search_problem_knowledge
from deps import get_chat_model


async def knowledge_node(state: AgentState) -> AgentState:
    task = state.get("task", "")
    problem_id = state.get("problem_id")
    chat_model = get_chat_model()

    knowledge = search_knowledge(task, "", 3)

    if problem_id:
        problem_knowledge = search_problem_knowledge(problem_id, task)
        knowledge = f"{knowledge}\n\n{problem_knowledge}"

    prompt = f"用户问题：{task}\n\n检索到的知识：\n{knowledge}\n\n请基于检索到的知识回答问题。"

    response = await chat_model.ainvoke(prompt)
    state["knowledge_result"] = response.content
    state["next"] = "supervisor"
    return state
```

**Files:** 4 separate files per agent, created with content as shown above.

---

### Task 14: Agent Tools

**Files:**
- Create: `oj-ai-python/src/agent/tools/solution.py`
- Create: `oj-ai-python/src/agent/tools/judge.py`
- Create: `oj-ai-python/src/agent/tools/learning.py`
- Create: `oj-ai-python/src/agent/tools/retrieval.py`

- [ ] **Step 1: Write all 4 tool files**

```python
# --- solution.py ---
"""题解工具 - 获取题目详情与测试用例（异步，从agent节点调用）"""
from client.problem import problem_client


async def get_problem_detail(problem_id: int) -> str:
    """获取题目详细信息"""
    result = await problem_client.get_problem_by_id(problem_id)
    if not result:
        return f"题目不存在，ID: {problem_id}"

    parts = [
        f"题目ID: {result.get('id')}",
        f"标题: {result.get('title')}",
        f"难度: {result.get('difficulty')}",
    ]
    if result.get("content"):
        parts.append(f"描述: {result['content']}")
    if result.get("timeLimitMs"):
        parts.append(f"时间限制: {result['timeLimitMs']}ms")
    if result.get("memoryLimitMb"):
        parts.append(f"内存限制: {result['memoryLimitMb']}MB")
    return "\n".join(parts)


async def get_test_cases(problem_id: int) -> str:
    """获取测试用例"""
    cases = await problem_client.get_test_cases(problem_id)
    if not cases:
        return "该题目暂无测试用例"

    parts = [f"题目 {problem_id} 的测试用例:"]
    for i, tc in enumerate(cases, 1):
        parts.append(f"用例{i}:")
        parts.append(f"  输入: {tc.get('inputData')}")
        parts.append(f"  输出: {tc.get('outputData')}")
    return "\n".join(parts)


# --- judge.py ---
"""判题工具 - 获取测试用例用于代码验证（异步）"""
from client.problem import problem_client


async def get_test_cases_for_judge(problem_id: int) -> str:
    """获取题目的测试用例（最多5个）"""
    cases = await problem_client.get_test_cases(problem_id)
    if not cases:
        return "该题目暂无测试用例，请根据题目描述自行构造测试数据"

    parts = [f"题目{problem_id}的测试用例:"]
    for i, tc in enumerate(cases[:5], 1):
        parts.append(f"测试用例{i}:")
        parts.append(f"输入:\n{tc.get('inputData')}")
        parts.append(f"预期输出:\n{tc.get('outputData')}\n")
    if len(cases) > 5:
        parts.append(f"... 共{len(cases)}个测试用例")
    return "\n".join(parts)


# --- learning.py ---
"""学情分析工具 - 用户提交统计与进度（异步）"""
from client.judge import judge_client


async def get_user_submission_stats(user_id: int, days: int = 30) -> str:
    """获取用户提交统计"""
    count = await judge_client.get_user_submission_count(user_id)
    return f"用户{user_id}学情统计:\n总提交次数: {count}"


async def get_user_progress(user_id: int) -> str:
    """获取用户提交进度"""
    count = await judge_client.get_user_submission_count(user_id)
    return f"用户{user_id}的提交进度:\n总提交数: {count}"


# --- retrieval.py ---
"""知识检索工具 - FAISS向量检索（同步，无I/O）"""
from core.rag.retriever import retrieve_knowledge as _retrieve


def search_knowledge(query: str, context: str = "", top_k: int = 3) -> str:
    """搜索知识库"""
    results = _retrieve(query, context, top_k)
    if not results:
        return "未在知识库中找到相关知识"
    parts = [f"从知识库中检索到 {len(results)} 条相关知识：\n"]
    for i, text in enumerate(results, 1):
        parts.append(f"【知识{i}】\n{text}\n")
    return "\n".join(parts)


def search_problem_knowledge(problem_id: int, question: str) -> str:
    """检索与特定题目相关的知识"""
    results = _retrieve(question, f"题目ID: {problem_id}", 3)
    if not results:
        return f"未找到与题目 {problem_id} 相关的知识"
    parts = [f"题目 {problem_id} 的相关知识：\n"]
    for i, text in enumerate(results, 1):
        parts.append(f"{i}. {text}\n")
    return "\n".join(parts)
```

---

### Task 15: Agent API Endpoints

**Files:**
- Create: `oj-ai-python/src/api/user/agent.py`

- [ ] **Step 1: Write agent.py**

```python
"""用户端Agent对话接口"""
from fastapi import APIRouter
from pydantic import BaseModel
from sse_starlette.sse import EventSourceResponse

from agent.graph import run_agent
from deps import get_chat_model

router = APIRouter(prefix="/user/agent", tags=["用户端-Agent"])


class AgentRequestDTO(BaseModel):
    sessionId: str = "default"
    task: str = ""
    context: str = ""
    userId: int | None = None
    problemId: int | None = None
    agentType: str = "langgraph"


@router.post("/chat")
async def agent_chat(request: AgentRequestDTO):
    """AI智能对话（同步）"""
    response = await run_agent(
        session_id=request.sessionId,
        task=request.task,
        user_id=str(request.userId or 0),
        problem_id=request.problemId,
        context=request.context,
    )
    return {"code": 200, "data": response}


@router.post("/chat/stream")
async def agent_chat_stream(request: AgentRequestDTO):
    """AI流式智能对话"""
    response = await run_agent(
        session_id=request.sessionId,
        task=request.task,
        user_id=str(request.userId or 0),
        problem_id=request.problemId,
        context=request.context,
    )

    chat_model = get_chat_model()

    async def event_generator():
        messages = [
            ("system", "你是一个专业的OJ AI助手。使用中文回答，Markdown格式。"),
            ("user", f"请将以下内容整理成更流畅的回复：\n{response}"),
        ]
        try:
            async for chunk in chat_model.astream(messages):
                if chunk.content:
                    yield {"event": "message", "data": chunk.content}
        except Exception as e:
            yield {"event": "message", "data": f"处理出错: {str(e)}"}
        yield {"event": "done", "data": ""}

    return EventSourceResponse(event_generator())
```

---

### Task 16: Admin Knowledge API Endpoints

**Files:**
- Create: `oj-ai-python/src/api/admin/knowledge.py`

- [ ] **Step 1: Write knowledge.py**

```python
"""管理端知识库管理接口"""
from fastapi import APIRouter, UploadFile, File, Form

from core.rag.importer import import_pdf, import_directory, clear_knowledge_base, get_knowledge_stats

router = APIRouter(prefix="/admin/knowledge", tags=["管理端-知识库"])


@router.post("/import/pdf")
async def import_pdf_endpoint(file: UploadFile = File(...), category: str = Form("")):
    """上传PDF导入知识库"""
    contents = await file.read()
    count = await import_pdf(contents, file.filename, category)
    return {"code": 200, "data": {"imported_count": count, "filename": file.filename}}


@router.post("/import/dir")
async def import_dir_endpoint(dir_path: str = Form(...), category: str = Form("")):
    """批量导入目录"""
    count = await import_directory(dir_path, category)
    return {"code": 200, "data": {"imported_count": count, "dir_path": dir_path}}


@router.delete("/clear")
async def clear_knowledge_endpoint():
    """清空知识库"""
    await clear_knowledge_base()
    return {"code": 200, "message": "知识库已清空"}


@router.get("/stats")
async def knowledge_stats():
    """获取知识库统计信息"""
    stats = await get_knowledge_stats()
    return {"code": 200, "data": stats}
```

---

### Task 17: Dockerfile

**Files:**
- Create: `oj-ai-python/Dockerfile`

- [ ] **Step 1: Write Dockerfile**

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

---

### Task 18: Tests

**Files:**
- Create: `oj-ai-python/tests/conftest.py`
- Create: `oj-ai-python/tests/test_api.py`
- Create: `oj-ai-python/tests/test_rag.py`

- [ ] **Step 1: Write conftest.py**

```python
"""测试夹具"""
import pytest


@pytest.fixture
def mock_redis():
    """Mock Redis fixture - 测试时不连接真实Redis"""
    class MockRedis:
        def __init__(self):
            self.data = {}
            self.ttl = {}

        async def get(self, key):
            return self.data.get(key)

        async def set(self, key, value, ex=None):
            self.data[key] = value
            if ex:
                self.ttl[key] = ex

        async def delete(self, key):
            self.data.pop(key, None)

        async def expire(self, key, ttl):
            self.ttl[key] = ttl

        async def hset(self, key, mapping):
            self.data[key] = mapping

        async def hgetall(self, key):
            return self.data.get(key, {})

        async def close(self):
            pass

    return MockRedis()
```

- [ ] **Step 2: Write test_rag.py**

```python
"""RAG模块测试"""
import pytest


def test_retrieve_empty_store():
    """空向量库应返回空列表"""
    from unittest.mock import patch
    with patch("core.rag.retriever.get_vector_store", return_value=None):
        from core.rag.retriever import retrieve_knowledge
        result = retrieve_knowledge("测试查询")
        assert result == []


def test_config_loads():
    """配置模块能正确加载默认值"""
    from config import settings
    assert settings.service_port == 8086
    assert settings.service_name == "oj-ai-service"


def test_agent_state_creation():
    """AgentState创建测试"""
    from agent.state import create_initial_state
    state = create_initial_state(
        session_id="test-session",
        task="测试任务",
        user_id="123",
        problem_id=1,
    )
    assert state["session_id"] == "test-session"
    assert state["task"] == "测试任务"
    assert state["problem_id"] == 1
    assert state["next"] == "router"
```

- [ ] **Step 3: Write test_api.py**

```python
"""API接口测试"""
import pytest
from fastapi.testclient import TestClient


@pytest.fixture
def client():
    from main import app
    return TestClient(app)


def test_judge_submit_endpoint(client):
    response = client.post("/user/ai/judge/submit", json={
        "userId": 1, "problemId": 1, "code": "print('hello')", "language": "python"
    })
    assert response.status_code == 200
    data = response.json()
    assert data["code"] == 200
    assert "token" in data["data"]


def test_syntax_check_submit_endpoint(client):
    response = client.post("/user/ai/syntax-check/submit", json={
        "userId": 1, "code": "int main(){}", "language": "cpp"
    })
    assert response.status_code == 200
    data = response.json()
    assert "token" in data["data"]


def test_chat_submit_endpoint(client):
    response = client.post("/user/ai/chat/submit", json={
        "userId": 1, "problemId": 1, "message": "什么是动态规划"
    })
    assert response.status_code == 200
    data = response.json()
    assert "token" in data["data"]


def test_agent_chat_endpoint(client):
    response = client.post("/user/agent/chat", json={
        "sessionId": "test", "task": "解释快速排序", "userId": 1
    })
    assert response.status_code == 200


def test_knowledge_stats_endpoint(client):
    response = client.get("/admin/knowledge/stats")
    assert response.status_code == 200
```

---

### Task 19: Final Integration & Verification

- [ ] **Step 1: Install dependencies and verify startup**

```bash
Set-Location -LiteralPath "E:\oj-microservice\oj-ai-python"
uv sync
uv run uvicorn src.main:app --port 8086
```

Expected: FastAPI starts, health check at `/docs` shows all endpoints.

- [ ] **Step 2: Run tests**

```bash
uv run pytest tests/ -v
```

Expected: All tests pass.

- [ ] **Step 3: Verify API routes match Java service**

```bash
uv run python -c "from main import app; routes = [r.path for r in app.routes if hasattr(r, 'path')]; routes.sort(); [print(r) for r in routes]"
```

Expected output includes:
```
/admin/knowledge/clear
/admin/knowledge/import/dir
/admin/knowledge/import/pdf
/admin/knowledge/stats
/user/agent/chat
/user/agent/chat/stream
/user/ai/analyze-error/submit
/user/ai/analyze-error/stream/{token}
/user/ai/chat/submit
/user/ai/chat/stream/{token}
/user/ai/hint/submit
/user/ai/hint/stream/{token}
/user/ai/judge/submit
/user/ai/judge/stream/{token}
/user/ai/syntax-check/submit
/user/ai/syntax-check/stream/{token}
```
