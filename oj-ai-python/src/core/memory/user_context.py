"""用户长期记忆 - agent:user:{uid}:*，存储画像、交互历史、会话摘要"""
import json
import time

from src.deps import get_redis


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
