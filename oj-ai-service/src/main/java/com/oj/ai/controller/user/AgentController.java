package com.oj.ai.controller.user;

import com.oj.ai.dto.AgentRequestDTO;
import com.oj.ai.service.agent.AgentService;
import com.oj.ai.service.agent.LangGraphAgentOrchestrator;
import com.oj.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/user/agent")
@Slf4j
@Tag(name = "AI Agent接口")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired(required = false)
    private LangGraphAgentOrchestrator langGraphOrchestrator;

    @Value("${oj.agent.use-langgraph4j:false}")
    private boolean useLangGraph4jByDefault;

    @PostMapping("/chat")
    @Operation(summary = "AI智能对话")
    public Result<String> chat(@RequestBody AgentRequestDTO request) {
        log.info("Agent chat request: task={}, agentType={}", request.getTask(), request.getAgentType());

        String response;
        if (shouldUseLangGraph(request)) {
            response = langGraphOrchestrator.chat(request.getSessionId(), request.getTask(), request.getUserId(), request.getProblemId());
        } else {
            response = agentService.processAgentRequest(request);
        }
        return Result.success(response);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI流式智能对话")
    public Flux<String> chatStream(@RequestBody AgentRequestDTO request) {
        log.info("Agent chat stream request: task={}", request.getTask());

        if (shouldUseLangGraph(request)) {
            String response = langGraphOrchestrator.chat(request.getSessionId(), request.getTask(), request.getUserId(), request.getProblemId());
            return Flux.just(response);
        } else {
            return agentService.processAgentRequestStream(request);
        }
    }

    private boolean shouldUseLangGraph(AgentRequestDTO request) {
        if ("langgraph".equals(request.getAgentType())) return true;
        if ("traditional".equals(request.getAgentType())) return false;
        return useLangGraph4jByDefault && langGraphOrchestrator != null;
    }
}
