package com.oj.problem.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.oj.problem.entity.ProblemType;
import com.oj.problem.entity.TestCase;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProblemVO {
    private Long id;
    private String title;
    private String content;
    private String contentHtml;
    private String difficulty;
    private String problemType;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
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
    private List<ProblemType> typeList;
    private LocalDateTime createdAt;
    private BigDecimal acceptance;
    private List<TestCase> testCaseList;
}
