"""对话记忆 - ai:dialog:{uid}:{pid}，1小时TTL，最多10条"""
import json
import time

from src.deps import get_redis


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
