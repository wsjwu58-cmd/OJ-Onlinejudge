package com.oj.api.fallback;

import com.oj.api.UserClient;
import com.oj.api.dto.UserFeignDTO;
import com.oj.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        log.error("用户服务调用失败: {}", cause.getMessage());
        return new UserClient() {
            @Override
            public Result<UserFeignDTO> getUserById(Long id) {
                return Result.error("用户服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<List<UserFeignDTO>> getUsersByIds(List<Long> ids) {
                return Result.error("用户服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Void> updateUserSolvedCount(Long id) {
                return Result.error("用户服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<UserFeignDTO> getUserByUsername(String username) {
                return Result.error("用户服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Long> countUsers() {
                return Result.error("用户服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Integer> countUsersToday(String begin, String end) {
                return Result.error("用户服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Integer> countUserBeforeToday(String begin) {
                return Result.error("用户服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Integer> countCreateUser(String begin, String end) {
                return Result.error("用户服务调用失败: " + cause.getMessage());
            }
        };
    }
}
