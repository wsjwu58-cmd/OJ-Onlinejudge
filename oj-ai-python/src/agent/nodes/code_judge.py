"""Code Judge节点 v2 — ReAct循环，获取测试用例后分析代码"""
from src.agent.nodes._react import react_node
from src.agent.tools import CODE_TOOLS


async def code_judge_node(state):
    return await react_node(state, CODE_TOOLS,
        "你是专业的OJ代码分析助手。获取测试用例后分析代码正确性和复杂度。",
        "code_result")
