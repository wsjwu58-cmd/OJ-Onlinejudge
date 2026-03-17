package com.oj.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.oj.entity.ProblemType;
import com.oj.entity.TestCase;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Data
public class ProblemVO {
    //主键值
    private Long id;
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
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /*
     * 通过率 (%)，缓存字段
     */
    private BigDecimal acceptance;

    //测试用例
    private List<TestCase> testCaseList;

}
