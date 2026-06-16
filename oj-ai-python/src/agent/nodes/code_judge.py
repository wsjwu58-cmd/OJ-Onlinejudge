"""Code Judge节点 - 代码分析"""
from src.agent.state import AgentState
from src.agent.tools.judge import get_test_cases_for_judge
from src.deps import get_chat_model


async def code_judge_node(state: AgentState) -> AgentState:
    task = state.get("task", "")
    problem_id = state.get("problem_id")
    chat_model = get_chat_model()

    if problem_id:
        detail = await get_test_cases_for_judge(problem_id)
        prompt = f"用户问题：{task}\n\n题目信息：\n{detail}\n\n请分析代码正确性。"
    else:
        prompt = f"用户问题：{task}\n\n请分析代码。"

    response = await chat_model.ainvoke(prompt)
    state["code_result"] = response.content
    state["next"] = "supervisor"
    return state
