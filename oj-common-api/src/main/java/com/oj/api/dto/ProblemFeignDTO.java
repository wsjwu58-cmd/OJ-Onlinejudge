package com.oj.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ProblemFeignDTO implements Serializable {
    private Integer id;
    private String title;
    private String content;
    private String difficulty;
    private BigDecimal acceptance;
    private String frequency;
    private String problemType;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private String templateCode;
    private String dbSchema;
    private String dbInitData;
    private Integer status;
}
