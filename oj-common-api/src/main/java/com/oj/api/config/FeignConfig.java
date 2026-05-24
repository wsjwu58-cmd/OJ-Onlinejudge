package com.oj.api.config;

import com.oj.common.context.BaseContext;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return template -> {
            // 1. 优先从HTTP请求上下文获取用户信息头
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String userId = request.getHeader("X-User-Id");
                String userRole = request.getHeader("X-User-Role");
                if (userId != null && !userId.isEmpty()) {
                    template.header("X-User-Id", userId);
                }
                if (userRole != null && !userRole.isEmpty()) {
                    template.header("X-User-Role", userRole);
                }
            }

            // 2. MQ消费者场景：无HTTP上下文时，从BaseContext获取
            boolean hasUserId = template.headers().get("X-User-Id") != null
                    && !template.headers().get("X-User-Id").isEmpty();
            if (!hasUserId) {
                Long currentId = BaseContext.getCurrentId();
                if (currentId != null) {
                    template.header("X-User-Id", String.valueOf(currentId));
                }
            }
        };
    }
}
