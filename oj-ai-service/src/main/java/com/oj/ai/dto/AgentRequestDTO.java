package com.oj.ai.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class AgentRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId;
    private String task;
    private String context;
    private Long userId;
    private Integer problemId;
    private String agentType; // "traditional" or "langgraph"
}
