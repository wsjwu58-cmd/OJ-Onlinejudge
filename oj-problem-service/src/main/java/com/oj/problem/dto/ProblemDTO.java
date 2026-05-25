package com.oj.problem.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.oj.problem.entity.ProblemType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProblemDTO {
    private Integer id;
    private String title;
    private String content;
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
    private String validatorCode;
    private String referenceCode;
    private List<ProblemType> typeList;
}
