package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题组题目关联表
 */
@Data
@TableName("group_problems")
public class GroupProblems {
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 题组ID，关联 problem_groups.id
     */
    private Integer groupId;

    /**
     * 题目ID，关联 problems.id
     */
    private Integer problemId;

    /**
     * 题目在题组中的顺序，数值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 该题在题组中的分数
     */
    private Integer score;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
