package com.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkDataVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 总用户数
     */
    private Integer totalUsers;

    /**
     * 今日活跃用户数
     */
    private Integer activeUsersToday;

    /**
     * 今日提交数
     */
    private Integer submissionsToday;

    /**
     * 题目总数
     */
    private Integer totalProblems;

    /**
     * 用户增长率（百分比）
     */
    private Double userChange;

    /**
     * 活跃用户变化率（百分比）
     */
    private Double activeChange;

    /**
     * 提交数变化率（百分比）
     */
    private Double submissionChange;

    /**
     * 题目数变化率（百分比）
     */
    private Double problemChange;
}
