package com.oj.judge.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionVO {
    private Long id;
    private Long userId;
    private String username;
    private Integer problemId;
    private String problemTitle;
    private Integer contestId;
    private String code;
    private String language;
    private String status;
    private Integer runtimeMs;
    private Integer memoryKb;
    private Integer testCasesPassed;
    private Integer testCasesTotal;
    private String errorInfo;
    private String ipAddress;
    private LocalDateTime submitTime;
}
