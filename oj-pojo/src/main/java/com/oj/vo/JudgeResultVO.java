package com.oj.vo;

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
public class JudgeResultVO implements Serializable {


    /** 提交记录ID（运行模式为null） */
    private Long submissionId;

    /**
     * 状态: Accepted, Wrong Answer, Time Limit Exceeded,
     * Memory Limit Exceeded, Runtime Error, Compile Error, Pending, Judging
     */
    private String status;

    /** 运行耗时 (ms) */
    private Integer runtimeMs;

    /** 内存消耗 (KB) */
    private Integer memoryKb;

    /** 通过测试用例数 */
    private Integer testCasesPassed;

    /** 总测试用例数 */
    private Integer testCasesTotal;

    /** 标准输出（运行模式） */
    private String stdout;

    /** 编译/运行错误信息 */
    private String errorInfo;

    /** 提交时间 */
    private LocalDateTime submitTime;

    //题目标题
    private String title;

    /**
     * 题目ID
     */
    private Integer problemId;

    private String submitToken;
}
