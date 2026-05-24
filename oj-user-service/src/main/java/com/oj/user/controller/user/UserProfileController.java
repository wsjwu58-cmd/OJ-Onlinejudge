package com.oj.user.controller.user;

import com.oj.api.JudgeClient;
import com.oj.common.context.BaseContext;
import com.oj.common.result.Result;
import com.oj.user.dto.UserDTO;
import com.oj.user.entity.User;
import com.oj.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/user/userInfo")
@Tag(name = "用户端-个人信息")
public class UserProfileController {
    @Autowired
    private UserService userService;
    @Autowired
    private JudgeClient judgeClient;

    @GetMapping
    @Operation(summary = "个人信息")
    public Result<User> userProfile() {
        log.info("个人主页");
        Long currentId = BaseContext.getCurrentId();
        User user = userService.selectById(currentId);
        if (user != null) {
            // 通过Feign调用judge-service获取提交次数
            try {
                Result<Long> countResult = judgeClient.getUserSubmissionCount(currentId);
                if (countResult != null && countResult.getData() != null) {
                    user.setTotalSubmissions(countResult.getData().intValue());
                }
            } catch (Exception e) {
                log.warn("获取用户提交次数失败: {}", e.getMessage());
            }
        }
        return Result.success(user);
    }

    @PutMapping
    @Operation(summary = "修改个人信息")
    public Result updateProfile(@RequestBody UserDTO userDTO) {
        log.info("修改个人信息：{}", userDTO);
        Long currentId = BaseContext.getCurrentId();
        userDTO.setId(currentId);
        userService.updateUser(userDTO);
        return Result.success();
    }

    @GetMapping("/sign")
    @Operation(summary = "用户签到")
    public Result userSign() {
        return userService.signout();
    }

    @GetMapping("/sign/count")
    public Result signCount() {
        return userService.signCount();
    }

    @GetMapping("/sign/days")
    @Operation(summary = "获取指定月份签到日期列表")
    public Result signDays(@RequestParam(defaultValue = "0") int year,
                           @RequestParam(defaultValue = "0") int month) {
        if (year == 0 || month == 0) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            year = year == 0 ? now.getYear() : year;
            month = month == 0 ? now.getMonthValue() : month;
        }
        return userService.signDays(year, month);
    }
}
