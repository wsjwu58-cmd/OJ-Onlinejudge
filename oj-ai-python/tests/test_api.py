"""API接口测试"""
import pytest


def test_judge_submit_endpoint(client):
    response = client.post("/user/ai/judge/submit", json={
        "userId": 1, "problemId": 1, "code": "print('hello')", "language": "python"
    })
    assert response.status_code == 200
    data = response.json()
    assert data["code"] == 200
    assert "token" in data["data"]


def test_syntax_check_submit_endpoint(client):
    response = client.post("/user/ai/syntax-check/submit", json={
        "userId": 1, "code": "int main(){}", "language": "cpp"
    })
    assert response.status_code == 200
    data = response.json()
    assert "token" in data["data"]


def test_chat_submit_endpoint(client):
    response = client.post("/user/ai/chat/submit", json={
        "userId": 1, "problemId": 1, "message": "什么是动态规划"
    })
    assert response.status_code == 200
    data = response.json()
    assert "token" in data["data"]


def test_agent_chat_endpoint(client):
    response = client.post("/user/agent/chat", json={
        "sessionId": "test", "task": "解释快速排序", "userId": 1
    })
    assert response.status_code == 200


def test_knowledge_stats_endpoint(client):
    response = client.get("/admin/knowledge/stats")
    assert response.status_code == 200


def test_judge_stream_token_not_found(client):
    """测试SSE流中token不存在的情况"""
    response = client.get("/user/ai/judge/stream/nonexistent")
    assert response.status_code == 200


def test_analyze_error_submit_endpoint(client):
    response = client.post("/user/ai/analyze-error/submit", json={
        "userId": 1, "problemId": 1, "code": "def foo(:\n  pass", "language": "python"
    })
    assert response.status_code == 200
    assert "token" in response.json()["data"]


def test_hint_submit_endpoint(client):
    response = client.post("/user/ai/hint/submit", json={
        "userId": 1, "problemId": 1, "message": "如何优化排序算法"
    })
    assert response.status_code == 200
    assert "token" in response.json()["data"]


def test_agent_stream_endpoint(client):
    response = client.post("/user/agent/chat/stream", json={
        "sessionId": "test", "task": "解释动态规划", "userId": 1
    })
    assert response.status_code == 200
