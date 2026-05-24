package com.oj.common.handler;

import com.oj.common.exception.BaseException;
import com.oj.common.exception.LoginFailedException;
import com.oj.common.exception.UserNotLoginException;
import com.oj.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BaseException.class)
    public Result<String> baseExceptionHandler(BaseException ex) {
        log.error("业务异常: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler(LoginFailedException.class)
    public Result<String> loginFailedExceptionHandler(LoginFailedException ex) {
        log.error("登录失败: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler(UserNotLoginException.class)
    public Result<String> userNotLoginExceptionHandler(UserNotLoginException ex) {
        log.error("用户未登录: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(Exception ex) {
        log.error("未知异常: {}", ex.getMessage(), ex);
        return Result.error("未知错误: " + ex.getMessage());
    }
}
