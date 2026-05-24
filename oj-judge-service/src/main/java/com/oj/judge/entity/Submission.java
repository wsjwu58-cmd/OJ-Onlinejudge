package com.oj.judge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("submissions")
public class Submission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer problemId;
    private Integer contestId;
    private String code;
    private String language;
    private String status;
    private Integer runtimeMs;
    private Integer memoryKb;
    private Integer testCasesPassed;
    private Integer testCasesTotal;
    private String errorInfo;
    private String ipAddress;
    private LocalDateTime submitTime;
}
