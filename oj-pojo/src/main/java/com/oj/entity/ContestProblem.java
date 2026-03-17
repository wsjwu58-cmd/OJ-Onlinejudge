package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 比赛题目关联表
 */
@Data
@TableName("contest_problems")
public class ContestProblem {
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 比赛ID
     */
    private Integer contestId;

    /**
     * 题目ID
     */
    private Integer problemId;

    /**
     * 该题分数
     */
    private Integer score;

    /**
     * 题目在比赛中的顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
