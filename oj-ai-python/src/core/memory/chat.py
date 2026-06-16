"""Agent聊天记忆 - agent:chat:{memory_id}，7天TTL"""
import json

from src.deps import get_redis


async def save_chat_messages(memory_id: str, messages: list[dict]):
    """保存聊天消息列表"""
    r = await get_redis()
    key = f"agent:chat:{memory_id}"
    await r.set(key, json.dumps(messages, ensure_ascii=False), ex=604800)


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
