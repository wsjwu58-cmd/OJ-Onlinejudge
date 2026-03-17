package com.oj.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class ProblemTypeQueryDTO extends PageQueryDTO implements Serializable {
    /**
     * 类型名称 (如: 数组, 动态规划, 数据库, 链表)
     */
    private String name;
}
