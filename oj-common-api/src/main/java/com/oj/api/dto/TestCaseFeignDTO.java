package com.oj.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TestCaseFeignDTO implements Serializable {
    private Integer id;
    private Integer problemId;
    private String inputData;
    private String outputData;
    private Boolean isSample;
    private Integer orderNum;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private BigDecimal scoreWeight;
    private Integer status;
}
