package com.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("problem_groups")
public class ProblemGroup {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String description;
    private Long creatorId;
    private String difficultyRange;
    private Integer estimatedDurationMinutes;
    private Boolean isPublic;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer joinCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
