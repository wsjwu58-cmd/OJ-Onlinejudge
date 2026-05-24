package com.oj.judge.dto;

import com.oj.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SubmissionQueryDTO extends PageQueryDTO {
    private Integer problemId;
    private Long userId;
    private String problemTitle;
    private String username;
    private String status;
}
