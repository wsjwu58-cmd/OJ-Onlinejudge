"""SSE令牌管理 - 统一的token存储与读取"""
import json
import uuid

from src.deps import get_redis


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
