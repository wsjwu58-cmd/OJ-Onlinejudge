package com.oj.contest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contest_problems")
public class ContestProblem {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer contestId;
    private Integer problemId;
    private Integer score;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
