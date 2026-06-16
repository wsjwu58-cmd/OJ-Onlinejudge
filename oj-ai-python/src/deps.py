"""依赖注入模块 - LLM/Embedding/Redis单例管理"""
import redis.asyncio as aioredis
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

from .config import settings

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
