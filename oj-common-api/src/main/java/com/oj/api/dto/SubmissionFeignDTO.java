package com.oj.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SubmissionFeignDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Integer problemId;
    private Integer contestId;
    private String code;
    private String language;
    private String status;
    private Integer runtimeMs;
    private Integer memoryKb;
    private LocalDateTime submitTime;
}
