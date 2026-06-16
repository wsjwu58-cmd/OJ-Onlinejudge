"""用户端Agent对话接口"""
from fastapi import APIRouter
from pydantic import BaseModel
from sse_starlette.sse import EventSourceResponse

from src.agent.graph import run_agent
from src.deps import get_chat_model

router = APIRouter(prefix="/user/agent", tags=["用户端-Agent"])


class AgentRequestDTO(BaseModel):
    sessionId: str = "default"
    task: str = ""
    context: str = ""
    userId: int | None = None
    problemId: int | None = None
    agentType: str = "langgraph"


@router.post("/chat")
async def agent_chat(request: AgentRequestDTO):
    """AI智能对话（同步）"""
    response = await run_agent(
        session_id=request.sessionId,
        task=request.task,
        user_id=str(request.userId or 0),
        problem_id=request.problemId,
        context=request.context,
    )
    return {"code": 200, "data": response}


@router.post("/chat/stream")
async def agent_chat_stream(request: AgentRequestDTO):
    """AI流式智能对话"""
    response = await run_agent(
        session_id=request.sessionId,
        task=request.task,
        user_id=str(request.userId or 0),
        problem_id=request.problemId,
        context=request.context,
    )

    chat_model = get_chat_model()

    async def event_generator():
        messages = [
            ("system", "你是一个专业的OJ AI助手。使用中文回答，Markdown格式。"),
            ("user", f"请将以下内容整理成更流畅的回复：\n{response}"),
        ]
        try:
            async for chunk in chat_model.astream(messages):
                if chunk.content:
                    yield {"event": "message", "data": chunk.content}
        except Exception as e:
            yield {"event": "message", "data": f"处理出错: {str(e)}"}
        yield {"event": "done", "data": ""}

    return EventSourceResponse(event_generator())
