"""Problem服务客户端 - 题目详情、测试用例查询"""
from src.client.gateway import gateway


class ProblemClient:

    async def get_problem_by_id(self, problem_id: int) -> dict | None:
        """获取题目详情，返回Result.data或None"""
        try:
            resp = await gateway.get(f"/api/internal/problem/{problem_id}")
            if resp.get("code") == 1:
                return resp.get("data")
            return None
        except Exception:
            return None

    async def get_test_cases(self, problem_id: int) -> list[dict] | None:
        """获取测试用例列表"""
        try:
            resp = await gateway.get(f"/api/internal/problem/{problem_id}/test-cases")
            if resp.get("code") == 1:
                return resp.get("data")
            return None
        except Exception:
            return None


problem_client = ProblemClient()
