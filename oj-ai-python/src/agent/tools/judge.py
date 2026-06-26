"""判题工具 - 获取测试用例用于代码验证（@tool装饰器）"""
from langchain_core.tools import tool
from src.client.problem import problem_client


@tool
async def get_test_cases_for_judge(problem_id: int) -> str:
    """获取题目的测试用例(最多5个)，用于验证代码正确性。当需要分析代码能否通过测试时调用。"""
    cases = await problem_client.get_test_cases(problem_id)
    if not cases:
        return "该题目暂无测试用例"
    parts = [f"题目{problem_id}的测试用例:"]
    for i, tc in enumerate(cases[:5], 1):
        parts.append(f"测试用例{i}:")
        parts.append(f"输入:\n{tc.get('inputData', '')}")
        parts.append(f"预期输出:\n{tc.get('outputData', '')}\n")
    return "\n".join(parts)
