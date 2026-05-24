package com.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_problems")
public class GroupProblems {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer groupId;
    private Integer problemId;
    private Integer sortOrder;
    private Integer score;
    private LocalDateTime createdAt;
}
