package com.oj.ai.service.agent.specialized;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.ai.service.agent.graph.OJAgentState;
import com.oj.ai.service.tools.LearningAnalyzerTool;
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
public class LearningAgent implements NodeAction<OJAgentState> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final LearningAnalyzerTool learningTool;
    private final LearningService learningService;

    @Autowired
    public LearningAgent(LearningAnalyzerTool learningTool, ChatModel chatModel) {
        this.learningTool = learningTool;
        this.learningService = AiServices.builder(LearningService.class).chatModel(chatModel).tools(learningTool).build();
    }

    @Override
    public Map<String, Object> apply(OJAgentState state) {
        String task = state.getTask();
        Long userId = state.getUserId().orElse(null);
        log.info("LearningAgent processing task: {}, userId: {}", task, userId);

        String result;
        if (userId != null) {
            String stats = learningTool.getUserSubmissionStats(userId, 30);
            String progress = learningTool.getUserProblemProgress(userId);
            String jsonRequest = toJson(new HashMap<String, String>() {{ put("userMessage", task); put("stats", stats != null ? stats : ""); put("progress", progress != null ? progress : ""); }});
            result = learningService.analyze(jsonRequest);
        } else {
            result = learningService.analyzeGeneral(task);
        }
        return Map.of(OJAgentState.LEARNING_RESULT, result, OJAgentState.NEXT, "supervisor");
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return obj.toString(); }
    }

    public interface LearningService {
        @SystemMessage("你是专业的OJ学习分析助手。") String analyze(@UserMessage String requestJson);
        @SystemMessage("你是专业的OJ学习分析助手。") String analyzeGeneral(@UserMessage String userMessage);
    }
}
