"""Router节点 v2 — 意图分析 + 并行判断 + Send分发"""
from src.agent.state import AgentState
from src.deps import get_chat_model


INTENT_PROMPT = """你是一个意图识别助手。分析用户问题，判断需要调用哪些Agent。
可选Agent: solution(题解/解题), code(代码分析/判题), learning(学情分析/进度), knowledge(知识检索/概念)。

规则:
1. 如果问题同时涉及多个领域，返回多个Agent名称(用逗号分隔)
2. 如果问题模糊，返回优先级最高的一个
3. 包含"分析这道题并评估我的代码" → solution,code
4. 包含"推荐题目"或"我的弱项" → learning,code
5. 只返回Agent名称列表，例如: "solution,code" 或 "knowledge"

用户问题：{task}"""


def _parse_intent(routing: str) -> tuple[list[str], bool]:
    """解析意图，返回agent列表和是否并行"""
    agents = [a.strip() for a in routing.lower().replace("，", ",").split(",")]
    valid = {"solution", "code", "learning", "knowledge"}
    filtered = [a for a in agents if a in valid]
    if not filtered:
        return ["supervisor"], False
    return filtered, len(filtered) > 1


async def router_node(state: AgentState) -> AgentState:
    task = state.get("task", "")
    feedback = state.get("router_feedback", "")

    if not task:
        state["current_agent"] = "supervisor"
        state["next"] = "supervisor"
        return state

    chat_model = get_chat_model()
    prompt = INTENT_PROMPT.format(task=task)
    if feedback:
        prompt += f"\n\n[上次路由反馈]: {feedback}\n请重新判断。"

    response = await chat_model.ainvoke(prompt)
    routing = response.content.strip()
    agents, is_parallel = _parse_intent(routing)

    state["routing_result"] = routing
    state["is_parallel"] = is_parallel
    state["active_agents"] = agents
    state["current_agent"] = agents[0] if agents else "supervisor"
    state["next"] = agents[0] if not is_parallel else "solution"
    state["retry_count"] = state.get("retry_count", 0) + 1
    return state
