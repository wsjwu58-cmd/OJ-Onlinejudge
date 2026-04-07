package com.oj.aspect;

import com.oj.utils.JudgePerformanceMonitor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 判题链路性能监控AOP切面
 * 自动拦截关键方法并记录性能数据
 */
@Slf4j
@Aspect
@Component
public class PerformanceMonitorAspect {

    @Pointcut("execution(* com.oj.mapper.TestCaseMapper.selectByProblemId(..))")
    public void testCaseQuery() {}

    @Pointcut("execution(* com.oj.mapper.ProblemMapper.selectById(..))")
    public void problemQuery() {}

    @Pointcut("execution(* com.oj.mapper.SubMissionMapper.selectCountProblem(..))")
    public void submissionCountQuery() {}

    @Pointcut("execution(* com.oj.mapper.SubMissionMapper.insert(..))")
    public void submissionInsert() {}

    @Pointcut("execution(* com.oj.service.impl.JudgeServiceImpl.submit(..))")
    public void judgeSubmit() {}

    @Pointcut("execution(* com.oj.mq.JudgeTaskConsumer.onMessage(..))")
    public void judgeTaskConsume() {}

    @Pointcut("execution(* com.oj.mq.DatabaseUpdateConsumer.onMessage(..))")
    public void databaseUpdateConsume() {}

    @Around("testCaseQuery()")
    public Object monitorTestCaseQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "TestCaseMapper.selectByProblemId");
    }

    @Around("problemQuery()")
    public Object monitorProblemQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "ProblemMapper.selectById");
    }

    @Around("submissionCountQuery()")
    public Object monitorSubmissionCountQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "SubMissionMapper.selectCountProblem");
    }

    @Around("submissionInsert()")
    public Object monitorSubmissionInsert(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "SubMissionMapper.insert");
    }

    @Around("judgeSubmit()")
    public Object monitorJudgeSubmit(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "JudgeService.submit");
    }

    @Around("judgeTaskConsume()")
    public Object monitorJudgeTaskConsume(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "JudgeTaskConsumer.onMessage");
    }

    @Around("databaseUpdateConsume()")
    public Object monitorDatabaseUpdateConsume(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "DatabaseUpdateConsumer.onMessage");
    }

    private Object monitorPerformance(ProceedingJoinPoint joinPoint, String methodName) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = true;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            JudgePerformanceMonitor.record(
                    Thread.currentThread().getName(),
                    methodName,
                    duration
            );

            if (duration > 100) {
                log.warn("[性能警告] {} 耗时 {}ms, success={}",
                        methodName, duration, success);
            }
        }
    }
}
