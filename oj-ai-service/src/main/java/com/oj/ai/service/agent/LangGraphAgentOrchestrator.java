package com.oj.ai.service.agent;

import com.oj.ai.service.agent.graph.OJAgentState;
import com.oj.ai.service.agent.specialized.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;

@Service
@Slf4j
public class LangGraphAgentOrchestrator {

    private final RouterAgent routerAgent;
    private final SolutionAgent solutionAgent;
    private final CodeJudgeAgent codeJudgeAgent;
    private final LearningAgent learningAgent;
    private final KnowledgeAgent knowledgeAgent;
    private final SupervisorAgent supervisorAgent;
    private final MemorySaver checkpointSaver;

    private CompiledGraph<OJAgentState> compiledGraph;

    @Autowired
    public LangGraphAgentOrchestrator(RouterAgent routerAgent, SolutionAgent solutionAgent,
                                      CodeJudgeAgent codeJudgeAgent, LearningAgent learningAgent,
                                      KnowledgeAgent knowledgeAgent, SupervisorAgent supervisorAgent) {
        this.routerAgent = routerAgent;
        this.solutionAgent = solutionAgent;
        this.codeJudgeAgent = codeJudgeAgent;
        this.learningAgent = learningAgent;
        this.knowledgeAgent = knowledgeAgent;
        this.supervisorAgent = supervisorAgent;
        this.checkpointSaver = new MemorySaver();
    }

    @PostConstruct
    public void init() throws GraphStateException {
        log.info("Initializing LangGraph Agent Orchestrator...");
        try {
            StateGraph<OJAgentState> graph = buildGraph();
            CompileConfig config = CompileConfig.builder()
                    .checkpointSaver(checkpointSaver)
                    .build();
            this.compiledGraph = graph.compile(config);
            log.info("LangGraph Agent Orchestrator initialized successfully");
        } catch (GraphStateException e) {
            log.error("Failed to initialize LangGraph Agent Orchestrator", e);
            throw e;
        }
    }

    private StateGraph<OJAgentState> buildGraph() throws GraphStateException {
        var builder = new StateGraph<>(OJAgentState.SCHEMA, OJAgentState::new);

        builder.addNode("router", node_async(routerAgent));
        builder.addNode("solution", node_async(solutionAgent));
        builder.addNode("code", node_async(codeJudgeAgent));
        builder.addNode("learning", node_async(learningAgent));
        builder.addNode("knowledge", node_async(knowledgeAgent));
        builder.addNode("supervisor", node_async(supervisorAgent));

        builder.addEdge(START, "router");

        AsyncEdgeAction<OJAgentState> routingCondition = state ->
                CompletableFuture.completedFuture(state.next().orElse("supervisor"));

        builder.addConditionalEdges("router", routingCondition,
                Map.of("solution", "solution", "code", "code",
                        "learning", "learning", "knowledge", "knowledge",
                        "supervisor", "supervisor"));

        builder.addEdge("solution", "supervisor");
        builder.addEdge("code", "supervisor");
        builder.addEdge("learning", "supervisor");
        builder.addEdge("knowledge", "supervisor");
        builder.addEdge("supervisor", END);

        return builder;
    }

    public String chat(String sessionId, String task, Long userId, Integer problemId) {
        log.info("Processing chat - sessionId: {}, task: {}", sessionId, task);

        Map<String, Object> initialState = Map.of(
                OJAgentState.SESSION_ID, sessionId,
                OJAgentState.TASK, task,
                OJAgentState.USER_ID, userId != null ? userId : 0L,
                OJAgentState.PROBLEM_ID, problemId != null ? problemId : 0
        );

        RunnableConfig config = RunnableConfig.builder().threadId(sessionId).build();

        try {
            Optional<OJAgentState> result = compiledGraph.invoke(initialState, config);
            return result.map(OJAgentState::getFinalResponse).orElse("抱歉，处理您的请求时遇到了错误。");
        } catch (Exception e) {
            log.error("Error during graph execution", e);
            return "抱歉，处理您的请求时遇到了错误。请稍后重试。";
        }
    }

    public StateGraph<OJAgentState> getStateGraph() throws GraphStateException {
        return buildGraph();
    }
}
