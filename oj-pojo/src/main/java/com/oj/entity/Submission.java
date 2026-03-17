package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户提交记录表
 */
@Data
@TableName("submissions")
public class Submission {
    /**
     * 提交ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 题目ID
     */
    private Integer problemId;

    /**
     * 比赛ID（NULL表示普通练习）
     */
    private Integer contestId;

    /**
     * 提交的代码
     */
    private String code;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 判题状态: Pending-等待中, Judging-判题中, Accepted-通过, Wrong Answer-答案错误, 
     * Time Limit Exceeded-超时, Memory Limit Exceeded-内存超限, Runtime Error-运行错误, Compile Error-编译错误
     */
    private String status;

    /**
     * 运行时间 (毫秒)
     */
    private Integer runtimeMs;

    /**
     * 内存消耗 (KB)
     */
    private Integer memoryKb;

    /**
     * 通过的测试用例数
     */
    private Integer testCasesPassed;

    /**
     * 总测试用例数
     */
    private Integer testCasesTotal;

    /**
     * 错误信息
     */
    private String errorInfo;

    /**
     * 提交IP地址
     */
    private String ipAddress;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;


}
