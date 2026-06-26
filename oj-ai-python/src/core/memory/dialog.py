"""对话记忆 - ai:dialog:{uid}:{pid}，1小时TTL，最多20条"""
import json
import time

from src.deps import get_redis

MAX_HISTORY = 20
DIALOG_TTL = 3600


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
    if len(dialogs) >= MAX_HISTORY:
        dialogs.pop(0)
    dialogs.append(msg)
    await r.set(key, json.dumps(dialogs, ensure_ascii=False), ex=DIALOG_TTL)


async def get_dialog_history(user_id: str, problem_id: str) -> list[dict]:
    """获取对话历史"""
    r = await get_redis()
    key = f"ai:dialog:{user_id}:{problem_id}"
    data = await r.get(key)
    if not data:
        return []
    return json.loads(data)


async def build_context_from_history(user_id: str, problem_id: str) -> str:
    """将对话历史转为Agent可用的上下文字符串"""
    history = await get_dialog_history(user_id, problem_id)
    if not history:
        return ""
    lines = ["[以下是与该题目的历史对话]"]
    for h in history:
        role_label = "用户" if h["role"] == "user" else "助手"
        content = h["content"][:300]
        lines.append(f"{role_label}: {content}")
    return "\n".join(lines)
