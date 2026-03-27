package com.oj.dto;

import lombok.Data;

@Data
public class AgentRequestDTO {
    private String task;
    private String context;
    private Long userId;
    private String sessionId;
    private Integer problemId;
    private String code;
    private String language;
}
