package com.oj.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "提交记录返回数据")
public class SubmissionVO {

    @Schema(description = "提交ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "题目ID")
    private Integer problemId;

    @Schema(description = "题目标题")
    private String problemTitle;

    @Schema(description = "比赛ID")
    private Integer contestId;

    @Schema(description = "提交的代码")
    private String code;

    @Schema(description = "编程语言")
    private String language;

    @Schema(description = "判题状态")
    private String status;

    @Schema(description = "运行时间 (毫秒)")
    private Integer runtimeMs;

    @Schema(description = "内存消耗 (KB)")
    private Integer memoryKb;

    @Schema(description = "通过的测试用例数")
    private Integer testCasesPassed;

    @Schema(description = "总测试用例数")
    private Integer testCasesTotal;

    @Schema(description = "错误信息")
    private String errorInfo;

    @Schema(description = "提交IP地址")
    private String ipAddress;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;
}
