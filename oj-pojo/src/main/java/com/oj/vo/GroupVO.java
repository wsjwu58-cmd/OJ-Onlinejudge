package com.oj.vo;

import com.oj.entity.Problem;
import lombok.Data;

import java.util.List;
@Data
public class GroupVO {
    /**
     * 题组ID
     */

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
    /*
     * 题目
     */
    private List<Problem> problemList;
}
