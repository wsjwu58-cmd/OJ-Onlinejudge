package com.oj.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.api.ContestClient;
import com.oj.api.JudgeClient;
import com.oj.api.dto.WorkspaceActivityFeignDTO;
import com.oj.common.constant.MessageConstant;
import com.oj.common.constant.PasswordConstant;
import com.oj.common.constant.RedisConstant;
import com.oj.common.constant.StatusConstant;
import com.oj.common.context.BaseContext;
import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.user.dto.UserDTO;
import com.oj.user.dto.UserLoginDTO;
import com.oj.user.dto.UserQueryDTO;
import com.oj.user.entity.User;
import com.oj.user.mapper.UserMapper;
import com.oj.user.service.UserService;
import com.oj.user.vo.UserLoginVo;
import com.oj.common.enumeration.ActivityType;
import com.oj.common.exception.AccountLockedException;
import com.oj.common.exception.AccountNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ContestClient contestClient;
    @Autowired
    private JudgeClient judgeClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public User login(User user) {
        String username = user.getUsername();
        String password = user.getPasswordHash();

        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getUsername, username);
        User newuser = userMapper.selectOne(lambdaQueryWrapper);

        if (newuser == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if (newuser.getStatus() == StatusConstant.DISABLE) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        String role = newuser.getRole();
        if (!Objects.equals(role, "admin")) {
            throw new AccountLockedException(MessageConstant.USER_TOKEN);
        }

        newuser.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(newuser);

        // 记录活动到Redis（通过Feign调用contest-service）
        try {
            WorkspaceActivityFeignDTO dto = new WorkspaceActivityFeignDTO();
            dto.setUserId(newuser.getId());
            dto.setActivityType(String.valueOf(ActivityType.USER_LOGIN));
            dto.setTitle("用户登录成功");
            dto.setDescription(newuser.getUsername() + "登录成功");
            dto.setTargetId(newuser.getId());
            dto.setTargetType("user");
            contestClient.recordWorkspaceActivity(dto);
        } catch (Exception e) {
            log.warn("记录用户登录活动失败: {}", e.getMessage());
        }

        return newuser;
    }

    @Override
    public void saveUser(UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setCreatedAt(LocalDateTime.now());
        user.setPasswordHash(PasswordConstant.DEFAULT_PASSWORD);
        user.setDailyQuestionStreak(0);
        user.setPoints(0);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public PageResult pageQuery(UserQueryDTO userQueryDTO) {
        String username = userQueryDTO.getUsername();
        String role = userQueryDTO.getRole();

        Page<User> userPage = Page.of(userQueryDTO.getPage(), userQueryDTO.getPageSize());
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper
                .like(username != null && !username.isEmpty(), User::getUsername, username)
                .eq(role != null && !role.isEmpty(), User::getRole, role);

        Page<User> userPage1 = userMapper.selectPage(userPage, userLambdaQueryWrapper);
        return new PageResult(userPage1.getTotal(), userPage1.getRecords());
    }

    @Override
    public User selectById(long id) {
        return userMapper.selectById(id);
    }

    @Override
    public void updateUser(UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void userStatus(Integer status, long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return;
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public UserLoginVo userlogin(User user) {
        String username = user.getUsername();
        String password = user.getPasswordHash();

        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getUsername, username)
                .eq(User::getPasswordHash, password);
        User newuser = userMapper.selectOne(lambdaQueryWrapper);

        if (newuser == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (newuser.getStatus() == StatusConstant.DISABLE) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        newuser.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(newuser);

        // 记录活动到Redis（通过Feign调用contest-service）
        try {
            WorkspaceActivityFeignDTO dto = new WorkspaceActivityFeignDTO();
            dto.setUserId(newuser.getId());
            dto.setActivityType(String.valueOf(ActivityType.USER_LOGIN));
            dto.setTitle("用户登录成功");
            dto.setDescription(newuser.getUsername() + "登录成功");
            dto.setTargetId(newuser.getId());
            dto.setTargetType("user");
            contestClient.recordWorkspaceActivity(dto);
        } catch (Exception e) {
            log.warn("记录用户登录活动失败: {}", e.getMessage());
        }

        // 随机生成令牌
        String token = UUID.randomUUID().toString();
        // 转为DTO, map
        UserLoginDTO newUser = BeanUtil.copyProperties(newuser, UserLoginDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(newUser, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName, filedValue) -> filedValue.toString()));
        // 存储
        redisTemplate.opsForHash().putAll(RedisConstant.LOGIN_USER_KEY + token, userMap);
        // 设置token有效期
        redisTemplate.expire(RedisConstant.LOGIN_USER_KEY + token, 30, TimeUnit.MINUTES);

        UserLoginVo userLoginVo = new UserLoginVo();
        userLoginVo.setId(newuser.getId());
        userLoginVo.setUserName(newuser.getUsername());
        userLoginVo.setName(newuser.getNickName());
        userLoginVo.setToken(token);
        return userLoginVo;
    }

    @Override
    public Result signout() {
        Long userId = BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstant.USER_SIGN_KEY + userId + keySuffix;
        int dayOfMonth = now.getDayOfMonth();
        redisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.success();
    }

    @Override
    public Result signCount() {
        Long userId = BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstant.USER_SIGN_KEY + userId + keySuffix;
        int dayOfMonth = now.getDayOfMonth();
        List<Long> result = redisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            return Result.success(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.success(0);
        }
        int count = 0;
        while (true) {
            if ((num & 1) == 0) {
                break;
            } else {
                count++;
            }
            num >>>= 1;
        }
        return Result.success(count);
    }

    @Override
    public Result signDays(int year, int month) {
        Long userId = BaseContext.getCurrentId();
        String keySuffix = String.format(":%04d%02d", year, month);
        String key = RedisConstant.USER_SIGN_KEY + userId + keySuffix;

        java.time.YearMonth yearMonth = java.time.YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();

        List<Long> result = redisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(daysInMonth)).valueAt(0)
        );

        List<Integer> signedDays = new ArrayList<>();
        if (result != null && !result.isEmpty()) {
            Long num = result.get(0);
            if (num != null && num != 0) {
                for (int day = 1; day <= daysInMonth; day++) {
                    if ((num & 1) == 1) {
                        signedDays.add(day);
                    }
                    num >>>= 1;
                }
            }
        }
        return Result.success(signedDays);
    }

    @Override
    public void updateUserSolvedCount(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPoints(user.getPoints() != null ? user.getPoints() + 1 : 1);
            userMapper.updateById(user);
        }
    }
}
