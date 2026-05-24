package com.oj.user.service;

import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.user.dto.UserDTO;
import com.oj.user.dto.UserQueryDTO;
import com.oj.user.entity.User;
import com.oj.user.vo.UserLoginVo;

public interface UserService {
    User login(User user);

    void saveUser(UserDTO userDTO);

    PageResult pageQuery(UserQueryDTO userQueryDTO);

    User selectById(long id);

    void updateUser(UserDTO userDTO);

    void userStatus(Integer status, long id);

    UserLoginVo userlogin(User user);

    Result signout();

    Result signCount();

    Result signDays(int year, int month);

    void updateUserSolvedCount(Long id);
}
