package com.oj.service.agent;

import com.oj.dto.AgentRequestDTO;
import com.oj.service.tools.AiJudgeTool;
import com.oj.service.tools.KnowledgeRetrievalTool;
import com.oj.service.tools.LearningAnalyzerTool;
import com.oj.service.tools.SolutionGeneratorTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final SolutionGeneratorTool solutionGeneratorTool;
    private final LearningAnalyzerTool learningAnalyzerTool;
    private final AiJudgeTool aiJudgeTool;
    private final KnowledgeRetrievalTool knowledgeRetrievalTool;

    private AgentAssistant agentAssistant;
    private final Map<String, MessageWindowChatMemory> chatMemoryMap = new ConcurrentHashMap<>();
    private final InMemoryChatMemoryStore chatMemoryStore = new InMemoryChatMemoryStore();

    @Autowired
    public AgentService(
            ChatLanguageModel chatLanguageModel,
            StreamingChatLanguageModel streamingChatLanguageModel,
            SolutionGeneratorTool solutionGeneratorTool,
            LearningAnalyzerTool learningAnalyzerTool,
            AiJudgeTool aiJudgeTool,
            KnowledgeRetrievalTool knowledgeRetrievalTool) {
        this.chatLanguageModel = chatLanguageModel;
        this.streamingChatLanguageModel = streamingChatLanguageModel;
        this.solutionGeneratorTool = solutionGeneratorTool;
        this.learningAnalyzerTool = learningAnalyzerTool;
        this.aiJudgeTool = aiJudgeTool;
        this.knowledgeRetrievalTool = knowledgeRetrievalTool;
    }

    @PostConstruct
    public void init() {
        this.agentAssistant = AiServices.builder(AgentAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .tools(solutionGeneratorTool, learningAnalyzerTool, aiJudgeTool, knowledgeRetrievalTool)
                .chatMemoryProvider(memoryId -> {
                    MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .maxMessages(20)
                            .chatMemoryStore(chatMemoryStore)
                            .build();
                    chatMemoryMap.put(memoryId.toString(), memory);
                    return memory;
                })
                .build();
        log.info("AgentService initialized with LangChain4j tools including RAG knowledge retrieval");
    }

    public String chat(String sessionId, String message) {
        log.info("Agent chat - sessionId: {}, message: {}", sessionId, message);
        try {
            return agentAssistant.chat(sessionId, message);
        } catch (Exception e) {
            log.error("Agent chat error", e);
            return "抱歉，处理您的请求时出现错误: " + e.getMessage();
        }
    }

    public Flux<String> chatStream(String sessionId, String message) {
        log.info("Agent chat stream - sessionId: {}, message: {}", sessionId, message);
        // 直接返回流，但附加错误处理，防止异常被吞导致“假死”
        return agentAssistant.chatStream(sessionId, message)
                .doOnSubscribe(sub -> log.info("✅ 流已订阅"))
                .doOnNext(chunk -> {
                    // 打印每一个收到的数据块，哪怕它是空的
                    if (chunk == null || chunk.isEmpty()) {
                        log.debug("⚠️ 收到空数据块");
                    } else {
                        log.info("📩 收到数据块: [{}]", chunk.length() > 50 ? chunk.substring(0, 50) + "..." : chunk);
                    }
                })
                .doOnError(err -> log.error("❌ 流发生错误", err))
                .doOnComplete(() -> log.info("🏁 流完成"))
                .onErrorResume(throwable -> {
                    log.error("捕获异常并返回错误信息", throwable);
                    return Flux.just("系统出错: " + throwable.getMessage());
                });
    }

    public String processAgentRequest(AgentRequestDTO request) {
        log.info("Process agent request - task: {}, userId: {}, problemId: {}", 
                request.getTask(), request.getUserId(), request.getProblemId());
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "default";
        return chat(sessionId, request.getTask());
    }

    public Flux<String> processAgentRequestStream(AgentRequestDTO request) {
        log.info("Process agent request stream - task: {}, userId: {}, problemId: {}", 
                request.getTask(), request.getUserId(), request.getProblemId());
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "default";
        return chatStream(sessionId, request.getTask());
    }

    public void clearSession(String sessionId) {
        log.info("Clear session: {}", sessionId);
        chatMemoryMap.remove(sessionId);
    }
}
