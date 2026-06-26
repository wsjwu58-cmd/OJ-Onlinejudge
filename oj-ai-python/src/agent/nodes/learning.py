"""Learning节点 v2 — ReAct循环，获取用户数据后分析学情"""
from src.agent.nodes._react import react_node
from src.agent.tools import LEARNING_TOOLS


async def learning_node(state):
    return await react_node(state, LEARNING_TOOLS,
        "你是专业的OJ学情分析助手。获取用户提交统计和进度数据，分析薄弱点并给出建议。",
        "learning_result")
