package com.oj.ai.service.agent.specialized;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.ai.service.agent.graph.OJAgentState;
import com.oj.ai.service.tools.SolutionGeneratorTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.bsc.langgraph4j.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SolutionAgent implements NodeAction<OJAgentState> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final SolutionGeneratorTool solutionTool;
    private final SolutionService solutionService;

    @Autowired
    public SolutionAgent(SolutionGeneratorTool solutionTool, ChatModel chatModel) {
        this.solutionTool = solutionTool;
        this.solutionService = AiServices.builder(SolutionService.class).chatModel(chatModel).tools(solutionTool).build();
    }

    @Override
    public Map<String, Object> apply(OJAgentState state) {
        String task = state.getTask();
        Integer problemId = state.getProblemId().orElse(null);
        log.info("SolutionAgent processing task: {}, problemId: {}", task, problemId);

        String result;
        if (problemId != null) {
            String problemDetail = solutionTool.getProblemDetail(problemId);
            String jsonRequest = toJson(new HashMap<String, String>() {{ put("userMessage", task); put("problemDetail", problemDetail); }});
            result = solutionService.solve(jsonRequest);
        } else {
            result = solutionService.solveGeneral(task);
        }
        return Map.of(OJAgentState.SOLUTION_RESULT, result, OJAgentState.NEXT, "supervisor");
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return obj.toString(); }
    }

    public interface SolutionService {
        @SystemMessage("你是专业的OJ题目解答助手。") String solve(@UserMessage String requestJson);
        @SystemMessage("你是专业的OJ题目解答助手。") String solveGeneral(@UserMessage String userMessage);
    }
}
