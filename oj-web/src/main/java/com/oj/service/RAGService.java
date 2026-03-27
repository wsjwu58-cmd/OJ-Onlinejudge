package com.oj.service;

import com.oj.dto.AiChatDTO;
import com.oj.dto.AiJudgeDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface RAGService {
    SseEmitter chatWithKnowledge(AiChatDTO dto);
    SseEmitter analyzeErrorWithKnowledge(AiJudgeDTO dto, String errorInfo);
    SseEmitter getHintWithKnowledge(AiChatDTO dto);
}