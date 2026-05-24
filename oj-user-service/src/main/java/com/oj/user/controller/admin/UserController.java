package com.oj.user.controller.admin;

import com.oj.common.constant.JwtClaimsConstant;
import com.oj.common.properties.JwtProperties;
import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.common.utils.JwtUtil;
import com.oj.user.dto.UserDTO;
import com.oj.user.dto.UserQueryDTO;
import com.oj.user.entity.User;
import com.oj.user.service.UserService;
import com.oj.user.vo.UserLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/user")
@Slf4j
@Tag(name = "管理端-用户相关接口")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public Result<UserLoginVo> login(@RequestBody User user) {
        log.info("管理员登录：{}", user);
        User user1 = userService.login(user);
        if (user1.getStatus() == 0) {
            return Result.error("该账号被禁用");
        }
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
    public Result save(@RequestBody UserDTO userDTO) {
        log.info("新增用户,{}", userDTO);
        userService.saveUser(userDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<PageResult> pageOf(UserQueryDTO userQueryDTO) {
        log.info("分页查询：{}", userQueryDTO);
        PageResult pageResult = userService.pageQuery(userQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户")
    public Result<User> SelectById(@PathVariable long id) {
        log.info("用户id:{}", id);
        User user = userService.selectById(id);
        return Result.success(user);
    }

    @PutMapping
    @Operation(summary = "编辑用户")
    public Result updateUser(@RequestBody UserDTO userDTO) {
        log.info("编辑的用户信息：{}", userDTO);
        userService.updateUser(userDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}/{id}")
    @Operation(summary = "启用/停用账号")
    public Result UserStatus(@PathVariable Integer status, @PathVariable Long id) {
        log.info("用户id:{}", id);
        userService.userStatus(status, id);
        return Result.success();
    }
}
