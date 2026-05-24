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

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SupervisorAgent implements NodeAction<OJAgentState> {

    private final SupervisorService supervisorService;

    @Autowired
    public SupervisorAgent(ChatModel chatModel) {
        this.supervisorService = AiServices.builder(SupervisorService.class).chatModel(chatModel).build();
    }

    @Override
    public Map<String, Object> apply(OJAgentState state) {
        log.info("SupervisorAgent processing state - iteration: {}", state.getIterationCount());

        String task = state.getTask();
        String solutionResult = state.getSolutionResult();
        String codeResult = state.getCodeResult();
        String learningResult = state.getLearningResult();
        String knowledgeResult = state.getKnowledgeResult();

        String finalResponse;
        if (!solutionResult.isEmpty() || !codeResult.isEmpty() || !learningResult.isEmpty() || !knowledgeResult.isEmpty()) {
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("用户任务：").append(task).append("\n\n各Agent处理结果：\n\n");
            Map<String, String> agentResults = new HashMap<>();
            if (!solutionResult.isEmpty()) agentResults.put("Solution Agent", solutionResult);
            if (!codeResult.isEmpty()) agentResults.put("Code Agent", codeResult);
            if (!learningResult.isEmpty()) agentResults.put("Learning Agent", learningResult);
            if (!knowledgeResult.isEmpty()) agentResults.put("Knowledge Agent", knowledgeResult);
            for (Map.Entry<String, String> entry : agentResults.entrySet()) {
                contextBuilder.append("\n【").append(entry.getKey()).append("】\n").append(entry.getValue()).append("\n");
            }
            contextBuilder.append("\n请整合生成最终回复。");
            finalResponse = supervisorService.summarize(contextBuilder.toString());
        } else {
            finalResponse = supervisorService.summarizeSimple(task);
        }
        return Map.of(OJAgentState.FINAL_RESPONSE, finalResponse, OJAgentState.NEXT, "FINISH");
    }

    public interface SupervisorService {
        @SystemMessage("你是OJ AI助手监督者，整合各Agent结果生成最终回复。") String summarize(@UserMessage String fullContext);
        @SystemMessage("你是OJ AI助手，直接回答用户问题。") String summarizeSimple(@UserMessage String userMessage);
    }
}
