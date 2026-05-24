package com.oj.ai.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class AiJudgeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private Integer problemId;
    private String code;
    private String language;
}
