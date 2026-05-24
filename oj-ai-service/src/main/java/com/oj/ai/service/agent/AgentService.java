package com.oj.ai.service.agent;

import com.oj.ai.dto.AgentRequestDTO;
import com.oj.ai.service.memory.LongTermMemoryService;
import com.oj.ai.service.memory.RedisChatMemoryStore;
import com.oj.ai.service.tools.AiJudgeTool;
import com.oj.ai.service.tools.KnowledgeRetrievalTool;
import com.oj.ai.service.tools.LearningAnalyzerTool;
import com.oj.ai.service.tools.SolutionGeneratorTool;
import com.oj.common.exception.ParameterMissingException;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AgentService {

    private static final Pattern EXPLICIT_PROBLEM_ID_PATTERN = Pattern.compile(
            "(?:题目\\s*ID|problem\\s*id|第)\\s*[:：#]?(\\d+)\\s*(?:题)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALLBACK_NUMBER_PATTERN = Pattern.compile("\\b(\\d{1,9})\\b");

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final SolutionGeneratorTool solutionGeneratorTool;
    private final LearningAnalyzerTool learningAnalyzerTool;
    private final AiJudgeTool aiJudgeTool;
    private final KnowledgeRetrievalTool knowledgeRetrievalTool;
    private final RedisChatMemoryStore redisChatMemoryStore;
    private final LongTermMemoryService longTermMemoryService;
    private ToolProvider mcpToolProvider;

    private AgentAssistant agentAssistant;

    @Autowired
    public AgentService(ChatModel chatModel, StreamingChatModel streamingChatModel,
                        SolutionGeneratorTool solutionGeneratorTool,
                        LearningAnalyzerTool learningAnalyzerTool,
                        AiJudgeTool aiJudgeTool,
                        KnowledgeRetrievalTool knowledgeRetrievalTool,
                        RedisChatMemoryStore redisChatMemoryStore,
                        LongTermMemoryService longTermMemoryService) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.solutionGeneratorTool = solutionGeneratorTool;
        this.learningAnalyzerTool = learningAnalyzerTool;
        this.aiJudgeTool = aiJudgeTool;
        this.knowledgeRetrievalTool = knowledgeRetrievalTool;
        this.redisChatMemoryStore = redisChatMemoryStore;
        this.longTermMemoryService = longTermMemoryService;
    }

    @Autowired(required = false)
    @Qualifier("mcpToolProvider")
    public void setMcpToolProvider(ToolProvider mcpToolProvider) {
        this.mcpToolProvider = mcpToolProvider;
    }

    @PostConstruct
    public void init() {
        if (mcpToolProvider != null) {
            this.agentAssistant = AiServices.builder(AgentAssistant.class)
                    .chatModel(chatModel)
                    .streamingChatModel(streamingChatModel)
                    .tools(solutionGeneratorTool, learningAnalyzerTool, aiJudgeTool, knowledgeRetrievalTool)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .maxMessages(20)
                            .chatMemoryStore(redisChatMemoryStore)
                            .build())
                    .toolProvider(mcpToolProvider)
                    .build();
            log.info("AgentService initialized with MCP ToolProvider");
        } else {
            this.agentAssistant = AiServices.builder(AgentAssistant.class)
                    .chatModel(chatModel)
                    .streamingChatModel(streamingChatModel)
                    .tools(solutionGeneratorTool, learningAnalyzerTool, aiJudgeTool, knowledgeRetrievalTool)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .maxMessages(20)
                            .chatMemoryStore(redisChatMemoryStore)
                            .build())
                    .build();
            log.info("AgentService initialized without MCP ToolProvider");
        }
    }

    public String chat(String sessionId, String message) {
        return agentAssistant.chat(sessionId, message);
    }

    public Flux<String> chatStream(String sessionId, String message) {
        return agentAssistant.chatStream(sessionId, message);
    }

    public String processAgentRequest(AgentRequestDTO request) {
        String sessionId = resolveSessionId(request);
        try {
            String userTask = request.getTask() != null ? request.getTask() : "";
            String augmented = augmentUserMessage(request, userTask);
            String response = chat(sessionId, augmented);
            if (request.getUserId() != null && !userTask.isEmpty()) {
                longTermMemoryService.saveUserInteraction(request.getUserId(), sessionId, userTask, response);
            }
            return response;
        } catch (ParameterMissingException ex) {
            return handleParameterMissing(sessionId, ex);
        }
    }

    public Flux<String> processAgentRequestStream(AgentRequestDTO request) {
        String sessionId = resolveSessionId(request);
        try {
            String userTask = request.getTask() != null ? request.getTask() : "";
            String augmented = augmentUserMessage(request, userTask);
            StringBuilder responseBuffer = new StringBuilder();
            return chatStream(sessionId, augmented)
                    .doOnNext(responseBuffer::append)
                    .doOnComplete(() -> {
                        if (request.getUserId() != null && !userTask.isEmpty()) {
                            longTermMemoryService.saveUserInteraction(
                                    request.getUserId(), sessionId, userTask, responseBuffer.toString());
                        }
                    })
                    .onErrorResume(ParameterMissingException.class,
                            ex -> Flux.just(handleParameterMissing(sessionId, ex)));
        } catch (ParameterMissingException ex) {
            return Flux.just(handleParameterMissing(sessionId, ex));
        }
    }

    public void clearSession(String sessionId) {
        redisChatMemoryStore.deleteMessages(sessionId);
    }

    private static String resolveSessionId(AgentRequestDTO request) {
        String sid = request.getSessionId();
        return (sid == null || sid.isBlank()) ? "default" : sid;
    }

    private String augmentUserMessage(AgentRequestDTO request, String userTask) {
        StringBuilder sb = new StringBuilder();
        if (request.getContext() != null && !request.getContext().isBlank()) {
            sb.append("【附加上下文】\n").append(request.getContext().trim()).append("\n\n");
        }
        if (request.getUserId() != null) {
            Map<String, Object> ctx = longTermMemoryService.buildContextForUser(request.getUserId(), userTask);
            String memoryBlock = longTermMemoryService.formatContextForPrompt(ctx);
            if (!memoryBlock.isEmpty()) {
                sb.append("【系统已记住的与你相关的信息】\n").append(memoryBlock).append("\n\n");
            }
        }
        sb.append(userTask);
        return sb.toString();
    }

    private String handleParameterMissing(String sessionId, ParameterMissingException ex) {
        String missing = ex.getMissingParam();
        if ("problemId".equalsIgnoreCase(missing)) {
            return "请问您想看哪个题目的题解？请提供题目 ID。";
        }
        if ("userId".equalsIgnoreCase(missing)) {
            return "请提供您的用户ID（或请先登录）。";
        }
        return "参数缺失";
    }
}
