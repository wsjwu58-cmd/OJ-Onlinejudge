package com.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Data
public class JudgeRunDTO {
    @NotNull(message = "题目ID不能为空")
    private Integer problemId;

    @NotBlank(message = "代码不能为空")
    private String code;

    @NotBlank(message = "编程语言不能为空")
    private String language;

    /**
     * 用户自定义输入（可选，不传则用第一组示例用例）
     */
    private String customInput;
}
