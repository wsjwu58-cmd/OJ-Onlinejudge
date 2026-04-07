package com.oj.controller.User;

import com.oj.dto.AgentRequestDTO;
import com.oj.result.Result;
import com.oj.service.agent.AgentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/user/agent")
@Slf4j
@Tag(name = "AI Agent接口", description = "AI智能代理相关接口")
public class agentcontroller {

    @Autowired
    private AgentService agentService;


    @PostMapping("/chat/stream")
    @Operation(summary = "AI智能对话", description = "用户输入自然语言，AI自动识别意图并调用工具")
    public Result<String> chat(@RequestBody AgentRequestDTO request) {
        log.info("Agent chat request: {}", request.getTask());
        String response = agentService.processAgentRequest(request);
        return Result.success(response);
    }

//    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    @Operation(summary = "AI流式智能对话", description = "流式输出，AI自动识别意图")
//    public Flux<String> chatStream(@RequestBody AgentRequestDTO request) {
//        log.info("Agent chat stream request: {}", request.getTask());
//        return agentService.processAgentRequestStream(request);
//    }
}
