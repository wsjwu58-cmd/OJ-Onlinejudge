"""Judge服务客户端 - 提交记录查询（直连，不走Gateway）"""
import logging
import httpx
from src.config import settings

logger = logging.getLogger(__name__)


class JudgeClient:

    def __init__(self):
        self.base_url = settings.judge_service_url
        self._client: httpx.AsyncClient | None = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(timeout=10.0)
        return self._client

    async def get_user_submission_count(self, user_id: int) -> int:
        """获取用户提交总数"""
        try:
            client = await self._get_client()
            url = f"{self.base_url}/internal/judge/user/{user_id}/submission-count"
            logger.info(f"Judge直连 GET: {url}")
            resp = await client.get(url)
            resp.raise_for_status()
            data = resp.json()
            if data.get("code") == 1:
                return data.get("data") or 0
            logger.warning(f"Judge API code={data.get('code')}: {data}")
            return 0
        except Exception as e:
            logger.error(f"获取用户提交统计失败(user_id={user_id}): {e}")
            raise

    async def get_user_submissions(self, user_id: int, page_size: int = 50) -> list[dict]:
        """获取用户提交记录列表（含题目ID、状态、语言）"""
        try:
            client = await self._get_client()
            url = f"{self.base_url}/admin/submissions/page"
            params = {"userId": user_id, "page": 1, "pageSize": page_size}
            logger.info(f"Judge直连 GET: {url} params={params}")
            resp = await client.get(url, params=params)
            resp.raise_for_status()
            data = resp.json()
            if data.get("code") == 1 and data.get("data"):
                return data["data"].get("records", [])
            return []
        except Exception as e:
            logger.error(f"获取用户提交列表失败(user_id={user_id}): {e}")
            return []


judge_client = JudgeClient()
