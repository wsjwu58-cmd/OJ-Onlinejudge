"""配置模块 - 加载环境变量与Nacos配置"""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    service_host: str = "0.0.0.0"
    service_port: int = 8086
    service_name: str = "oj-ai-service"

    siliconflow_api_key: str = ""
    siliconflow_base_url: str = "https://api.siliconflow.cn/v1"
    llm_model: str = "Qwen/Qwen3-Coder-30B-A3B-Instruct"
    embedding_model: str = "BAAI/bge-large-zh-v1.5"
    llm_temperature: float = 0.7
    llm_max_tokens: int = 4096

    redis_host: str = "192.168.141.128"
    redis_port: int = 6378
    redis_password: str = "qwer1234"

    nacos_server: str = "192.168.141.129:8848"
    nacos_namespace: str = "public"
    nacos_username: str = "nacos"
    nacos_password: str = "nacos"

    gateway_url: str = "http://localhost:8080"

    problem_service_url: str = "http://localhost:8082"
    judge_service_url: str = "http://localhost:8084"
    user_service_url: str = "http://localhost:8081"

    agent_use_langgraph: bool = True
    mcp_bing_enabled: bool = False

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
