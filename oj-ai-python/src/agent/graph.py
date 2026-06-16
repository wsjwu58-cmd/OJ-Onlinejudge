"""LangGraph StateGraph构建与编译"""
from langgraph.graph import StateGraph, START, END

from src.agent.state import AgentState, create_initial_state
from src.agent.nodes.router import router_node
from src.agent.nodes.solution import solution_node
from src.agent.nodes.code_judge import code_judge_node
from src.agent.nodes.learning import learning_node
from src.agent.nodes.knowledge import knowledge_node
from src.agent.nodes.supervisor import supervisor_node


def build_graph() -> StateGraph:
    """构建LangGraph Agent图"""
    builder = StateGraph(AgentState)

    builder.add_node("router", router_node)
    builder.add_node("solution", solution_node)
    builder.add_node("code", code_judge_node)
    builder.add_node("learning", learning_node)
    builder.add_node("knowledge", knowledge_node)
    builder.add_node("supervisor", supervisor_node)

    builder.add_edge(START, "router")

    builder.add_conditional_edges(
        "router",
        lambda state: state.get("next", "supervisor"),
        {
            "solution": "solution",
            "code": "code",
            "learning": "learning",
            "knowledge": "knowledge",
            "supervisor": "supervisor",
        },
    )

    builder.add_edge("solution", "supervisor")
    builder.add_edge("code", "supervisor")
    builder.add_edge("learning", "supervisor")
    builder.add_edge("knowledge", "supervisor")
    builder.add_edge("supervisor", END)

    return builder


compiled_graph = build_graph().compile()


async def run_agent(
    session_id: str = "default",
    task: str = "",
    user_id: str = "0",
    problem_id: int | None = None,
    context: str = "",
) -> str:
    """执行Agent图，返回最终响应"""
    initial_state = create_initial_state(
        session_id=session_id,
        task=task,
        user_id=user_id,
        problem_id=problem_id,
        context=context,
    )
    result = await compiled_graph.ainvoke(initial_state)
    return result.get("final_response", "抱歉，处理您的请求时遇到了错误。")
