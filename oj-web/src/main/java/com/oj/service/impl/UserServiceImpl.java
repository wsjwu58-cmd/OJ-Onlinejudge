package com.oj.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.constant.MessageConstant;
import com.oj.constant.PasswordConstant;
import com.oj.constant.RedisConstant;
import com.oj.constant.StatusConstant;
import com.oj.context.BaseContext;
import com.oj.dto.UserDTO;
import com.oj.dto.UserLoginDTO;
import com.oj.dto.UserQueryDTO;
import com.oj.entity.User;
import com.oj.enumeration.ActivityType;
import com.oj.exception.AccountLockedException;
import com.oj.exception.AccountNotFoundException;
import com.oj.mapper.UserMapper;
import com.oj.result.PageResult;
import com.oj.result.Result;
import com.oj.service.UserService;
import com.oj.service.WorkSpaceService;
import com.oj.vo.UserLoginVo;
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
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkSpaceService workSpaceService;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public User login(User user) {
        String username = user.getUsername();
        String password = user.getPasswordHash();


        //1、根据用户名查询数据库中的数据
        LambdaQueryWrapper<User> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getUsername,username);
        User newuser = userMapper.selectOne(lambdaQueryWrapper);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (newuser == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }


        //密码比对
        // TODO 后期需要进行md5加密，然后再进行比对
//        password = DigestUtils.md5DigestAsHex(password.getBytes());
//        if (!password.equals(newuser.getPasswordHash())) {
//            //密码错误
//            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
//        }

        if (newuser.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        String role=newuser.getRole();
        if(!Objects.equals(role, "admin")){
            throw new AccountLockedException(MessageConstant.USER_TOKEN);
        }
        //修改登录时间

        newuser.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(newuser);

        //记录加入redis
        workSpaceService.recordWorkSpace(newuser.getId(), String.valueOf(ActivityType.USER_LOGIN),"用户登录成功",
                newuser.getUsername()+"登录成功",newuser.getId(),"user");

        //3、返回实体对象
        return newuser;
    }

    @Override
    public void saveUser(UserDTO userDTO) {
        User user=new User();
        BeanUtils.copyProperties(userDTO,user);
        //设置创建日期，默认密码，分数和答题数
        user.setCreatedAt(LocalDateTime.now());
        user.setPasswordHash(PasswordConstant.DEFAULT_PASSWORD);
        user.setDailyQuestionStreak(0);
        user.setPoints(0);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public PageResult pageQuery(UserQueryDTO userQueryDTO) {
        // 获取用户名
        String username = userQueryDTO.getUsername();
        String role = userQueryDTO.getRole(); // 可能是 "" 或 null

        // 构造分页条件
        Page<User> userPage = Page.of(userQueryDTO.getPage(), userQueryDTO.getPageSize());
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();

        // 使用 StringUtils.hasText() 或者手动判断 null 和 empty
        userLambdaQueryWrapper
                .like(username != null && !username.isEmpty(), User::getUsername, username) // username 不为 null 且不为空字符串时才添加条件
                .eq(role != null && !role.isEmpty(), User::getRole, role); // 修改这里：role 不为 null 且不为空字符串时才添加条件

        Page<User> userPage1 = userMapper.selectPage(userPage, userLambdaQueryWrapper);

        // 返回结果
        return new PageResult(userPage1.getTotal(), userPage1.getRecords());
    }

    @Override
    public User selectById(long id) {
        return userMapper.selectById(id);
    }

    @Override
    public void updateUser(UserDTO userDTO) {
        User user=new User();
        BeanUtils.copyProperties(userDTO,user);
        //设置其他变量
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void userStatus(Integer status, long id) {
        User user=userMapper.selectById(id);
        if(user==null){
            return;
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public UserLoginVo userlogin(User user) {
        String username = user.getUsername();
        String password = user.getPasswordHash();


        //1、根据用户名查询数据库中的数据
        LambdaQueryWrapper<User> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getUsername,username)
                .eq(User::getPasswordHash,password);
        User newuser = userMapper.selectOne(lambdaQueryWrapper);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (newuser == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (newuser.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        //修改登录时间
        newuser.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(newuser);
        //记录加入redis
        workSpaceService.recordWorkSpace(newuser.getId(), String.valueOf(ActivityType.USER_LOGIN),"用户登录成功",
                newuser.getUsername()+"登录成功",newuser.getId(),"user");
        //随机生成令牌
        String token = UUID.randomUUID().toString();
        //转为DTO,map
        UserLoginDTO newUser= BeanUtil.copyProperties(newuser, UserLoginDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(newUser,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName, filedValue)->filedValue.toString()));
        //存储
        redisTemplate.opsForHash().putAll(RedisConstant.LOGIN_USER_KEY+token,userMap);
        //设置token有效期
        redisTemplate.expire(RedisConstant.LOGIN_USER_KEY+token,30, TimeUnit.MINUTES);

        UserLoginVo userLoginVo=new UserLoginVo();
        userLoginVo.setId(newuser.getId());
        userLoginVo.setUserName(newuser.getUsername());
        userLoginVo.setName(newuser.getNickName());
        userLoginVo.setToken(token);
        return userLoginVo;
    }

    @Override
    public Result signout() {
        //获取用户
        Long userId= BaseContext.getCurrentId();
        //获取日期
        LocalDateTime now=LocalDateTime.now();
        String keySuffix=now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key=RedisConstant.USER_SIGN_KEY+userId+keySuffix;
        //获取天数
        int dayOfMonth = now.getDayOfMonth();
        redisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.success();

    }

    @Override
    public Result signCount() {
        // 1.获取当前登录用户
        Long userId = BaseContext.getCurrentId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstant.USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = redisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.success(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.success(0);
        }
        // 6.循环遍历
        int count = 0;
        while (true) {
            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            }else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.success(count);
    }
}
