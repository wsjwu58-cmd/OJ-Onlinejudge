package com.oj.ai.config;

import com.oj.ai.service.agent.LangGraphAgentOrchestrator;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.studio.LangGraphStudioServer;
import org.bsc.langgraph4j.studio.springboot.LangGraphStudioConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AgentStudioConfig extends LangGraphStudioConfig {

    @Autowired
    private LangGraphAgentOrchestrator orchestrator;

    @Override
    public Map<String, LangGraphStudioServer.Instance> instanceMap() {
        LangGraphStudioServer.Instance instance;
        try {
            instance = LangGraphStudioServer.Instance.builder()
                    .title("OJ AI Agent")
                    .graph(orchestrator.getStateGraph())
                    .build();
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        return Map.of("oj-agent", instance);
    }
}
