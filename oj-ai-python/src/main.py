"""FastAPI入口 - 生命周期管理、Nacos注册、路由挂载"""
import socket
from contextlib import asynccontextmanager

from fastapi import FastAPI
from v2.nacos.common.client_config_builder import ClientConfigBuilder
from v2.nacos.naming.model.naming_param import DeregisterInstanceParam, RegisterInstanceParam
from v2.nacos.naming.nacos_naming_service import NacosNamingService

from .config import settings
from .deps import close_redis, get_redis


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期：启动时注册Nacos并恢复向量库，关闭时注销"""
    local_ip = socket.gethostbyname(socket.gethostname())
    config = (
        ClientConfigBuilder()
        .server_address(settings.nacos_server)
        .namespace_id(settings.nacos_namespace)
        .username(settings.nacos_username)
        .password(settings.nacos_password)
        .build()
    )
    naming_service = NacosNamingService(config)
    register_param = RegisterInstanceParam(
        ip=local_ip,
        port=settings.service_port,
        service_name=settings.service_name,
    )
    await naming_service.register_instance(register_param)
    app.state.naming_service = naming_service
    yield
    deregister_param = DeregisterInstanceParam(
        ip=local_ip,
        port=settings.service_port,
        service_name=settings.service_name,
    )
    await naming_service.deregister_instance(deregister_param)
    r = await get_redis()
    if r:
        await close_redis()


app = FastAPI(title="OJ AI Service", version="1.0.0", lifespan=lifespan)
