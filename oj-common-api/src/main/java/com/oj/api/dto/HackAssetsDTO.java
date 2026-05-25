package com.oj.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class HackAssetsDTO implements Serializable {
    private String validatorPath;
    private String validatorExePath;
    private String validatorSrcHash;
    private String referencePath;
    private String referenceLanguage;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
}
