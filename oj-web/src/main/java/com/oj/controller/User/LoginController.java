package com.oj.controller.User;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.oj.entity.Captcha;
import com.oj.entity.User;
import com.oj.result.Result;
import com.oj.service.UserService;
import com.oj.service.impl.CaptchaService;
import com.oj.vo.UserLoginVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/userLogin")
@Slf4j
@Tag(name = "登录接口")
public class LoginController {
    @Autowired
    private UserService userService;
    @Autowired
    private CaptchaService captchaService;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result<UserLoginVo> UserLogin(@RequestBody User user){
        log.info("登录用户：{}",user);
        String msg = captchaService.checkImageCode(user.getNonceStr(),user.getValue());
        if (StringUtils.isNotBlank(msg)) {
            return Result.error(msg);
        }
        UserLoginVo login = userService.userlogin(user);
        return Result.success(login);
    }

    @Operation(summary = "生成验证码拼图")
    @PostMapping("/get-captcha")
    public Result getCaptcha(@RequestBody Captcha captcha) {
        return Result.success(captchaService.getCaptcha(captcha));
    }

}
