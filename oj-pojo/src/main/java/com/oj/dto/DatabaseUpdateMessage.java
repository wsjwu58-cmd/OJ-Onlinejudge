package com.oj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库更新 MQ 消息体（发往 database-update-topic）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseUpdateMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 题目ID */
    private Integer problemId;

    /** 提交的源代码 */
    private String code;

    /** 编程语言 */
    private String language;

    /** 判题状态: Accepted / Wrong Answer / TLE 等 */
    private String status;

    /** 运行耗时 (ms) */
    private Integer runtimeMs;

    /** 内存消耗 (KB) */
    private Integer memoryKb;

    /** 通过测试用例数 */
    private Integer testCasesPassed;

    /** 总测试用例数 */
    private Integer testCasesTotal;

    /** 错误信息 */
    private String errorInfo;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 是否首次AC（用于决定是否更新MySQL解题数） */
    private boolean firstAc;

    /** Redis 中本次提交的唯一标识 token */
    private String submissionToken;

    /** 比赛ID（普通练习时为null） */
    private Integer contestId;
}
