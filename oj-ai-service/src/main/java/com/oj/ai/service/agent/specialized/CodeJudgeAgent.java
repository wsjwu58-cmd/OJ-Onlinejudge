package com.oj.ai.service.agent.specialized;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.ai.service.agent.graph.OJAgentState;
import com.oj.ai.service.tools.AiJudgeTool;
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
public class CodeJudgeAgent implements NodeAction<OJAgentState> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final AiJudgeTool aiJudgeTool;
    private final CodeJudgeService codeJudgeService;

    @Autowired
    public CodeJudgeAgent(AiJudgeTool aiJudgeTool, ChatModel chatModel) {
        this.aiJudgeTool = aiJudgeTool;
        this.codeJudgeService = AiServices.builder(CodeJudgeService.class).chatModel(chatModel).tools(aiJudgeTool).build();
    }

    @Override
    public Map<String, Object> apply(OJAgentState state) {
        String task = state.getTask();
        Integer problemId = state.getProblemId().orElse(null);
        log.info("CodeJudgeAgent processing task: {}, problemId: {}", task, problemId);

        String result;
        if (problemId != null) {
            String problemDetail = aiJudgeTool.getTestCasesForJudge(problemId);
            String jsonRequest = toJson(new HashMap<String, String>() {{ put("userMessage", task); put("problemDetail", problemDetail); }});
            result = codeJudgeService.analyze(jsonRequest);
        } else {
            result = codeJudgeService.analyzeGeneral(task);
        }
        return Map.of(OJAgentState.CODE_RESULT, result, OJAgentState.NEXT, "supervisor");
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return obj.toString(); }
    }

    public interface CodeJudgeService {
        @SystemMessage("你是专业的OJ代码分析助手。") String analyze(@UserMessage String requestJson);
        @SystemMessage("你是专业的OJ代码分析助手。") String analyzeGeneral(@UserMessage String userMessage);
    }
}
