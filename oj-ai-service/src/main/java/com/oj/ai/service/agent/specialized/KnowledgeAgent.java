package com.oj.ai.service.agent.specialized;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.ai.service.agent.graph.OJAgentState;
import com.oj.ai.service.tools.KnowledgeRetrievalTool;
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
public class KnowledgeAgent implements NodeAction<OJAgentState> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final KnowledgeRetrievalTool knowledgeTool;
    private final KnowledgeService knowledgeService;

    @Autowired
    public KnowledgeAgent(KnowledgeRetrievalTool knowledgeTool, ChatModel chatModel) {
        this.knowledgeTool = knowledgeTool;
        this.knowledgeService = AiServices.builder(KnowledgeService.class).chatModel(chatModel).tools(knowledgeTool).build();
    }

    @Override
    public Map<String, Object> apply(OJAgentState state) {
        String task = state.getTask();
        Integer problemId = state.getProblemId().orElse(null);
        log.info("KnowledgeAgent processing task: {}, problemId: {}", task, problemId);

        String result;
        if (problemId != null) {
            String knowledge = knowledgeTool.searchProblemKnowledge(problemId, task);
            String jsonRequest = toJson(new HashMap<String, String>() {{ put("userMessage", task); put("knowledge", knowledge != null ? knowledge : ""); }});
            result = knowledgeService.retrieve(jsonRequest);
        } else {
            String knowledge = knowledgeTool.searchKnowledge(task, "", 3);
            String jsonRequest = toJson(new HashMap<String, String>() {{ put("userMessage", task); put("knowledge", knowledge != null ? knowledge : ""); }});
            result = knowledgeService.retrieveGeneral(jsonRequest);
        }
        return Map.of(OJAgentState.KNOWLEDGE_RESULT, result, OJAgentState.NEXT, "supervisor");
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return obj.toString(); }
    }

    public interface KnowledgeService {
        @SystemMessage("你是专业的OJ知识检索助手。") String retrieve(@UserMessage String requestJson);
        @SystemMessage("你是专业的OJ知识检索助手。") String retrieveGeneral(@UserMessage String requestJson);
    }
}
