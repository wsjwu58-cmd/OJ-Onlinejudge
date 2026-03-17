package com.oj.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeSubmitDTO {
    /**
     * 题目ID
     */
    @NotNull(message = "题目ID不能为空")
    private Integer problemId;

    /**
     * 提交的源代码
     */
    @NotBlank(message = "代码不能为空")
    private String code;

    /**
     * 编程语言: Java, Python, C++, JavaScript
     */
    @NotBlank(message = "编程语言不能为空")
    private String language;

    /**
     * 比赛ID（普通练习提交时为null）
     */
    private Integer contestId;
}
