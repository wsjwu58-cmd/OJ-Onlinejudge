package com.oj.ai.service.agent.specialized;

import com.oj.ai.service.agent.graph.OJAgentState;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.bsc.langgraph4j.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class RouterAgent implements NodeAction<OJAgentState> {

    private final RoutingService routingService;

    @Autowired
    public RouterAgent(ChatModel chatModel) {
        this.routingService = AiServices.builder(RoutingService.class)
                .chatModel(chatModel).build();
    }

    @Override
    public Map<String, Object> apply(OJAgentState state) {
        String task = state.getTask();
        log.info("Router processing task: {}", task);
        String routingResult = routingService.route(task);
        String nextAgent = determineNextAgent(routingResult);
        log.info("Routing result: {}, next agent: {}", routingResult, nextAgent);
        return Map.of(OJAgentState.ROUTING_RESULT, routingResult,
                OJAgentState.CURRENT_AGENT, nextAgent, OJAgentState.NEXT, nextAgent);
    }

    private String determineNextAgent(String routingResult) {
        String lowerResult = routingResult.toLowerCase();
        if (lowerResult.contains("solution") || lowerResult.contains("题解") || lowerResult.contains("解题"))
            return "solution";
        if (lowerResult.contains("code") || lowerResult.contains("代码") || lowerResult.contains("分析"))
            return "code";
        if (lowerResult.contains("learning") || lowerResult.contains("学习") || lowerResult.contains("进度"))
            return "learning";
        if (lowerResult.contains("knowledge") || lowerResult.contains("知识") || lowerResult.contains("概念"))
            return "knowledge";
        return "supervisor";
    }

    public interface RoutingService {
        @SystemMessage("""
                你是一个意图识别助手，分析用户问题并确定最合适的处理Agent。
                可选Agent：solution(题解/解题), code(代码分析), learning(学情分析), knowledge(知识检索)。
                只返回Agent名称。
                """)
        String route(@UserMessage String userMessage);
    }
}
