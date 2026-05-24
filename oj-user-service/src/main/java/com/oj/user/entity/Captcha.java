package com.oj.user.entity;

import lombok.Data;

@Data
public class Captcha {
    private String nonceStr;
    private String value;
    private String canvasSrc;
    private Integer canvasWidth;
    private Integer canvasHeight;
    private String blockSrc;
    private Integer blockWidth;
    private Integer blockHeight;
    private Integer blockRadius;
    private Integer blockX;
    private Integer blockY;
    private Integer place;
}
