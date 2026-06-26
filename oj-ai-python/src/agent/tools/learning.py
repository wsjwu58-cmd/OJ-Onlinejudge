"""学情分析工具 - 用户提交统计、进度（@tool装饰器）"""
import logging
from collections import Counter
from langchain_core.tools import tool
from src.client.judge import judge_client

logger = logging.getLogger(__name__)


@tool
async def get_user_submission_stats(user_id: int) -> str:
    """获取用户的提交统计数据，包括总提交次数、各状态占比、涉及题目数。用于学情分析。"""
    try:
        submissions = await judge_client.get_user_submissions(user_id, page_size=100)
        total = len(submissions)
        if total == 0:
            return f"用户{user_id}: 暂无提交记录"

        status_counter = Counter(s.get("status", "Unknown") for s in submissions)
        languages = Counter(s.get("language", "?") for s in submissions)

        parts = [f"用户{user_id}学情统计:"]
        parts.append(f"- 总提交: {total}次")
        for s, c in status_counter.most_common(5):
            parts.append(f"- {s}: {c}次({c * 100 // total}%)")
        parts.append(f"- 常用语言: {', '.join(f'{l}({c})' for l, c in languages.most_common(3))}")
        return "\n".join(parts)
    except Exception as e:
        logger.error(f"获取提交统计失败: {e}")
        return f"用户{user_id}: 无法获取提交记录"


@tool
async def get_user_progress(user_id: int) -> str:
    """获取用户题目通过进度，包括已通过和未通过的题目列表。用于分析薄弱点。"""
    try:
        submissions = await judge_client.get_user_submissions(user_id, page_size=100)
        if not submissions:
            return f"用户{user_id}: 暂无提交记录"

        ac = set(s.get("problemId") for s in submissions if s.get("status") in ("Accepted", "AC"))
        failed = [s for s in submissions if s.get("status") not in ("Accepted", "AC")]
        failed_pids = set(s.get("problemId") for s in failed)

        return f"用户{user_id}: 已通过{len(ac)}题, 未通过{len(failed_pids)}题"
    except Exception as e:
        logger.error(f"获取提交进度失败: {e}")
        return f"用户{user_id}: 无法获取进度数据"
