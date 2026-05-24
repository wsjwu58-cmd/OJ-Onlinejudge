package com.oj.judge.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResultVO implements Serializable {
    private Long submissionId;
    private String status;
    private Integer runtimeMs;
    private Integer memoryKb;
    private Integer testCasesPassed;
    private Integer testCasesTotal;
    private String stdout;
    private String errorInfo;
    private LocalDateTime submitTime;
    private String title;
    private Integer problemId;
    private String submitToken;
}
