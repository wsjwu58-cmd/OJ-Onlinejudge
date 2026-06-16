"""Solution节点 - 题解生成"""
from src.agent.state import AgentState
from src.agent.tools.solution import get_problem_detail, get_test_cases
from src.deps import get_chat_model


async def solution_node(state: AgentState) -> AgentState:
    task = state.get("task", "")
    problem_id = state.get("problem_id")
    chat_model = get_chat_model()

    if problem_id:
        detail = await get_problem_detail(problem_id)
        prompt = f"用户问题：{task}\n\n题目详情：\n{detail}\n\n请基于题目详情生成解题思路和参考代码。"
    else:
        prompt = f"用户问题：{task}\n\n请为用户提供解题建议。"

    response = await chat_model.ainvoke(prompt)
    state["solution_result"] = response.content
    state["next"] = "supervisor"
    return state
