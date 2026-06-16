"""RAG模块测试"""
import pytest


def test_retrieve_empty_store():
    """空向量库应返回空列表"""
    from unittest.mock import patch
    with patch("src.core.rag.retriever.get_vector_store", return_value=None):
        from src.core.rag.retriever import retrieve_knowledge
        result = retrieve_knowledge("测试查询")
        assert result == []


def test_config_loads():
    """配置模块能正确加载默认值"""
    from src.config import settings
    assert settings.service_port == 8086
    assert settings.service_name == "oj-ai-service"


def test_agent_state_creation():
    """AgentState创建测试"""
    from src.agent.state import create_initial_state
    state = create_initial_state(
        session_id="test-session",
        task="测试任务",
        user_id="123",
        problem_id=1,
    )
    assert state["session_id"] == "test-session"
    assert state["task"] == "测试任务"
    assert state["problem_id"] == 1
    assert state["next"] == "router"


def test_sse_token_functions():
    """SSE令牌函数导入测试"""
    from src.core.sse_token import create_token, get_token_data
    assert callable(create_token)
    assert callable(get_token_data)
