package com.oj.service;

import com.oj.dto.UserDTO;
import com.oj.dto.UserQueryDTO;
import com.oj.entity.User;
import com.oj.result.PageResult;
import com.oj.result.Result;
import com.oj.vo.UserLoginVo;

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
}
