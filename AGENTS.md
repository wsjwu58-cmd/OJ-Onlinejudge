# OJ-Microservice 开发规范

## 项目概述
在线判题系统从单体（oj-project/）向Spring Cloud微服务迁移改造。

## 目录结构
- oj-project/ — 原单体项目（参考源，不可修改）
- vue-project1/ — 用户端前端 (Vue3 + Element Plus + Vite)
- vue-Element/ — 管理端前端 (Vue3 + Element Plus + Vite)
- oj-*/ — 微服务模块 (gateway / user / problem / contest / judge / ai)

## 核心原则
1. 功能完全迁移：原项目所有功能完整迁移，不得遗漏
2. 禁止新功能：不做任何原项目没有的功能添加
3. 配置一致：端口、数据库、中间件地址等均与原项目保持一致
4. 问题溯源：遇到错误优先查看 oj-project/ 中对应实现

## 技术栈
后端：Spring Boot 3.4.2 + Spring Cloud + Nacos + Sentinel + OpenFeign + MyBatis-Plus + RocketMQ + JWT + Redis
前端：Vue 3 + Vite + Element Plus + Pinia + Axios + Monaco Editor
数据库：MySQL 8.0 + Redis 7.0
中间件：Nacos/Sentinel(192.168.141.129) | RocketMQ/Judge0/Skywalking(192.168.141.128)

## 开发约定
- 同步调用用 Feign（定义在 oj-common-api），异步用 RocketMQ
- Controller 分 admin/user/internal 三层，internal 供 Feign 内部调用
- 统一返回体 Result<T>，Gateway 层统一 JWT 鉴权，X-User-Id/X-User-Role 头传播用户信息
- 前端 API 统一指向 Gateway，用户端 /api/user，管理端 /api/admin
- 各服务配置由 Nacos 统一管理，本地仅保留 bootstrap.yml
- 禁止引入 Lombok 以外的注解处理器
