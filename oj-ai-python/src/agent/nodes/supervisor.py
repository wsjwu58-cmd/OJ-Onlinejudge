"""Supervisor节点 - 汇总各Agent结果生成最终响应"""
from src.agent.state import AgentState
from src.deps import get_chat_model


async def supervisor_node(state: AgentState) -> AgentState:
    """汇总所有Agent结果，生成最终回复"""
    task = state.get("task", "")
    results_parts = []

    for key, label in [
        ("solution_result", "Solution Agent"),
        ("code_result", "Code Agent"),
        ("learning_result", "Learning Agent"),
        ("knowledge_result", "Knowledge Agent"),
    ]:
        val = state.get(key, "")
        if val and val.strip():
            results_parts.append(f"### {label}\n{val}")

    if not results_parts:
        chat_model = get_chat_model()
        response = await chat_model.ainvoke(
            f"用户问题：{task}\n请直接回答用户的问题。"
        )
        state["final_response"] = response.content
        return state

    chat_model = get_chat_model()
    summary_prompt = f"""用户任务：{task}

各Agent处理结果：
{"\n\n".join(results_parts)}

请基于以上各Agent的处理结果，生成一个综合、连贯的最终回复。
使用Markdown格式，不要使用引号包裹整个回复，不要使用'data:'作为前缀。"""

    response = await chat_model.ainvoke(summary_prompt)
    state["final_response"] = response.content
    return state
