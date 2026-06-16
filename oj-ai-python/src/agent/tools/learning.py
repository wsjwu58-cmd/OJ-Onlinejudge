"""学情分析工具 - 用户提交统计与进度"""
from src.client.judge import judge_client


async def get_user_submission_stats(user_id: int, days: int = 30) -> str:
    """获取用户提交统计"""
    count = await judge_client.get_user_submission_count(user_id)
    return f"用户{user_id}学情统计:\n总提交次数: {count}"


async def get_user_progress(user_id: int) -> str:
    """获取用户提交进度"""
    count = await judge_client.get_user_submission_count(user_id)
    return f"用户{user_id}的提交进度:\n总提交数: {count}"
