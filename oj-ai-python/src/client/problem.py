"""Problem服务客户端 - 题目详情、测试用例查询（直连，不走Gateway）"""
import logging
import httpx
from src.config import settings

logger = logging.getLogger(__name__)


class ProblemClient:

    def __init__(self):
        self.base_url = settings.problem_service_url
        self._client: httpx.AsyncClient | None = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(timeout=10.0)
        return self._client

    async def get_problem_by_id(self, problem_id: int) -> dict | None:
        """获取题目详情"""
        try:
            client = await self._get_client()
            url = f"{self.base_url}/internal/problem/{problem_id}"
            logger.info(f"Problem直连 GET: {url}")
            resp = await client.get(url)
            resp.raise_for_status()
            data = resp.json()
            if data.get("code") == 1:
                return data.get("data")
            logger.warning(f"Problem API code={data.get('code')}")
            return None
        except Exception as e:
            logger.error(f"获取题目详情失败(problem_id={problem_id}): {e}")
            return None

    async def get_test_cases(self, problem_id: int) -> list[dict] | None:
        """获取测试用例列表"""
        try:
            client = await self._get_client()
            url = f"{self.base_url}/internal/problem/{problem_id}/test-cases"
            resp = await client.get(url)
            resp.raise_for_status()
            data = resp.json()
            if data.get("code") == 1:
                return data.get("data")
            return None
        except Exception as e:
            logger.error(f"获取测试用例失败(problem_id={problem_id}): {e}")
            return None


    async def get_problems_batch(self, ids: list[int]) -> list[dict]:
        """批量获取题目"""
        try:
            client = await self._get_client()
            url = f"{self.base_url}/internal/problem/batch"
            resp = await client.post(url, json=ids)
            resp.raise_for_status()
            data = resp.json()
            if data.get("code") == 1:
                return data.get("data") or []
            return []
        except Exception as e:
            logger.error(f"批量获取题目失败: {e}")
            return []

    async def get_problem_types(self) -> list[dict]:
        """获取所有题目分类"""
        try:
            client = await self._get_client()
            url = f"{self.base_url}/admin/problem/type/all"
            resp = await client.get(url)
            resp.raise_for_status()
            data = resp.json()
            if data.get("code") == 1:
                return data.get("data") or []
            return []
        except Exception as e:
            logger.error(f"获取题目分类失败: {e}")
            return []

    async def search_problems(self, difficulty: str = "", problem_type_id: int = 0, page_size: int = 20) -> list[dict]:
        """按难度/类型搜索题目"""
        try:
            client = await self._get_client()
            url = f"{self.base_url}/user/problem/type"
            params = {"page": 1, "pageSize": page_size}
            if difficulty:
                params["difficulty"] = difficulty
            if problem_type_id > 0:
                params["problemTypeId"] = problem_type_id
            resp = await client.get(url, params=params)
            resp.raise_for_status()
            data = resp.json()
            if data.get("code") == 1 and data.get("data"):
                return data["data"].get("records", [])
            return []
        except Exception as e:
            logger.error(f"搜索题目失败: {e}")
            return []


problem_client = ProblemClient()
