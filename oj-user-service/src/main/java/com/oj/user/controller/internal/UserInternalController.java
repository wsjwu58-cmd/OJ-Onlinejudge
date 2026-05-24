package com.oj.user.controller.internal;

import com.oj.api.dto.UserFeignDTO;
import com.oj.common.result.Result;
import com.oj.user.dto.UserDTO;
import com.oj.user.entity.User;
import com.oj.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内部Feign接口 - 供其他微服务调用，不经过网关鉴权
 */
@RestController
@RequestMapping("/internal/user")
@Slf4j
public class UserInternalController {

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/{id}")
    public Result<UserFeignDTO> getUserById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        UserFeignDTO dto = new UserFeignDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setNickname(user.getNickName());
        return Result.success(dto);
    }

    @PostMapping("/batch")
    public Result<List<UserFeignDTO>> getUsersByIds(@RequestBody List<Long> ids) {
        List<UserFeignDTO> result = new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(ids);
            for (User user : users) {
                UserFeignDTO dto = new UserFeignDTO();
                BeanUtils.copyProperties(user, dto);
                dto.setNickname(user.getNickName());
                result.add(dto);
            }
        }
        return Result.success(result);
    }

    @PutMapping("/{id}/solved-count")
    public Result<Void> updateUserSolvedCount(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPoints(user.getPoints() != null ? user.getPoints() + 1 : 1);
            userMapper.updateById(user);
        }
        return Result.success();
    }

    @GetMapping("/username")
    public Result<UserFeignDTO> getUserByUsername(@RequestParam("username") String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return Result.error("用户不存在");
        }
        UserFeignDTO dto = new UserFeignDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setNickname(user.getNickName());
        return Result.success(dto);
    }

    @GetMapping("/count")
    public Result<Long> countUsers() {
        return Result.success(userMapper.selectCount(null));
    }

    @GetMapping("/count-today")
    public Result<Integer> countUsersToday(
            @RequestParam("begin") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime begin,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);
        Integer count = userMapper.countUserToday(map);
        return Result.success(count != null ? count : 0);
    }

    @GetMapping("/count-before-today")
    public Result<Integer> countUserBeforeToday(
            @RequestParam("begin") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime begin) {
        Integer count = userMapper.countUserBeforeToday(begin);
        return Result.success(count != null ? count : 0);
    }

    @GetMapping("/count-create")
    public Result<Integer> countCreateUser(
            @RequestParam("begin") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime begin,
            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);
        Integer count = userMapper.countCreateUser(map);
        return Result.success(count != null ? count : 0);
    }
}
