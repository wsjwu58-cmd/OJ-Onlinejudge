"""Judge服务客户端 - 提交记录查询"""
from src.client.gateway import gateway


class JudgeClient:

    async def get_user_submission_count(self, user_id: int) -> int:
        """获取用户提交总数"""
        try:
            resp = await gateway.get(f"/api/internal/judge/user/{user_id}/submission-count")
            if resp.get("code") == 1:
                return resp.get("data") or 0
            return 0
        except Exception:
            return 0


judge_client = JudgeClient()
