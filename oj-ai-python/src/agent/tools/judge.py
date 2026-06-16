"""判题工具 - 获取测试用例用于代码验证"""
from src.client.problem import problem_client


async def get_test_cases_for_judge(problem_id: int) -> str:
    """获取题目的测试用例（最多5个）"""
    cases = await problem_client.get_test_cases(problem_id)
    if not cases:
        return "该题目暂无测试用例，请根据题目描述自行构造测试数据"

    parts = [f"题目{problem_id}的测试用例:"]
    for i, tc in enumerate(cases[:5], 1):
        parts.append(f"测试用例{i}:")
        parts.append(f"输入:\n{tc.get('inputData')}")
        parts.append(f"预期输出:\n{tc.get('outputData')}\n")
    if len(cases) > 5:
        parts.append(f"... 共{len(cases)}个测试用例")
    return "\n".join(parts)
