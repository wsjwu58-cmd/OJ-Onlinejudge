"""AgentState定义 - LangGraph状态TypedDict"""
from typing import TypedDict, Annotated
from langgraph.graph.message import add_messages
from langchain_core.messages import BaseMessage


class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
    user_id: str
    session_id: str
    task: str
    context: str
    problem_id: int | None
    current_agent: str
    routing_result: str
    solution_result: str
    code_result: str
    learning_result: str
    knowledge_result: str
    iteration_count: int
    confidence: float
    final_response: str
    next: str
    interrupt_reason: str
    retry_count: int
    is_parallel: bool
    active_agents: list[str]
    router_feedback: str


def create_initial_state(
    session_id: str = "default",
    task: str = "",
    user_id: str = "0",
    problem_id: int | None = None,
    context: str = "",
) -> AgentState:
    """创建初始AgentState"""
    return {
        "messages": [],
        "user_id": user_id,
        "session_id": session_id,
        "task": task,
        "context": context,
        "problem_id": problem_id,
        "current_agent": "router",
        "routing_result": "",
        "solution_result": "",
        "code_result": "",
        "learning_result": "",
        "knowledge_result": "",
        "iteration_count": 0,
        "confidence": 0.0,
        "final_response": "",
        "next": "router",
        "interrupt_reason": "",
        "retry_count": 0,
        "is_parallel": False,
        "active_agents": [],
        "router_feedback": "",
    }
