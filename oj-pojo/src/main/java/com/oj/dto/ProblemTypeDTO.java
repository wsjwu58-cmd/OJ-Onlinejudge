package com.oj.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProblemTypeDTO implements Serializable {
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
    private Boolean isActive;

    /**
     * 排序权重，数值越小越靠前
     */
    private Integer sortOrder;
}
