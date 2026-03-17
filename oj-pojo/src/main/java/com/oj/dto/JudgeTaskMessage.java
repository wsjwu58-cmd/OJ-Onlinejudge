package com.oj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 判题任务 MQ 消息体（发往 judge-task-topic）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeTaskMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Judge0 返回的 token */
    private String judge0Token;

    /** 用户ID */
    private Long userId;

    /** 题目ID */
    private Integer problemId;

    /** 提交的源代码 */
    private String code;

    /** 编程语言 */
    private String language;

    /** Redis 中本次提交的唯一标识 token */
    private String submissionToken;

    /** Judge0 语言ID */
    private int languageId;

    /** 测试用例总数 */
    private int testCasesTotal;

    /** 题目时间限制 (秒) */
    private Float timeLimitSec;

    /** 题目内存限制 (KB) */
    private Integer memoryLimitKb;

    /** 比赛ID（普通练习时为null） */
    private Integer contestId;
}
