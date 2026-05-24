package com.oj.ai.service.agent.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OJAgentState extends AgentState {

    public static final String USER_ID = "userId";
    public static final String SESSION_ID = "sessionId";
    public static final String TASK = "task";
    public static final String CONTEXT = "context";
    public static final String PROBLEM_ID = "problemId";
    public static final String CURRENT_AGENT = "currentAgent";
    public static final String ROUTING_RESULT = "routingResult";
    public static final String SOLUTION_RESULT = "solutionResult";
    public static final String CODE_RESULT = "codeResult";
    public static final String LEARNING_RESULT = "learningResult";
    public static final String KNOWLEDGE_RESULT = "knowledgeResult";
    public static final String ITERATION_COUNT = "iterationCount";
    public static final String CONFIDENCE = "confidence";
    public static final String FINAL_RESPONSE = "finalResponse";
    public static final String NEXT = "next";

    public static final Map<String, Channel<?>> SCHEMA;

    static {
        Map<String, Channel<?>> schemaMap = new HashMap<>();
        schemaMap.put(USER_ID, Channels.base(() -> 0L));
        schemaMap.put(SESSION_ID, Channels.base(() -> "default"));
        schemaMap.put(TASK, Channels.base(() -> ""));
        schemaMap.put(CONTEXT, Channels.base(() -> ""));
        schemaMap.put(PROBLEM_ID, Channels.base(() -> 0));
        schemaMap.put(CURRENT_AGENT, Channels.base(() -> "router"));
        schemaMap.put(ROUTING_RESULT, Channels.base(() -> ""));
        schemaMap.put(SOLUTION_RESULT, Channels.base(() -> ""));
        schemaMap.put(CODE_RESULT, Channels.base(() -> ""));
        schemaMap.put(LEARNING_RESULT, Channels.base(() -> ""));
        schemaMap.put(KNOWLEDGE_RESULT, Channels.base(() -> ""));
        schemaMap.put(ITERATION_COUNT, Channels.base(() -> 0));
        schemaMap.put(CONFIDENCE, Channels.base(() -> 0.0));
        schemaMap.put(FINAL_RESPONSE, Channels.base(() -> ""));
        schemaMap.put(NEXT, Channels.base(() -> ""));
        SCHEMA = Map.copyOf(schemaMap);
    }

    public OJAgentState(Map<String, Object> initData) {
        super(initData);
    }

    public Optional<Long> getUserId() {
        return value(USER_ID).map(obj -> ((Number) obj).longValue());
    }

    public String getSessionId() {
        return (String) value(SESSION_ID).orElse("default");
    }

    public String getTask() {
        return (String) value(TASK).orElse("");
    }

    public Optional<Integer> getProblemId() {
        return value(PROBLEM_ID).map(obj -> ((Number) obj).intValue());
    }

    public String getCurrentAgent() {
        return (String) value(CURRENT_AGENT).orElse("router");
    }

    public String getRoutingResult() {
        return (String) value(ROUTING_RESULT).orElse("");
    }

    public int getIterationCount() {
        return ((Number) value(ITERATION_COUNT).orElse(0)).intValue();
    }

    public Optional<String> next() {
        return value(NEXT).map(Object::toString);
    }

    public String getSolutionResult() {
        return (String) value(SOLUTION_RESULT).orElse("");
    }

    public String getCodeResult() {
        return (String) value(CODE_RESULT).orElse("");
    }

    public String getLearningResult() {
        return (String) value(LEARNING_RESULT).orElse("");
    }

    public String getKnowledgeResult() {
        return (String) value(KNOWLEDGE_RESULT).orElse("");
    }

    public String getFinalResponse() {
        return (String) value(FINAL_RESPONSE).orElse("");
    }
}
