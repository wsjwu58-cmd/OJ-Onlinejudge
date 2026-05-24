package com.oj.ai.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class AiChatDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private Integer problemId;
    private String message;
    private String code;

    @Data
    public static class MessageHistory implements Serializable {
        private String role;
        private String content;
    }
}
