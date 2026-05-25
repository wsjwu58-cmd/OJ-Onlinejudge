package com.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "problems", autoResultMap = true)
public class Problem {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String content;

    @TableField(exist = false)
    private String contentHtml;

    private String difficulty;
    private BigDecimal acceptance;
    private String frequency;
    private String problemType;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private Integer likesCount;
    private Integer dislikesCount;
    private Integer status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> templateCode;

    private String dbSchema;
    private String dbInitData;

    private String validatorPath;
    private String validatorExePath;
    private String validatorSrcHash;
    private String referencePath;
    private String referenceLanguage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Integer score;
}
