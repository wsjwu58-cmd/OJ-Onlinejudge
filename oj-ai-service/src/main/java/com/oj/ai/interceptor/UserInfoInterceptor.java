package com.oj.ai.interceptor;

import com.oj.common.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            BaseContext.setCurrentId(Long.parseLong(userId));
        }
        String userRole = request.getHeader("X-User-Role");
        if (userRole != null && !userRole.isEmpty()) {
            BaseContext.setCurrentRole(userRole);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.remove();
    }
}
