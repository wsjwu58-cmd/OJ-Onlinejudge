package com.oj.judge.dto;

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
public class DatabaseUpdateMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private Integer problemId;
    private String code;
    private String language;
    private String status;
    private Integer runtimeMs;
    private Integer memoryKb;
    private Integer testCasesPassed;
    private Integer testCasesTotal;
    private String errorInfo;
    private LocalDateTime submitTime;
    private boolean firstAc;
    private String submissionToken;
    private Integer contestId;
}
