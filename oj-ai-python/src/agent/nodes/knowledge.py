"""Knowledge节点 - 知识检索"""
from src.agent.state import AgentState
from src.agent.tools.retrieval import search_knowledge, search_problem_knowledge
from src.deps import get_chat_model


async def knowledge_node(state: AgentState) -> AgentState:
    task = state.get("task", "")
    problem_id = state.get("problem_id")
    chat_model = get_chat_model()

    knowledge = search_knowledge(task, "", 3)

    if problem_id:
        problem_knowledge = search_problem_knowledge(problem_id, task)
        knowledge = f"{knowledge}\n\n{problem_knowledge}"

    prompt = f"用户问题：{task}\n\n检索到的知识：\n{knowledge}\n\n请基于检索到的知识回答问题。"

    response = await chat_model.ainvoke(prompt)
    state["knowledge_result"] = response.content
    state["next"] = "supervisor"
    return state
