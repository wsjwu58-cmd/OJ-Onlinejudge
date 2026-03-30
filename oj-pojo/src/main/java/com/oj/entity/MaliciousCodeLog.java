package com.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("malicious_code_log")
public class MaliciousCodeLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer problemId;

    private String language;

    private String code;

    private String detectionReason;

    private String matchedPattern;

    private Integer severity;

    private String ipAddress;

    private LocalDateTime createTime;

    private String userAgent;
}
