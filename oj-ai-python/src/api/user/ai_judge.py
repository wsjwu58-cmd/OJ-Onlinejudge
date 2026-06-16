"""用户端AI判题/对话/提示/语法检查/错误分析接口（Token-SSE模式）"""
import json
from fastapi import APIRouter, Request
from sse_starlette.sse import EventSourceResponse
from pydantic import BaseModel

from src.core.sse_token import create_token, get_token_data
from src.client.problem import problem_client
from src.core.rag.retriever import retrieve_knowledge
from src.core.memory.dialog import save_dialog, get_dialog_history
from src.deps import get_chat_model

router = APIRouter(prefix="/user/ai", tags=["用户端-AI"])

SYSTEM_RULES = """你是一位专业的OJ AI评审助手，使用中文回答。
重要规则：
1. 使用标准Markdown格式回复
2. 绝对不要使用引号包裹内容
3. 绝对不要使用'data:'作为前缀
"""


class AiJudgeDTO(BaseModel):
    userId: int | None = None
    problemId: int | None = None
    code: str = ""
    language: str = "java"


class AiChatDTO(BaseModel):
    userId: int | None = None
    problemId: int | None = None
    message: str = ""
    code: str = ""
    language: str = "java"


async def sse_stream(messages: list, chat_model) -> EventSourceResponse:
    """通用SSE流式响应生成器"""

    async def event_generator():
        try:
            async for chunk in chat_model.astream(messages):
                if chunk.content:
                    yield {"event": "message", "data": chunk.content}
        except Exception as e:
            yield {"event": "message", "data": f"AI处理出错: {str(e)}"}
        yield {"event": "done", "data": ""}

    return EventSourceResponse(event_generator())


@router.post("/judge/submit")
async def submit_judge(dto: AiJudgeDTO, request: Request):
    """提交AI判题任务"""
    data = dto.model_dump()
    user_id = request.headers.get("X-User-Id")
    if user_id:
        data["userId"] = int(user_id)
    return {"code": 200, "data": {"token": await create_token(data)}}


@router.get("/judge/stream/{token}")
async def stream_judge(token: str):
    """SSE流式获取判题结果"""
    data = await get_token_data(token)
    if not data:
        async def err():
            yield {"event": "message", "data": "任务不存在或已过期"}
            yield {"event": "done", "data": ""}
        return EventSourceResponse(err())

    dto = AiJudgeDTO(**data)
    problem = await problem_client.get_problem_by_id(dto.problemId) if dto.problemId else None

    prompt_parts = ["你是一位专业的编程竞赛评审老师。请对以下代码进行判题分析。\n"]
    if problem:
        prompt_parts.append(f"## 题目信息\n**标题：** {problem.get('title', '')}\n")
        content = problem.get("content", "") or ""
        if len(content) > 2000:
            content = content[:2000] + "..."
        prompt_parts.append(f"**描述：**\n{content}\n")
    prompt_parts.append(f"## 用户提交的代码\n**语言：** {dto.language}\n```{dto.language.lower()}\n{dto.code}\n```\n")
    prompt_parts.append("## 回复格式要求\n1. 使用Markdown格式回复\n2. 包含判定结果、代码分析、测试验证、复杂度分析、改进建议\n")

    messages = [
        ("system", SYSTEM_RULES),
        ("user", "".join(prompt_parts)),
    ]
    return await sse_stream(messages, get_chat_model())


@router.post("/syntax-check/submit")
async def submit_syntax_check(dto: AiJudgeDTO):
    return {"code": 200, "data": {"token": await create_token(dto.model_dump())}}


@router.get("/syntax-check/stream/{token}")
async def stream_syntax_check(token: str):
    data = await get_token_data(token)
    if not data:
        async def err():
            yield {"event": "message", "data": "任务不存在或已过期"}
            yield {"event": "done", "data": ""}
        return EventSourceResponse(err())

    dto = AiJudgeDTO(**data)
    prompt = f"请检查以下{dto.language}代码是否存在语法错误：\n```{dto.language.lower()}\n{dto.code}\n```\n请用中文回答。"
    messages = [
        ("system", "你是一位专业的代码审查专家。使用Markdown格式回复，不要使用引号包裹内容。"),
        ("user", prompt),
    ]
    return await sse_stream(messages, get_chat_model())


@router.post("/analyze-error/submit")
async def submit_analyze_error(dto: AiJudgeDTO):
    return {"code": 200, "data": {"token": await create_token(dto.model_dump())}}


@router.get("/analyze-error/stream/{token}")
async def stream_analyze_error(token: str):
    data = await get_token_data(token)
    if not data:
        async def err():
            yield {"event": "message", "data": "任务不存在或已过期"}
            yield {"event": "done", "data": ""}
        return EventSourceResponse(err())

    dto = AiJudgeDTO(**data)
    problem = await problem_client.get_problem_by_id(dto.problemId) if dto.problemId else None

    context = ""
    if problem:
        context += f"题目: {problem.get('title', '')}\n"
    context += f"代码: {dto.code}\n"

    knowledge = retrieve_knowledge("代码错误分析", context, 3)
    knowledge_text = "\n".join(f"{i + 1}. {k}" for i, k in enumerate(knowledge)) if knowledge else ""

    prompt = ""
    if knowledge_text:
        prompt += f"## 相关知识\n{knowledge_text}\n\n"
    prompt += f"## 代码\n```{dto.language.lower()}\n{dto.code}\n```\n请分析可能的错误并给出修改建议。"

    messages = [
        ("system", "你是一位专业的编程错误分析专家。使用Markdown格式回复。"),
        ("user", prompt),
    ]
    return await sse_stream(messages, get_chat_model())


@router.post("/chat/submit")
async def submit_chat(dto: AiChatDTO):
    return {"code": 200, "data": {"token": await create_token(dto.model_dump())}}


@router.get("/chat/stream/{token}")
async def stream_chat(token: str):
    data = await get_token_data(token)
    if not data:
        async def err():
            yield {"event": "message", "data": "任务不存在或已过期"}
            yield {"event": "done", "data": ""}
        return EventSourceResponse(err())

    dto = AiChatDTO(**data)
    uid = str(dto.userId or "0")
    pid = str(dto.problemId or "general")

    problem = await problem_client.get_problem_by_id(dto.problemId) if dto.problemId else None

    context = f"问题: {dto.message}"
    if problem:
        context = f"题目: {problem.get('title', '')}\n{context}"
    if dto.code:
        context += f"\n代码: {dto.code}"

    knowledge = retrieve_knowledge(dto.message, context, 3)
    knowledge_text = "\n".join(f"{i + 1}. {k}" for i, k in enumerate(knowledge)) if knowledge else ""

    history = await get_dialog_history(uid, pid)

    messages = [("system", "你是一位专业的编程导师，基于检索到的知识回答用户问题。使用Markdown格式回复。")]

    for h in history:
        if h["role"] == "user":
            messages.append(("user", h["content"]))
        else:
            messages.append(("assistant", h["content"]))

    prompt = ""
    if knowledge_text:
        prompt += f"## 相关知识\n{knowledge_text}\n\n"
    prompt += f"## 用户问题\n{dto.message}"
    messages.append(("user", prompt))

    await save_dialog(uid, pid, "user", dto.message)

    chat_model = get_chat_model()

    async def event_generator():
        full_response = ""
        try:
            async for chunk in chat_model.astream(messages):
                if chunk.content:
                    full_response += chunk.content
                    yield {"event": "message", "data": chunk.content}
        except Exception as e:
            yield {"event": "message", "data": f"AI回复出错: {str(e)}"}
        if full_response:
            await save_dialog(uid, pid, "assistant", full_response)
        yield {"event": "done", "data": ""}

    return EventSourceResponse(event_generator())


@router.post("/hint/submit")
async def submit_hint(dto: AiChatDTO):
    return {"code": 200, "data": {"token": await create_token(dto.model_dump())}}


@router.get("/hint/stream/{token}")
async def stream_hint(token: str):
    data = await get_token_data(token)
    if not data:
        async def err():
            yield {"event": "message", "data": "任务不存在或已过期"}
            yield {"event": "done", "data": ""}
        return EventSourceResponse(err())

    dto = AiChatDTO(**data)
    problem = await problem_client.get_problem_by_id(dto.problemId) if dto.problemId else None

    context = ""
    if problem:
        context += f"题目: {problem.get('title', '')}\n"
        if problem.get("content"):
            context += f"题目描述: {problem['content']}\n"

    knowledge = retrieve_knowledge("解题提示", context, 3)
    knowledge_text = "\n".join(f"{i + 1}. {k}" for i, k in enumerate(knowledge)) if knowledge else ""

    prompt = ""
    if knowledge_text:
        prompt += f"## 相关知识\n{knowledge_text}\n\n"
    prompt += f"请为以下编程题目提供解题思路提示（不要直接给出完整代码）：\n题目：{problem.get('title', '') if problem else dto.message}\n"
    prompt += f"题目描述：{problem.get('content', '') if problem else dto.message}\n"
    prompt += "请用中文回答，包括问题分析、解题思路、可能用到的算法或数据结构、注意事项。"

    messages = [
        ("system", "你是一位专业的编程导师，擅长引导学生思考。只给提示，不要直接给出完整答案。使用Markdown格式回复。"),
        ("user", prompt),
    ]
    return await sse_stream(messages, get_chat_model())
