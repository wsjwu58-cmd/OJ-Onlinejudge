package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题目类型关联表
 */
@Data
@TableName("problem_types_rel")
public class ProblemTypesRel {
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题目ID
     */
    private Integer problemId;

    /**
     * 类型ID
     */
    private Integer typeId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
