"""Supervisor节点 v2 — 汇总结果 + confidence评分 + 动态回退"""
from src.agent.state import AgentState
from src.deps import get_chat_model


def _estimate_confidence(text: str) -> float:
    """基于回复内容估算置信度"""
    if not text or len(text) < 30:
        return 0.3
    low_confidence_words = ["不确定", "可能", "猜测", "无法确定", "抱歉", "无法"]
    count = sum(1 for w in low_confidence_words if w in text)
    base = 0.8
    return max(0.2, base - count * 0.15)


async def supervisor_node(state: AgentState) -> AgentState:
    task = state.get("task", "")
    results = []
    for key, label in [
        ("solution_result", "题解分析"),
        ("code_result", "代码分析"),
        ("learning_result", "学习分析"),
        ("knowledge_result", "知识检索"),
    ]:
        val = state.get(key, "")
        if val and val.strip():
            results.append(f"### {label}\n{val}")

    if not results:
        chat_model = get_chat_model()
        response = await chat_model.ainvoke(
            f"用户问题：{task}\n请直接回答用户的问题。"
        )
        state["final_response"] = response.content
        state["confidence"] = _estimate_confidence(response.content)
        state["next"] = "end"
        return state

    chat_model = get_chat_model()
    summary = f"""用户任务：{task}

各Agent处理结果：
{"\n\n".join(results)}

请综合分析以上结果，生成一个连贯的最终回复。Markdown格式。"""

    response = await chat_model.ainvoke(summary)
    conf = _estimate_confidence(response.content)
    state["final_response"] = response.content
    state["confidence"] = conf
    state["iteration_count"] = state.get("iteration_count", 0) + 1

    if conf < 0.5 and state.get("retry_count", 0) < 2:
        state["router_feedback"] = f"上次分析置信度低({conf}), 结果: {response.content[:200]}"
        state["next"] = "router"
    else:
        if conf < 0.5:
            state["final_response"] += "\n\n> ⚠️ AI对本轮分析置信度较低，建议核实。"
        state["next"] = "end"

    return state
