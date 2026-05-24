package com.oj.ai.service;

import com.oj.ai.dto.AiChatDTO;
import com.oj.ai.dto.AiJudgeDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiJudgeService {
    String judgeCode(AiJudgeDTO dto);
    String checkSyntax(String code, String language);
    String analyzeError(String code, String errorMessage, String language);
    String getHint(String problemTitle, String problemContent, String language);
    String chat(String message);
    SseEmitter judgeByAiStream(AiJudgeDTO dto, Long userId);
    SseEmitter syntaxCheck(AiJudgeDTO dto);
    SseEmitter analyzeError(AiJudgeDTO dto, String errorInfo);
    SseEmitter chat(AiChatDTO dto);
    SseEmitter getHint(AiChatDTO dto);
}
