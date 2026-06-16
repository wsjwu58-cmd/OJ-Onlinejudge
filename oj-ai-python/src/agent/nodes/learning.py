"""Learning节点 - 学情分析"""
from src.agent.state import AgentState
from src.agent.tools.learning import get_user_submission_stats, get_user_progress
from src.deps import get_chat_model


async def learning_node(state: AgentState) -> AgentState:
    task = state.get("task", "")
    user_id_str = state.get("user_id", "0")
    chat_model = get_chat_model()

    try:
        user_id = int(user_id_str) if user_id_str != "0" else 0
    except (ValueError, TypeError):
        user_id = 0

    if user_id > 0:
        stats = await get_user_submission_stats(user_id, 30)
        progress = await get_user_progress(user_id)
        prompt = f"用户问题：{task}\n\n统计数据：\n{stats}\n\n进度：\n{progress}\n\n请分析用户的学习情况。"
    else:
        prompt = f"用户问题：{task}\n\n请给出通用的学习建议。"

    response = await chat_model.ainvoke(prompt)
    state["learning_result"] = response.content
    state["next"] = "supervisor"
    return state
