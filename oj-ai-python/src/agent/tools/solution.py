"""题解工具 - 获取题目详情与测试用例（异步）"""
from src.client.problem import problem_client


async def get_problem_detail(problem_id: int) -> str:
    """获取题目详细信息"""
    result = await problem_client.get_problem_by_id(problem_id)
    if not result:
        return f"题目不存在，ID: {problem_id}"

    parts = [
        f"题目ID: {result.get('id')}",
        f"标题: {result.get('title')}",
        f"难度: {result.get('difficulty')}",
    ]
    if result.get("content"):
        parts.append(f"描述: {result['content']}")
    if result.get("timeLimitMs"):
        parts.append(f"时间限制: {result['timeLimitMs']}ms")
    if result.get("memoryLimitMb"):
        parts.append(f"内存限制: {result['memoryLimitMb']}MB")
    return "\n".join(parts)


async def get_test_cases(problem_id: int) -> str:
    """获取测试用例"""
    cases = await problem_client.get_test_cases(problem_id)
    if not cases:
        return "该题目暂无测试用例"

    parts = [f"题目 {problem_id} 的测试用例:"]
    for i, tc in enumerate(cases, 1):
        parts.append(f"用例{i}:")
        parts.append(f"  输入: {tc.get('inputData')}")
        parts.append(f"  输出: {tc.get('outputData')}")
    return "\n".join(parts)
