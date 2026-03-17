package com.oj.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.oj.entity.ProblemType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProblemDTO {
    /*
    题目ID
     */
    private Integer id;
    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目描述 (Markdown格式)
     */
    private String content;

    /**
     * 题目难度: Easy-简单, Medium-中等, Hard-困难
     */
    private String difficulty;
    /**
     * 题目大类: Algorithm-算法, Database-数据库, Shell-Shell, Concurrency-并发
     */
    private String problemType;

    /**
     * 时间限制 (毫秒)
     */
    private Integer timeLimitMs;

    /**
     * 内存限制 (MB)
     */
    private Integer memoryLimitMb;
    /**
     * 题目状态: 1-上架, 0-下架
     */
    private Integer status;

    /**
     * 不同语言的代码模板 (JSON格式)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String,String> templateCode;

    /**
     * 数据库题的表结构SQL
     */
    private String dbSchema;

    /**
     * 数据库题的初始化数据 (JSON格式)
     */
    private String dbInitData;

    /*
    分类
     */
    private List<ProblemType> typeList;


}
