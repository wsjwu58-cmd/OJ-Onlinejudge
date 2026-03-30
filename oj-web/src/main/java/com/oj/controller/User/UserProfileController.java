package com.oj.controller.User;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oj.context.BaseContext;
import com.oj.dto.UserDTO;
import com.oj.entity.Submission;
import com.oj.entity.User;
import com.oj.mapper.SubMissionMapper;
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
    @Autowired
    private SubMissionMapper subMissionMapper;
    
    @GetMapping
    @Operation(summary = "个人信息")
    public Result<User> userProfile(){
        log.info("个人主页");
        Long currentId = BaseContext.getCurrentId();
        User user = userService.selectById(currentId);
        if (user != null) {
            LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Submission::getUserId, currentId);
            Long submissionCount = subMissionMapper.selectCount(wrapper);
            user.setTotalSubmissions(submissionCount != null ? submissionCount.intValue() : 0);
        }
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

    @GetMapping("/sign/days")
    @Operation(summary = "获取指定月份签到日期列表")
    public Result signDays(@RequestParam(defaultValue = "0") int year, 
                           @RequestParam(defaultValue = "0") int month){
        if (year == 0 || month == 0) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            year = year == 0 ? now.getYear() : year;
            month = month == 0 ? now.getMonthValue() : month;
        }
        return userService.signDays(year, month);
    }
}
