"""User服务客户端 - 用户信息查询（直连，不走Gateway）"""
import logging
import httpx
from src.config import settings

logger = logging.getLogger(__name__)


class UserClient:

    def __init__(self):
        self.base_url = settings.user_service_url
        self._client: httpx.AsyncClient | None = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(timeout=10.0)
        return self._client

    async def get_user_by_id(self, user_id: int) -> dict | None:
        """获取用户信息"""
        try:
            client = await self._get_client()
            url = f"{self.base_url}/internal/user/{user_id}"
            resp = await client.get(url)
            resp.raise_for_status()
            data = resp.json()
            if data.get("code") == 1:
                return data.get("data")
            return None
        except Exception as e:
            logger.error(f"获取用户信息失败(user_id={user_id}): {e}")
            return None


user_client = UserClient()
