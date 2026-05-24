package com.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("problem_types_rel")
public class ProblemTypesRel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer problemId;
    private Integer typeId;
    private LocalDateTime createdAt;
}
