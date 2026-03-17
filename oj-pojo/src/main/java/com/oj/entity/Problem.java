package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 题目表
 */
@Data
@TableName(value = "problems", autoResultMap = true)
public class Problem {
    /**
     * 题目ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目描述 (Markdown格式)
     */
    private String content;

    @TableField(exist = false)
    private String contentHtml; // 存储解析后的 HTML 文本 (新增属性)

    /**
     * 题目难度: Easy-简单, Medium-中等, Hard-困难
     */
    private String difficulty;

    /**
     * 通过率 (%)，缓存字段
     */
    private BigDecimal acceptance;

    /**
     * 出现频率: Low-低, Medium-中, High-高
     */
    private String frequency;

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
     * 点赞数
     */
    private Integer likesCount;

    /**
     * 点踩数
     */
    private Integer dislikesCount;

    /**
     * 题目状态: 1-上架, 0-下架
     */
    private Integer status;

    /**
     * 不同语言的代码模板 (JSON格式)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> templateCode;

    /**
     * 数据库题的表结构SQL
     */
    private String dbSchema;

    /**
     * 数据库题的初始化数据 (JSON格式)
     */
    private String dbInitData;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /*
    分数
     */
    @TableField(exist = false)
    private Integer score;
}
