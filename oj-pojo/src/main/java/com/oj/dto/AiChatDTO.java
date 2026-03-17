package com.oj.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiChatDTO {
    private Integer problemId;
    
    private String code;
    
    private String language;
    
    private String message;
    
    private List<MessageHistory> history;

    private Long userId;
    
    @Data
    public static class MessageHistory {
        private String role;
        private String content;
    }
}
