"""Knowledge节点 v2 — ReAct循环，优先检索知识库再回答"""
from src.agent.nodes._react import react_node
from src.agent.tools import KNOWLEDGE_TOOLS


async def knowledge_node(state):
    return await react_node(state, KNOWLEDGE_TOOLS,
        "你是专业的OJ知识检索助手。优先从知识库搜索相关内容再回答问题。",
        "knowledge_result")
