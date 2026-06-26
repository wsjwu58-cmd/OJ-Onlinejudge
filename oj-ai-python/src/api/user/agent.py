"""用户端Agent对话接口"""
from fastapi import APIRouter
from pydantic import BaseModel
from sse_starlette.sse import EventSourceResponse

from src.agent.graph import run_agent, run_agent_stream
from src.client.problem import problem_client
from src.client.user import user_client
from src.core.memory.dialog import get_dialog_history, save_dialog

router = APIRouter(prefix="/user/agent", tags=["用户端-Agent"])


class AgentRequestDTO(BaseModel):
    sessionId: str = "default"
    task: str = ""
    context: str = ""
    userId: int | None = None
    problemId: int | None = None
    agentType: str = "langgraph"


async def _build_enriched_context(dto: AgentRequestDTO) -> str:
    """自动注入用户信息、题目详情、历史对话"""
    parts = []
    uid = str(dto.userId or "0")
    pid = str(dto.problemId or "general")

    if dto.userId:
        user = await user_client.get_user_by_id(dto.userId)
        if user:
            parts.append(
                f"## 当前用户信息\n"
                f"- 用户ID: {dto.userId}\n"
                f"- 用户名: {user.get('nickname', user.get('username', '未知'))}\n"
                f"- 角色: {user.get('role', '未知')}\n"
                f"- 积分: {user.get('points', 0)}\n"
            )
        else:
            parts.append(f"## 当前用户信息\n- 用户ID: {dto.userId}\n")

    if dto.problemId:
        problem = await problem_client.get_problem_by_id(dto.problemId)
        if problem:
            parts.append(
                f"## 当前题目信息\n"
                f"- 题目ID: {dto.problemId}\n"
                f"- 标题: {problem.get('title', '未知')}\n"
                f"- 难度: {problem.get('difficulty', '未知')}\n"
                f"- 类型: {problem.get('problemType', '未知')}\n"
                f"- 时间限制: {problem.get('timeLimitMs', '?')}ms\n"
                f"- 内存限制: {problem.get('memoryLimitMb', '?')}MB\n"
            )
            content = problem.get("content", "") or ""
            if content:
                parts.append(f"- 题目描述:\n{content[:1500]}\n")
        else:
            parts.append(f"## 当前题目信息\n- 题目ID: {dto.problemId}\n")

    history = await get_dialog_history(uid, pid)
    if history:
        lines = ["## 历史对话"]
        for h in history[-20:]:
            role = "用户" if h["role"] == "user" else "助手"
            lines.append(f"{role}: {h['content'][:500]}")
        parts.append("\n".join(lines))

    if dto.context:
        parts.append(f"## 附加上下文\n{dto.context}")

    return "\n\n".join(parts)


@router.post("/chat")
async def agent_chat(request: AgentRequestDTO):
    """AI智能对话（同步）"""
    uid = str(request.userId or "0")
    pid = str(request.problemId or "general")
    full_context = await _build_enriched_context(request)

    response = await run_agent(
        session_id=request.sessionId,
        task=request.task,
        user_id=uid,
        problem_id=request.problemId,
        context=full_context,
    )

    await save_dialog(uid, pid, "user", request.task)
    await save_dialog(uid, pid, "assistant", response)
    return {"code": 200, "data": response}


@router.post("/chat/stream")
async def agent_chat_stream(request: AgentRequestDTO):
    """AI流式智能对话（v2: 发射中间进度事件）"""
    uid = str(request.userId or "0")
    pid = str(request.problemId or "general")
    full_context = await _build_enriched_context(request)
    await save_dialog(uid, pid, "user", request.task)

    async def event_generator():
        full_response = ""
        try:
            async for event in run_agent_stream(
                session_id=request.sessionId,
                task=request.task,
                user_id=uid,
                problem_id=request.problemId,
                context=full_context,
            ):
                data = event.get("data", "")
                if data:
                    full_response += data
                if event.get("event") == "done":
                    break
                yield event
        except Exception as e:
            yield {"data": f"处理出错: {str(e)}"}
        if full_response:
            await save_dialog(uid, pid, "assistant", full_response)
        yield {"event": "done", "data": ""}

    return EventSourceResponse(event_generator())
