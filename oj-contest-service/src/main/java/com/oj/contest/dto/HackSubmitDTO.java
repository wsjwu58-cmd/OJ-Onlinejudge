package com.oj.contest.dto;

import lombok.Data;

@Data
public class HackSubmitDTO {
    private Integer contestId;
    private Integer problemId;
    private Long targetUserId;
    private Long targetSubmissionId;
    private String hackInput;
}
