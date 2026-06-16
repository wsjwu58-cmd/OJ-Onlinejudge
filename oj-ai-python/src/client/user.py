"""User服务客户端 - 用户信息查询"""
from src.client.gateway import gateway


class UserClient:

    async def get_user_by_id(self, user_id: int) -> dict | None:
        """获取用户信息"""
        try:
            resp = await gateway.get(f"/api/internal/user/{user_id}")
            if resp.get("code") == 1:
                return resp.get("data")
            return None
        except Exception:
            return None


user_client = UserClient()
