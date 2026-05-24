package com.oj.user.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.oj.user.config.CaptchaUtils;
import com.oj.user.entity.Captcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CaptchaServiceImpl implements com.oj.user.service.CaptchaService {
    private static Integer ALLOW_DEVIATION = 3;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String checkImageCode(String imageKey, String imageCode) {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String text = ops.get("imageCode:" + imageKey);
        if (StrUtil.isBlank(text)) {
            return "验证码已失效";
        }
        if (Math.abs(Integer.parseInt(text) - Integer.parseInt(imageCode)) > ALLOW_DEVIATION) {
            return "验证失败，请控制拼图对齐缺口";
        }
        return null;
    }

    @Override
    public void saveImageCode(String key, String code) {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("imageCode:" + key, code, 15, TimeUnit.MINUTES);
    }

    @Override
    public Object getCaptcha(Captcha captcha) {
        CaptchaUtils.checkCaptcha(captcha);
        int canvasWidth = captcha.getCanvasWidth();
        int canvasHeight = captcha.getCanvasHeight();
        int blockWidth = captcha.getBlockWidth();
        int blockHeight = captcha.getBlockHeight();
        int blockRadius = captcha.getBlockRadius();
        BufferedImage canvasImage = CaptchaUtils.getBufferedImage(captcha.getPlace());
        canvasImage = CaptchaUtils.imageResize(canvasImage, canvasWidth, canvasHeight);
        int blockX = CaptchaUtils.getNonceByRange(blockWidth, canvasWidth - blockWidth - 10);
        int blockY = CaptchaUtils.getNonceByRange(10, canvasHeight - blockHeight + 1);
        BufferedImage blockImage = new BufferedImage(blockWidth, blockHeight, BufferedImage.TYPE_4BYTE_ABGR);
        CaptchaUtils.cutByTemplate(canvasImage, blockImage, blockWidth, blockHeight, blockRadius, blockX, blockY);
        String nonceStr = UUID.randomUUID().toString().replaceAll("-", "");
        saveImageCode(nonceStr, String.valueOf(blockX));
        captcha.setNonceStr(nonceStr);
        captcha.setBlockY(blockY);
        captcha.setBlockSrc(CaptchaUtils.toBase64(blockImage, "png"));
        captcha.setCanvasSrc(CaptchaUtils.toBase64(canvasImage, "png"));
        return captcha;
    }
}
