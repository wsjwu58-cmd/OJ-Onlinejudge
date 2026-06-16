"""Gateway基础HTTP客户端 - 封装认证头X-User-Id/X-User-Role"""
import httpx
from src.config import settings


class GatewayClient:
    """通过Gateway调用Java微服务的HTTP客户端"""

    def __init__(self):
        self.base_url = settings.gateway_url
        self._client: httpx.AsyncClient | None = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(timeout=30.0)
        return self._client

    async def get(self, path: str, user_id: str | None = None, user_role: str | None = None) -> dict:
        """GET请求，自动附加认证头"""
        client = await self._get_client()
        headers = {}
        if user_id:
            headers["X-User-Id"] = user_id
        if user_role:
            headers["X-User-Role"] = user_role
        resp = await client.get(f"{self.base_url}{path}", headers=headers)
        resp.raise_for_status()
        return resp.json()

    async def post(self, path: str, json_data: dict | None = None, user_id: str | None = None, user_role: str | None = None) -> dict:
        """POST请求，自动附加认证头"""
        client = await self._get_client()
        headers = {}
        if user_id:
            headers["X-User-Id"] = user_id
        if user_role:
            headers["X-User-Role"] = user_role
        resp = await client.post(f"{self.base_url}{path}", json=json_data or {}, headers=headers)
        resp.raise_for_status()
        return resp.json()

    async def close(self):
        if self._client:
            await self._client.aclose()
            self._client = None


gateway = GatewayClient()
