"""Solution节点 v2 — ReAct循环，获取题目详情后生成题解"""
from src.agent.nodes._react import react_node
from src.agent.tools import SOLUTION_TOOLS


async def solution_node(state):
    return await react_node(state, SOLUTION_TOOLS,
        "你是专业的OJ题解助手。先获取题目详情，再生成解题思路和参考代码。",
        "solution_result")
