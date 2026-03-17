package com.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiJudgeDTO {
    @NotNull(message = "题目ID不能为空")
    private Integer problemId;

    @NotBlank(message = "代码不能为空")
    private String code;

    @NotBlank(message = "编程语言不能为空")
    private String language;

    private Long userId;
}
