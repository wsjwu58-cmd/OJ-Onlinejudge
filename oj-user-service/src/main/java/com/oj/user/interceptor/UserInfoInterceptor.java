package com.oj.user.interceptor;

import com.oj.common.constant.MessageConstant;
import com.oj.common.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户信息拦截器 - 从Gateway传递的请求头中提取用户信息，设置到ThreadLocal
 */
@Component
@Slf4j
public class UserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdStr = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                Long userId = Long.valueOf(userIdStr);
                BaseContext.setCurrentId(userId);
                log.debug("设置当前用户ID: {}, 角色: {}", userId, userRole);
            } catch (NumberFormatException e) {
                log.warn("用户ID格式错误: {}", userIdStr);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}
