package com.oj.judge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeTaskMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String judge0Token;
    private Long userId;
    private Integer problemId;
    private String code;
    private String language;
    private String submissionToken;
    private int languageId;
    private int testCasesTotal;
    private Float timeLimitSec;
    private Integer memoryLimitKb;
    private Integer contestId;
}
