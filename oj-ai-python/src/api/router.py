"""路由汇总"""
from fastapi import APIRouter

from src.api.user import ai_judge, agent
from src.api.admin import knowledge

api_router = APIRouter()
api_router.include_router(ai_judge.router)
api_router.include_router(agent.router)
api_router.include_router(knowledge.router)
