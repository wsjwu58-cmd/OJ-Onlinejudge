package com.oj.common.aspect;

import com.oj.common.annotation.AutoFill;
import com.oj.common.constant.AutoFillConstant;
import com.oj.common.context.BaseContext;
import com.oj.common.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    @Before("@annotation(com.oj.common.annotation.AutoFill)")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始自动填充...");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return;
        Object entity = args[0];
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();
        if (operationType == OperationType.INSERT) {
            try {
                Method setCreatedAt = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATED_AT, LocalDateTime.class);
                Method setUpdatedAt = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATED_AT, LocalDateTime.class);
                setCreatedAt.invoke(entity, now);
                setUpdatedAt.invoke(entity, now);
            } catch (Exception e) {
                log.warn("自动填充INSERT字段异常: {}", e.getMessage());
            }
        } else if (operationType == OperationType.UPDATE) {
            try {
                Method setUpdatedAt = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATED_AT, LocalDateTime.class);
                setUpdatedAt.invoke(entity, now);
            } catch (Exception e) {
                log.warn("自动填充UPDATE字段异常: {}", e.getMessage());
            }
        }
    }
}
