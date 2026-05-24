package com.oj.user.service;

import com.oj.user.entity.Captcha;

public interface CaptchaService {
    String checkImageCode(String imageKey, String imageCode);

    void saveImageCode(String key, String code);

    Object getCaptcha(Captcha captcha);
}
