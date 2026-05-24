package com.oj.api;

import com.oj.api.dto.UserFeignDTO;
import com.oj.api.fallback.UserClientFallbackFactory;
import com.oj.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "oj-user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {
    @GetMapping("/internal/user/{id}")
    Result<UserFeignDTO> getUserById(@PathVariable("id") Long id);

    @PostMapping("/internal/user/batch")
    Result<List<UserFeignDTO>> getUsersByIds(@RequestBody List<Long> ids);

    @PutMapping("/internal/user/{id}/solved-count")
    Result<Void> updateUserSolvedCount(@PathVariable("id") Long id);

    @GetMapping("/internal/user/username")
    Result<UserFeignDTO> getUserByUsername(@RequestParam("username") String username);

    @GetMapping("/internal/user/count")
    Result<Long> countUsers();

    @GetMapping("/internal/user/count-today")
    Result<Integer> countUsersToday(@RequestParam("begin") String begin, @RequestParam("end") String end);

    @GetMapping("/internal/user/count-before-today")
    Result<Integer> countUserBeforeToday(@RequestParam("begin") String begin);

    @GetMapping("/internal/user/count-create")
    Result<Integer> countCreateUser(@RequestParam("begin") String begin, @RequestParam("end") String end);
}
