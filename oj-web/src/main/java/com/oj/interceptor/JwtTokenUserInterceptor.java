package com.oj.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.oj.constant.RedisConstant;
import com.oj.context.BaseContext;
import com.oj.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate redisTemplate;



    /**
     * 校验redis
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            token = request.getParameter("authorization");
        }
        if(StrUtil.isBlank(token)){
            return true;
        }
        Map<Object, Object> userMap = redisTemplate.opsForHash().entries(RedisConstant.LOGIN_USER_KEY + token);
        if(userMap.isEmpty()){
            response.setStatus(401);
            return false;
        }
        UserDTO user = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        BaseContext.setCurrentId(user.getId());
        log.info("当前用户id:{}",BaseContext.getCurrentId());
        redisTemplate.expire(RedisConstant.LOGIN_USER_KEY+token,30, TimeUnit.MINUTES);
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        BaseContext.removeCurrentId();
    }
}
