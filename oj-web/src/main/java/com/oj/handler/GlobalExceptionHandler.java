package com.oj.handler;

import com.oj.constant.MessageConstant;
import com.oj.exception.BaseException;
import com.oj.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex) {
        log.error("业务异常：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获数据库唯一性约束异常
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException sql){
        String message=sql.getMessage();
        if(message.contains("Duplicate entry")){
            String[] split=message.split(" ");
            String username=split[2];
            String msg=username+ MessageConstant.ALREADY_EXISTS;
            return  Result.error(msg);
        }else {
            return Result.error(MessageConstant.DATABASE_ERROR);
        }
    }

    /**
     * 捕获数据库相关异常
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLException sql){
        log.error("数据库异常：{}", sql.getMessage(), sql);
        return Result.error(MessageConstant.DATABASE_ERROR);
    }

    /**
     * 捕获空指针异常
     */
    @ExceptionHandler
    public Result exceptionHandler(NullPointerException ex){
        log.error("空指针异常：{}", ex.getMessage(), ex);
        return Result.error("系统异常：空指针异常");
    }

    /**
     * 捕获参数非法异常
     */
    @ExceptionHandler
    public Result exceptionHandler(IllegalArgumentException ex){
        log.error("参数非法异常：{}", ex.getMessage(), ex);
        return Result.error("参数错误：" + ex.getMessage());
    }

    /**
     * 捕获所有未处理的异常
     */
    @ExceptionHandler(Exception.class)
    public Result exceptionHandler(Exception ex) {
        log.error("系统异常：{}", ex.getMessage(), ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }

}
