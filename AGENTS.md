# OJ-Microservice 开发规范

## 项目概述
在线判题系统

## 目录结构
两个前端

用户端在vue-project1

管理端在vue-Element中

- oj-*/ — 微服务模块 (gateway / user / problem / contest / judge / ai)

## 核心原则
1. 根据文档进行功能实现
2. 配置一致：端口、数据库、中间件地址等均与原项目保持一致

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

## Hack 功能规范
- 竞赛锁定：选手AC后POST /user/contest/{cid}/problem/{pid}/lock锁定，Redis标记`contest:lock:{cid}:{uid}:{pid}`，TTL=比赛结束
- AC代码可见：锁定后可查同题其他AC提交的代码，统一用GET /ac-submissions接口
- Hack判题：用ProcessBuilder编译运行C++ Validator校验数据合法性（exit 0=合法），再用Judge0并行运行目标代码+标准解答，目标失败且标准通过=HackSuccess
- 排行更新：成功时Redis ZINCRBY hacker +score, hacked -score，扣减hacked用户的solved_count并清除AC标记，落地到hack_records表
- 测试用例入库：Hack成功数据写入test_cases表，source_hack_id标记来源，后续所有提交均须通过
- 通信：Hack任务/结果通过新增RocketMQ Topic异步传递；Feign定义在oj-common-api
