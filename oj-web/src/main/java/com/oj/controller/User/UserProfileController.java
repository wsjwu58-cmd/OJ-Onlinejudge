package com.oj.controller.User;

import com.oj.context.BaseContext;
import com.oj.dto.UserDTO;
import com.oj.entity.User;
import com.oj.result.Result;
import com.oj.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j
@RequestMapping("/user/userInfo")
@Tag(name = "个人信息")
public class UserProfileController {
    @Autowired
    private UserService userService;
    @GetMapping
    @Operation(summary = "个人信息")
    public Result<User> userProfile(){
        log.info("个人主页");
        Long currentId = BaseContext.getCurrentId();
        User user = userService.selectById(currentId);
        return Result.success(user);
    }

    @PutMapping
    @Operation(summary = "修改个人信息")
    public Result updateProfile(@RequestBody UserDTO userDTO){
        log.info("修改个人信息：{}", userDTO);
        Long currentId = BaseContext.getCurrentId();
        userDTO.setId(currentId);
        userService.updateUser(userDTO);
        return Result.success();
    }

    @GetMapping("/sign")
    @Operation(summary = "用户签到")
    public Result userSign(){
        return userService.signout();
    }

    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
