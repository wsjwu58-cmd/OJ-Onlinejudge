package com.oj.judge.interceptor;

import com.oj.common.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdStr = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        if (userIdStr != null && !userIdStr.isEmpty()) {
            BaseContext.setCurrentId(Long.valueOf(userIdStr));
        }
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
