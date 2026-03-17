package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题组表
 */
@Data
@TableName("problem_groups")
public class ProblemGroup {
    /**
     * 题组ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 题组标题
     */
    private String title;

    /**
     * 题组描述
     */
    private String description;

    /**
     * 创建者ID，关联 users.id
     */
    private Long creatorId;

    /**
     * 难度范围 (如: Easy-Medium, All)
     */
    private String difficultyRange;

    /**
     * 预计完成时长 (分钟)
     */
    private Integer estimatedDurationMinutes;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 题组状态: 1-启用, 0-禁用
     */
    private Integer status;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 加入/练习人数
     */
    private Integer joinCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
