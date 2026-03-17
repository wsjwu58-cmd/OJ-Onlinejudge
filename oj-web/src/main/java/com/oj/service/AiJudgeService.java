package com.oj.service;

import com.oj.dto.AiChatDTO;
import com.oj.dto.AiJudgeDTO;
import com.oj.vo.JudgeResultVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiJudgeService {

    SseEmitter judgeByAiStream(AiJudgeDTO dto, Long userId);

    SseEmitter syntaxCheck(AiJudgeDTO dto);

    SseEmitter analyzeError(AiJudgeDTO dto, JudgeResultVO judgeResult);

    SseEmitter chat(AiChatDTO dto);

    SseEmitter getHint(AiChatDTO dto);
}
