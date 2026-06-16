"""测试夹具 - Mock外部依赖(Nacos/Redis/LLM)"""
import pytest
from fastapi.testclient import TestClient


class MockRedis:
    """内存版Redis，用于测试中替代真实Redis连接"""
    def __init__(self):
        self.data = {}
        self.ttl = {}

    async def get(self, key):
        return self.data.get(key)

    async def set(self, key, value, ex=None):
        self.data[key] = value
        if ex:
            self.ttl[key] = ex

    async def delete(self, key):
        self.data.pop(key, None)

    async def expire(self, key, ttl):
        self.ttl[key] = ttl

    async def hset(self, key, mapping):
        self.data[key] = mapping

    async def hgetall(self, key):
        return self.data.get(key, {})

    async def close(self):
        self.data.clear()
        self.ttl.clear()


class MockChatModel:
    """Mock LLM，返回固定文本的异步流"""
    async def astream(self, messages):
        class Chunk:
            def __init__(self, content):
                self.content = content
        yield Chunk("[Mock AI Response]")

    async def ainvoke(self, messages):
        class Response:
            content = "[Mock AI Response]"
        return Response()


@pytest.fixture
def mock_redis():
    return MockRedis()


@pytest.fixture(autouse=True)
def _patch_deps(monkeypatch, mock_redis):
    """自动Mock deps模块中的外部依赖"""
    async def mock_get_redis():
        return mock_redis

    async def mock_close_redis():
        mock_redis.data.clear()

    def mock_get_chat_model():
        return MockChatModel()

    def mock_get_embedding_model():
        return None

    # Mock Nacos naming service
    class MockNamingService:
        async def register_instance(self, *args, **kwargs):
            pass
        async def deregister_instance(self, *args, **kwargs):
            pass

    monkeypatch.setattr("src.deps.get_redis", mock_get_redis)
    monkeypatch.setattr("src.deps.close_redis", mock_close_redis)
    monkeypatch.setattr("src.deps.get_chat_model", mock_get_chat_model)
    monkeypatch.setattr("src.deps.get_embedding_model", mock_get_embedding_model)
    monkeypatch.setattr("v2.nacos.naming.nacos_naming_service.NacosNamingService",
                        lambda config: MockNamingService())


@pytest.fixture
def client():
    """创建带Mock依赖的TestClient"""
    from src.main import app
    return TestClient(app)
