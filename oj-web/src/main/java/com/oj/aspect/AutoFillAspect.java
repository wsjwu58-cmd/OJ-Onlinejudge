package com.oj.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 切入点
     */
//    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.wsj.annotation.AutoFill)")
//    public void autoFillPointCut(){}
//
//    @Pointcut("@annotation(com.wsj.annotation.ClearCache)")
//    public void ClearCachePointCut(){}
//    /**
//     * 前置通知，在通知中进行公共字段的赋值
//     */
//    @Before("autoFillPointCut()")
//
//    // joinPoint:连接点，即被拦截的方法的参数
//    public void autoFill(JoinPoint  joinPoint){
//        log.info("开始进行数据填充");
//        MethodSignature signature=(MethodSignature) joinPoint.getSignature();//方法签名对象
//        AutoFill autoFill=signature.getMethod().getAnnotation(AutoFill.class);
//        OperationType operationType = autoFill.value();
//        //获取当前被拦截的方法参数
//        Object[] args=joinPoint.getArgs();
//        if(args == null || args.length == 0){
//            return;
//        }
//        Object entity=args[0];
//        LocalDateTime now=LocalDateTime.now();
//        Long currentId = BaseContext.getCurrentId();
//        if(operationType == OperationType.INSERT){
//            try {
//                Method setCreateTime=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
//                Method setUpdateTime=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
//                Method setCreateUser=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER,Long.class);
//                Method setUpdateUser=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);
//                //通过反射为对象属性赋值
//                setCreateTime.invoke(entity,now);
//                setUpdateTime.invoke(entity,now);
//                setCreateUser.invoke(entity,currentId);
//                setUpdateUser.invoke(entity,currentId);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }else if(operationType==OperationType.UPDATE){
//            try {
//                //为两个公共字段赋值
//                Method setUpdateTime=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
//                Method setUpdateUser=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);
//                //通过反射为对象属性赋值
//                setUpdateTime.invoke(entity,now);
//                setUpdateUser.invoke(entity,currentId);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @AfterReturning("ClearCachePointCut()")
//    public void ClearCache(JoinPoint joinPoint){
//        log.info("开始进行缓存清理");
//        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//        Method method = signature.getMethod();
//        ClearCache annotation = method.getAnnotation(ClearCache.class);
//        Object[] args=joinPoint.getArgs();
//        //获取缓存的key
//        String[] keyPatterns = annotation.keyPatterns();
//        DishDTO dishDTO=new DishDTO();
//
//        if(keyPatterns!=null && keyPatterns.length>0){
//            for(Object arg:args){
//                if(arg instanceof DishDTO){
//                    dishDTO = (DishDTO) arg;
//                }
//            }
//            for (String keyPattern : keyPatterns) {
//                //清理缓存
//                ClearCacheKeyPattern(keyPattern+dishDTO.getCategoryId());
//            }
//        }
//    }
//
//    private void ClearCacheKeyPattern(String keyPattern) {
//        Set keys=redisTemplate.keys(keyPattern);
//        redisTemplate.delete(keys);
//    }
}

