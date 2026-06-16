"""Router节点 - 意图识别与分流"""
from src.agent.state import AgentState
from src.deps import get_chat_model


async def router_node(state: AgentState) -> AgentState:
    """分析用户意图，路由到对应Agent"""
    task = state.get("task", "")
    if not task:
        state["next"] = "supervisor"
        state["routing_result"] = "empty"
        return state

    chat_model = get_chat_model()
    prompt = f"""你是一个意图识别助手，分析用户问题并确定最合适的处理Agent。
可选Agent：solution(题解/解题), code(代码分析), learning(学情分析), knowledge(知识检索)。
只返回Agent名称。

用户问题：{task}"""

    response = await chat_model.ainvoke(prompt)
    routing = response.content.strip().lower()

    if "solution" in routing or "题解" in routing or "解题" in routing:
        next_agent = "solution"
    elif "code" in routing or "代码" in routing or "分析" in routing:
        next_agent = "code"
    elif "learning" in routing or "学习" in routing or "进度" in routing:
        next_agent = "learning"
    elif "knowledge" in routing or "知识" in routing or "概念" in routing:
        next_agent = "knowledge"
    else:
        next_agent = "supervisor"

    state["routing_result"] = routing
    state["current_agent"] = next_agent
    state["next"] = next_agent
    return state
