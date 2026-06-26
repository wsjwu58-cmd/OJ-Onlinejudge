"""题解工具 - 获取题目详情（@tool装饰器）"""
from langchain_core.tools import tool
from src.client.problem import problem_client


@tool
async def get_problem_detail(problem_id: int) -> str:
    """获取题目的详细信息，包括标题、难度、描述、时间限制、内存限制。当需要了解题目内容时调用。"""
    result = await problem_client.get_problem_by_id(problem_id)
    if not result:
        return f"题目不存在，ID: {problem_id}"
    parts = [
        f"题目ID: {result.get('id')}",
        f"标题: {result.get('title')}",
        f"难度: {result.get('difficulty')}",
    ]
    if result.get("content"):
        parts.append(f"描述: {result['content'][:2000]}")
    if result.get("timeLimitMs"):
        parts.append(f"时间限制: {result['timeLimitMs']}ms")
    if result.get("memoryLimitMb"):
        parts.append(f"内存限制: {result['memoryLimitMb']}MB")
    return "\n".join(parts)
