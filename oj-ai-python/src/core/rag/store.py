"""FAISS向量存储 - 内存索引 + Redis持久化快照"""
import json
import faiss
from langchain_community.vectorstores import FAISS

from src.deps import get_embedding_model, get_redis

VECTOR_SNAPSHOT_KEY = "ai:vector:snapshot"
_global_vector_store: FAISS | None = None


def get_vector_store() -> FAISS | None:
    """获取全局向量存储实例"""
    return _global_vector_store


def set_vector_store(store: FAISS | None):
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
            faiss_index = faiss.deserialize_index(index_binary)
            global _global_vector_store
            _global_vector_store = FAISS(
                embedding_function=get_embedding_model(),
                index=faiss_index,
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
