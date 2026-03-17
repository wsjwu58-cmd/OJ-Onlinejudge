package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题目类型表
 */
@Data
@TableName("problem_types")

public class ProblemType {
    /**
     * 类型ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 类型名称 (如: 数组, 动态规划, 数据库, 链表)
     */
    private String name;

    /**
     * 类型描述
     */
    private String description;

    /**
     * 是否激活
     */
    private Integer isActive;

    /**
     * 排序权重，数值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
