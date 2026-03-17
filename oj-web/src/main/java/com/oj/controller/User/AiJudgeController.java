package com.oj.controller.User;

import com.alibaba.fastjson.JSON;
import com.oj.context.BaseContext;
import com.oj.dto.AiChatDTO;
import com.oj.dto.AiJudgeDTO;
import com.oj.dto.JudgeSubmitDTO;
import com.oj.result.Result;
import com.oj.service.AiJudgeService;
import com.oj.service.JudgeService;
import com.oj.vo.JudgeResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/user/ai")
@Tag(name = "用户端-AI判题接口")
public class AiJudgeController {

    @Autowired
    private AiJudgeService aiJudgeService;

    @Autowired
    private JudgeService judgeService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String AI_JUDGE_PREFIX = "ai_judge:";
    private static final String AI_SYNTAX_PREFIX = "ai_syntax:";
    private static final String AI_ERROR_PREFIX = "ai_error:";
    private static final String AI_CHAT_PREFIX = "ai_chat:";
    private static final String AI_HINT_PREFIX = "ai_hint:";
    private static final long TASK_EXPIRE_MINUTES = 5;

    @PostMapping("/judge/submit")
    @Operation(summary = "提交AI判题任务")
    public Result<String> submitJudge(@Valid @RequestBody AiJudgeDTO dto) {
        log.info("当前AI用户id:{}",dto.getUserId());
        String token = UUID.randomUUID().toString();
        dto.setUserId(BaseContext.getCurrentId());
        
        stringRedisTemplate.opsForValue().set(
            AI_JUDGE_PREFIX + token,
            JSON.toJSONString(dto),
            TASK_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );
        
        log.info("AI判题任务已提交: token={}, problemId={}", token, dto.getProblemId());
        return Result.success(token);
    }

    @GetMapping(value = "/judge/stream/{token}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式获取AI判题结果")
    public SseEmitter streamJudge(@PathVariable String token) {
        String json = stringRedisTemplate.opsForValue().get(AI_JUDGE_PREFIX + token);
        
        if (json == null) {
            SseEmitter emitter = new SseEmitter(1000L);
            try {
                emitter.send(SseEmitter.event().data("任务不存在或已过期"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        stringRedisTemplate.delete(AI_JUDGE_PREFIX + token);
        
        AiJudgeDTO dto = JSON.parseObject(json, AiJudgeDTO.class);
        Long userId = dto.getUserId() != null ? dto.getUserId() : BaseContext.getCurrentId();
        
        log.info("开始AI判题流: token={}, problemId={}", token, dto.getProblemId());
        return aiJudgeService.judgeByAiStream(dto, userId);
    }

    @PostMapping("/syntax-check/submit")
    @Operation(summary = "提交语法检测任务")
    public Result<String> submitSyntaxCheck(@Valid @RequestBody AiJudgeDTO dto) {
        String token = UUID.randomUUID().toString();
        
        stringRedisTemplate.opsForValue().set(
            AI_SYNTAX_PREFIX + token,
            JSON.toJSONString(dto),
            TASK_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );
        
        return Result.success(token);
    }

    @GetMapping(value = "/syntax-check/stream/{token}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式获取语法检测结果")
    public SseEmitter streamSyntaxCheck(@PathVariable String token) {
        String json = stringRedisTemplate.opsForValue().get(AI_SYNTAX_PREFIX + token);
        
        if (json == null) {
            SseEmitter emitter = new SseEmitter(1000L);
            try {
                emitter.send(SseEmitter.event().data("任务不存在或已过期"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        stringRedisTemplate.delete(AI_SYNTAX_PREFIX + token);
        AiJudgeDTO dto = JSON.parseObject(json, AiJudgeDTO.class);
        
        return aiJudgeService.syntaxCheck(dto);
    }

    @PostMapping("/analyze-error/submit")
    @Operation(summary = "提交错误分析任务")
    public Result<String> submitAnalyzeError(@Valid @RequestBody AiJudgeDTO dto) {
        String token = UUID.randomUUID().toString();
        dto.setUserId(BaseContext.getCurrentId());
        
        stringRedisTemplate.opsForValue().set(
            AI_ERROR_PREFIX + token,
            JSON.toJSONString(dto),
            TASK_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );
        
        return Result.success(token);
    }

    @GetMapping(value = "/analyze-error/stream/{token}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式获取错误分析结果")
    public SseEmitter streamAnalyzeError(@PathVariable String token) {
        String json = stringRedisTemplate.opsForValue().get(AI_ERROR_PREFIX + token);
        
        if (json == null) {
            SseEmitter emitter = new SseEmitter(1000L);
            try {
                emitter.send(SseEmitter.event().data("任务不存在或已过期"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        stringRedisTemplate.delete(AI_ERROR_PREFIX + token);
        AiJudgeDTO dto = JSON.parseObject(json, AiJudgeDTO.class);
        Long userId = dto.getUserId() != null ? dto.getUserId() : BaseContext.getCurrentId();
        
        JudgeResultVO judgeResult = judgeService.submit(
            JudgeSubmitDTO.builder()
                .problemId(dto.getProblemId())
                .code(dto.getCode())
                .language(dto.getLanguage())
                .build(),
            userId
        );
        
        return aiJudgeService.analyzeError(dto, judgeResult);
    }

    @PostMapping("/chat/submit")
    @Operation(summary = "提交AI聊天任务")
    public Result<String> submitChat(@RequestBody AiChatDTO dto) {
        log.info("当前AI用户id:{}",BaseContext.getCurrentId());
        String token = UUID.randomUUID().toString();
        
        stringRedisTemplate.opsForValue().set(
            AI_CHAT_PREFIX + token,
            JSON.toJSONString(dto),
            TASK_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );
        
        return Result.success(token);
    }

    @GetMapping(value = "/chat/stream/{token}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式获取AI聊天结果")
    public SseEmitter streamChat(@PathVariable String token) {
        log.info("当前AI用户id:{}",BaseContext.getCurrentId());
        String json = stringRedisTemplate.opsForValue().get(AI_CHAT_PREFIX + token);
        
        if (json == null) {
            SseEmitter emitter = new SseEmitter(1000L);
            try {
                emitter.send(SseEmitter.event().data("任务不存在或已过期"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        stringRedisTemplate.delete(AI_CHAT_PREFIX + token);
        AiChatDTO dto = JSON.parseObject(json, AiChatDTO.class);
        dto.setUserId(BaseContext.getCurrentId());
        
        return aiJudgeService.chat(dto);
    }

    @PostMapping("/hint/submit")
    @Operation(summary = "提交获取提示任务")
    public Result<String> submitHint(@RequestBody AiChatDTO dto) {
        String token = UUID.randomUUID().toString();
        
        stringRedisTemplate.opsForValue().set(
            AI_HINT_PREFIX + token,
            JSON.toJSONString(dto),
            TASK_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );
        
        return Result.success(token);
    }

    @GetMapping(value = "/hint/stream/{token}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式获取解题提示")
    public SseEmitter streamHint(@PathVariable String token) {
        String json = stringRedisTemplate.opsForValue().get(AI_HINT_PREFIX + token);
        
        if (json == null) {
            SseEmitter emitter = new SseEmitter(1000L);
            try {
                emitter.send(SseEmitter.event().data("任务不存在或已过期"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        stringRedisTemplate.delete(AI_HINT_PREFIX + token);
        AiChatDTO dto = JSON.parseObject(json, AiChatDTO.class);
        
        return aiJudgeService.getHint(dto);
    }
}
