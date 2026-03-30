package com.oj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "提交记录查询条件")
public class SubmissionQueryDTO extends PageQueryDTO implements Serializable {

    @Schema(description = "题目ID")
    private Integer problemId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "题目名称")
    private String problemTitle;

    @Schema(description = "用户名称")
    private String username;

    @Schema(description = "判题状态: Accepted-通过, 其他状态为未通过")
    private String status;
}
