"""ReAct循环节点工厂 — 所有Agent节点共享模板"""
from langchain_core.messages import SystemMessage, HumanMessage, ToolMessage
from src.deps import get_chat_model


async def react_node(
    state,
    tools: list,
    system_prompt: str,
    result_key: str,
    task_prefix: str = "",
    max_rounds: int = 5,
) -> dict:
    """通用ReAct循环: 思考→调工具→观察→再思考"""
    model = get_chat_model().bind_tools(tools)
    task = state.get("task", "")
    context = state.get("context", "")
    problem_id = state.get("problem_id")

    user_content = f"{task_prefix}\n## 用户问题\n{task}"
    if context:
        user_content = f"{context}\n\n{user_content}"
    if problem_id:
        user_content = f"当前题目ID: {problem_id}\n\n{user_content}"

    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_content),
    ]

    for _ in range(max_rounds):
        response = await model.ainvoke(messages)
        messages.append(response)

        if not response.tool_calls:
            return {result_key: response.content, "next": "supervisor"}

        for tc in response.tool_calls:
            tool_name = tc.get("name", "")
            tool_args = tc.get("args", {})
            try:
                tool_func = {t.name: t for t in tools}[tool_name]
                result = await tool_func.ainvoke(tool_args)
                result_str = str(result)
            except Exception as e:
                result_str = f"工具调用失败: {e}"
            messages.append(ToolMessage(content=result_str, tool_call_id=tc["id"]))

    final = await model.ainvoke(messages)
    return {result_key: final.content, "next": "supervisor"}
