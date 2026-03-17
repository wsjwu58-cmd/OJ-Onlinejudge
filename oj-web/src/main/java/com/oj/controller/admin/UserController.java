package com.oj.controller.admin;

import com.oj.constant.JwtClaimsConstant;
import com.oj.dto.UserDTO;
import com.oj.dto.UserQueryDTO;
import com.oj.entity.User;
import com.oj.properties.JwtProperties;
import com.oj.result.PageResult;
import com.oj.result.Result;
import com.oj.utils.JwtUtil;
import com.oj.vo.UserLoginVo;
import com.oj.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/user")
@Slf4j
@Tag(name = "用户相关接口")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param user
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "员工登录")
    public Result<UserLoginVo> login(@RequestBody User user) {
        log.info("员工登录：{}", user);

        User user1 = userService.login(user);
        if(user1.getStatus()==0){
            return Result.error("该账号被禁用");
        }
        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, user1.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        UserLoginVo userLoginVo = UserLoginVo.builder()
                .id(user1.getId())
                .userName(user1.getUsername())
                .name(user1.getNickName())
                .token(token)
                .build();

        return Result.success(userLoginVo);
    }
    @PostMapping("/add/admin")
    @Operation(summary = "新增用户")
    public Result save(@RequestBody UserDTO userDTO){
        log.info("新增用户,{}",userDTO);
        userService.saveUser(userDTO);
        return Result.success();
    }
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<PageResult> pageOf( UserQueryDTO userQueryDTO){
        log.info("分页查询：{}",userQueryDTO);
        PageResult pageResult=userService.pageQuery(userQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户")
    public Result<User> SelectById(@PathVariable long id){
        log.info("用户id:{}",id);
        User user=userService.selectById(id);
        return Result.success(user);
    }

    @PutMapping
    @Operation(summary = "编辑员工")
    public Result updateUser(@RequestBody UserDTO userDTO){
        log.info("编辑的员工信息：{}",userDTO);
        userService.updateUser(userDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}/{id}")
    @Operation(summary = "启用/停用账号")
    public Result UserStatus(@PathVariable Integer status,@PathVariable Long id){
        log.info("用户id:{}",id);
        userService.userStatus(status,id);
        return Result.success();
    }



}