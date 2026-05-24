package com.oj.gateway.filter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.oj.common.constant.JwtClaimsConstant;
import com.oj.common.constant.RedisConstant;
import com.oj.common.properties.JwtProperties;
import com.oj.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.time.Duration;

@Component
@Slf4j
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ReactiveStringRedisTemplate reactiveRedisTemplate;

    // 白名单路径（不需要鉴权）
    private static final String[] WHITE_LIST = {
            "/admin/user/login",
            "/api/admin/user/login",
            "/user/userLogin/login",
            "/api/user/userLogin/login",
            "/user/userLogin/get-captcha",
            "/api/user/userLogin/get-captcha",
            "/ws/",
            "/doc.html",
            "/webjars/",
            "/v3/api-docs",
            "/internal/"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Gateway鉴权: path={}", path);

        // 1. 白名单放行
        for (String white : WHITE_LIST) {
            if (path.contains(white)) {
                return chain.filter(exchange);
            }
        }

        // 2. 根据路径前缀判断鉴权方式
        if (path.startsWith("/admin/") || path.startsWith("/api/admin/")) {
            return adminAuth(exchange, chain, request);
        } else if (path.startsWith("/user/") || path.startsWith("/api/user/")) {
            return userAuth(exchange, chain, request);
        }

        // 3. 其他路径放行
        return chain.filter(exchange);
    }

    /**
     * 管理端JWT鉴权
     */
    private Mono<Void> adminAuth(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpRequest request) {
        String token = request.getHeaders().getFirst(jwtProperties.getAdminTokenName());
        if (StrUtil.isBlank(token)) {
            return unauthorized(exchange, "管理端Token缺失");
        }

        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.debug("管理端鉴权通过: empId={}", empId);

            // 设置用户信息头传递给微服务
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(empId))
                    .header("X-User-Role", "admin")
                    .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception e) {
            log.warn("管理端JWT鉴权失败: {}", e.getMessage());
            return unauthorized(exchange, "JWT令牌无效或已过期");
        }
    }

    /**
     * 用户端Redis鉴权
     */
    private Mono<Void> userAuth(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpRequest request) {
        String token = request.getHeaders().getFirst("authorization");
        if (StrUtil.isBlank(token)) {
            // 用户端允许无token访问部分接口（如题目浏览），直接放行但不设置用户信息
            return chain.filter(exchange);
        }

        String redisKey = RedisConstant.LOGIN_USER_KEY + token;
        return reactiveRedisTemplate.opsForHash().entries(redisKey)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(userMap -> {
                    if (userMap.isEmpty()) {
                        return unauthorized(exchange, "用户Token无效或已过期");
                    }

                    Object idObj = userMap.get("id");
                    if (idObj == null) {
                        return unauthorized(exchange, "用户信息不完整");
                    }

                    Long userId = Long.valueOf(idObj.toString());
                    log.debug("用户端鉴权通过: userId={}", userId);

                    // 刷新token有效期
                    reactiveRedisTemplate.expire(redisKey, Duration.ofMinutes(30)).subscribe();

                    // 设置用户信息头传递给微服务
                    ServerHttpRequest newRequest = request.mutate()
                            .header("X-User-Id", String.valueOf(userId))
                            .header("X-User-Role", "user")
                            .build();
                    return chain.filter(exchange.mutate().request(newRequest).build());
                });
    }

    /**
     * 返回401未授权
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        JSONObject result = new JSONObject();
        result.put("code", 0);
        result.put("msg", message);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(result.toJSONString().getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
