"""LangGraph StateGraph构建与编译 — v2 升级版"""
import sqlite3
from langgraph.graph import StateGraph, START, END
from langgraph.checkpoint.sqlite import SqliteSaver
from langgraph.types import Send

from src.agent.state import AgentState, create_initial_state
from src.agent.nodes.router import router_node
from src.agent.nodes.solution import solution_node
from src.agent.nodes.code_judge import code_judge_node
from src.agent.nodes.learning import learning_node
from src.agent.nodes.knowledge import knowledge_node
from src.agent.nodes.supervisor import supervisor_node

conn = sqlite3.connect("agent_checkpoints.db", check_same_thread=False)
checkpointer = SqliteSaver(conn)


def should_parallel(state: AgentState):
    """Router 之后的并行分发逻辑"""
    if state.get("is_parallel", False):
        sends = []
        for agent_name in state.get("active_agents", []):
            sends.append(Send(agent_name, {
                "task": state["task"],
                "user_id": state["user_id"],
                "problem_id": state.get("problem_id"),
                "context": state.get("context", ""),
                "messages": state.get("messages", []),
            }))
        if sends:
            return sends
    return state.get("next", "supervisor")


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
        should_parallel,
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

    builder.add_conditional_edges(
        "supervisor",
        lambda s: s.get("next", "end"),
        {"end": END, "router": "router"},
    )

    return builder


compiled_graph = build_graph().compile(checkpointer=checkpointer)


async def run_agent(
    session_id: str = "default",
    task: str = "",
    user_id: str = "0",
    problem_id: int | None = None,
    context: str = "",
) -> str:
    """执行Agent图，返回最终响应"""
    initial_state = create_initial_state(
        session_id=session_id, task=task, user_id=user_id,
        problem_id=problem_id, context=context,
    )
    config = {"configurable": {"thread_id": session_id}}
    result = await compiled_graph.ainvoke(initial_state, config)
    return result.get("final_response", "抱歉，处理您的请求时遇到了错误。")


async def run_agent_stream(
    session_id: str = "default",
    task: str = "",
    user_id: str = "0",
    problem_id: int | None = None,
    context: str = "",
):
    """流式执行Agent图，返回中间进度事件 + LLM输出"""
    initial_state = create_initial_state(
        session_id=session_id, task=task, user_id=user_id,
        problem_id=problem_id, context=context,
    )
    config = {"configurable": {"thread_id": session_id}}

    node_labels = {
        "router": "分析意图",
        "solution": "生成题解",
        "code": "分析代码",
        "learning": "学情分析",
        "knowledge": "检索知识",
        "supervisor": "汇总结果",
    }

    async for event in compiled_graph.astream_events(initial_state, config, version="v2"):
        kind = event.get("event", "")
        if kind == "on_chain_start":
            name = event.get("name", "")
            label = node_labels.get(name)
            if label:
                yield {"data": f"[系统] 正在{label}..."}
        elif kind == "on_chat_model_stream":
            chunk = event.get("data", {}).get("chunk")
            if chunk and hasattr(chunk, "content") and chunk.content:
                yield {"data": chunk.content}
    yield {"event": "done", "data": ""}
