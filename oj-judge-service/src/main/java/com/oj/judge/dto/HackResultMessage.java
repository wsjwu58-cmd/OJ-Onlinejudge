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
public class HackResultMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long hackId;
    private Integer contestId;
    private Integer problemId;
    private Long hackerId;
    private Long targetUserId;
    private Long targetSubmissionId;
    private String status;
    private String targetResult;
    private String hackOutput;
    private String hackInput;
    private String errorInfo;
}
