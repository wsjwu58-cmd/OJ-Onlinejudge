package com.oj.service.agent;

import com.oj.config.McpClientConfiguration;
import com.oj.dto.AgentRequestDTO;
import com.oj.exception.ParameterMissingException;
import com.oj.service.agent.memory.LongTermMemoryService;
import com.oj.service.agent.memory.RedisChatMemoryStore;
import com.oj.service.tools.AiJudgeTool;
import com.oj.service.tools.KnowledgeRetrievalTool;
import com.oj.service.tools.LearningAnalyzerTool;
import com.oj.service.tools.SolutionGeneratorTool;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final Pattern EXPLICIT_PROBLEM_ID_PATTERN = Pattern.compile("(?:题目\\s*ID|problem\\s*id|第)\\s*[:：#]?(\\d+)\\s*(?:题)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FALLBACK_NUMBER_PATTERN = Pattern.compile("\\b(\\d{1,9})\\b");

    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final SolutionGeneratorTool solutionGeneratorTool;
    private final LearningAnalyzerTool learningAnalyzerTool;
    private final AiJudgeTool aiJudgeTool;
    private final KnowledgeRetrievalTool knowledgeRetrievalTool;
    private final RedisChatMemoryStore redisChatMemoryStore;
    private final LongTermMemoryService longTermMemoryService;
    private ToolProvider mcpToolProvider;
    private McpClientConfiguration mcpClientConfiguration;

    private AgentAssistant agentAssistant;

    @Autowired
    public AgentService(
            ChatLanguageModel chatLanguageModel,
            StreamingChatLanguageModel streamingChatLanguageModel,
            SolutionGeneratorTool solutionGeneratorTool,
            LearningAnalyzerTool learningAnalyzerTool,
            AiJudgeTool aiJudgeTool,
            KnowledgeRetrievalTool knowledgeRetrievalTool,
            RedisChatMemoryStore redisChatMemoryStore,
            LongTermMemoryService longTermMemoryService,
            McpClientConfiguration mcpClientConfiguration
            ) {
        this.chatLanguageModel = chatLanguageModel;
        this.streamingChatLanguageModel = streamingChatLanguageModel;
        this.solutionGeneratorTool = solutionGeneratorTool;
        this.learningAnalyzerTool = learningAnalyzerTool;
        this.aiJudgeTool = aiJudgeTool;
        this.knowledgeRetrievalTool = knowledgeRetrievalTool;
        this.redisChatMemoryStore = redisChatMemoryStore;
        this.longTermMemoryService = longTermMemoryService;

        this.mcpClientConfiguration=mcpClientConfiguration;
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
                    .chatLanguageModel(chatLanguageModel)
                    .streamingChatLanguageModel(streamingChatLanguageModel)
                    .tools(solutionGeneratorTool, learningAnalyzerTool, aiJudgeTool, knowledgeRetrievalTool)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .maxMessages(20)
                            .chatMemoryStore(redisChatMemoryStore)
                            .build())
                    .toolProvider(mcpToolProvider)
                    .build();
            log.info("AgentService initialized with MCP ToolProvider");
        }
//        } else {
//            this.agentAssistant = AiServices.builder(AgentAssistant.class)
//                    .chatLanguageModel(chatLanguageModel)
//                    .streamingChatLanguageModel(streamingChatLanguageModel)
//                    .tools(solutionGeneratorTool, learningAnalyzerTool, aiJudgeTool, knowledgeRetrievalTool)
//                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
//                            .id(memoryId)
//                            .maxMessages(20)
//                            .chatMemoryStore(redisChatMemoryStore)
//                            .build())
//                    .build();
//            log.info("AgentService initialized without MCP ToolProvider");
//        }
        log.info("AgentService initialized with Redis-backed chat memory, long-term memory service");
    }

    public String chat(String sessionId, String message) {
        log.info("Agent chat - sessionId: {}, message: {}", sessionId, message);
        return agentAssistant.chat(sessionId, message);
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
                .doOnComplete(() -> log.info("🏁 流完成"));
    }

    public String processAgentRequest(AgentRequestDTO request) {
        log.info("Process agent request - task: {}, userId: {}, problemId: {}",
                request.getTask(), request.getUserId(), request.getProblemId());
        String sessionId = resolveSessionId(request);
        try {
            assertProblemIdIfNeeded(request);

            String userTask = request.getTask() != null ? request.getTask() : "";
            String augmented = augmentUserMessage(request, userTask);
            String response = chat(sessionId, augmented);
            if (request.getUserId() != null && !userTask.isEmpty()) {
                longTermMemoryService.saveUserInteraction(
                        request.getUserId(), sessionId, userTask, response);
            }
            return response;
        } catch (ParameterMissingException ex) {
            return handleParameterMissing(sessionId, ex);
        }
    }

    public Flux<String> processAgentRequestStream(AgentRequestDTO request) {
        log.info("Process agent request stream - task: {}, userId: {}, problemId: {}",
                request.getTask(), request.getUserId(), request.getProblemId());
        String sessionId = resolveSessionId(request);
        try {
            assertProblemIdIfNeeded(request);

            String userTask = request.getTask() != null ? request.getTask() : "";
            String augmented = augmentUserMessage(request, userTask);
            StringBuilder responseBuffer = new StringBuilder();
            return chatStream(sessionId, augmented)
                    .doOnNext(responseBuffer::append)
                    .doOnComplete(() -> {
                        if (request.getUserId() != null && !userTask.isEmpty()) {
                            longTermMemoryService.saveUserInteraction(
                                    request.getUserId(), sessionId, userTask,
                                    responseBuffer.toString());
                        }
                    })
                    .onErrorResume(ParameterMissingException.class,
                            ex -> Flux.just(handleParameterMissing(sessionId, (ParameterMissingException) ex)));
        } catch (ParameterMissingException ex) {
            return Flux.just(handleParameterMissing(sessionId, ex));
        }
    }

    public void clearSession(String sessionId) {
        log.info("Clear session: {}", sessionId);
        redisChatMemoryStore.deleteMessages(sessionId);
    }

    private static String resolveSessionId(AgentRequestDTO request) {
        String sid = request.getSessionId();
        if (sid == null || sid.isBlank()) {
            log.warn("Agent 请求未带 sessionId，多轮对话将共用 default；请前端为每个会话传入稳定 sessionId");
            return "default";
        }
        return sid;
    }

    /**
     * 拼接 request.context、用户登录后的 Redis 长期记忆摘要；不改变 LangChain 窗口本身（仍按 sessionId 存 Redis）。
     */
    private String augmentUserMessage(AgentRequestDTO request, String userTask) {
        StringBuilder sb = new StringBuilder();
        if (request.getContext() != null && !request.getContext().isBlank()) {
            sb.append("【附加上下文】\n").append(request.getContext().trim()).append("\n\n");
        }
        if (request.getUserId() != null) {
            Map<String, Object> ctx = longTermMemoryService.buildContextForUser(request.getUserId(), userTask);
            String memoryBlock = longTermMemoryService.formatContextForPrompt(ctx);
            if (!memoryBlock.isEmpty()) {
                sb.append("【系统已记住的与你相关的信息（含近期对话摘要）】\n")
                        .append(memoryBlock)
                        .append("\n\n");
            }
        }
        sb.append(userTask);
        return sb.toString();
    }

    /**
     * 当用户要看“题解/解题思路/参考代码”等但缺少明确题目 ID 时，直接询问用户。
     */
    private void assertProblemIdIfNeeded(AgentRequestDTO request) {
        String task = request.getTask() != null ? request.getTask() : "";
        String ctx = request.getContext() != null ? request.getContext() : "";
        String combined = task + "\n" + ctx;

        // 只有在“看题解/解题思路/参考代码”等场景才需要题目ID
        boolean likelyNeedsProblemId =
                task.contains("题解") || task.contains("解题") || task.contains("思路") ||
                        task.contains("参考代码") || task.contains("题目") || task.contains("写代码") ||
                        task.contains("第");
        if (!likelyNeedsProblemId) return;

        Integer problemId = resolveProblemIdFromRequest(request, combined);
        if (problemId != null) return;

        // 逻辑层面的“参数缺失”，用异常机制交由上层处理。
        throw new ParameterMissingException("problemId");
    }

    private Integer resolveProblemIdFromRequest(AgentRequestDTO request, String combined) {
        if (request.getProblemId() != null) return request.getProblemId();
        if (combined == null || combined.isBlank()) return null;

        // 优先提取显式“题目ID/第X题”等
        Matcher m = EXPLICIT_PROBLEM_ID_PATTERN.matcher(combined);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {
                // ignore parse error
            }
        }

        // 兜底：如果文本里只出现一串数字（通常是第X题），取第一个
        Matcher f = FALLBACK_NUMBER_PATTERN.matcher(combined);
        if (f.find()) {
            try {
                return Integer.parseInt(f.group(1));
            } catch (Exception ignored) {
                // ignore parse error
            }
        }

        return null;
    }

    private String handleParameterMissing(String sessionId, ParameterMissingException ex) {
        if (ex == null) {
            return "参数缺失";
        }

        String missing = ex.getMissingParam();
        String fixedProblemAsk = "请问您想看哪个题目的题解？请提供题目 ID。";
        String fixedUserAsk = "请提供您的用户ID（或请先登录）。";

        // 先让 LLM 走 System Prompt 规则；最后用固定文案做兜底，确保你要的输出严格一致。
        String llmResponse;
        try {
            llmResponse = agentAssistant.chat(sessionId, "参数缺失:" + missing);
        } catch (ParameterMissingException ignore) {
            llmResponse = null;
        }

        if ("problemId".equalsIgnoreCase(missing)) {
            return fixedProblemAsk;
        }
        if ("userId".equalsIgnoreCase(missing)) {
            return fixedUserAsk;
        }
        return llmResponse != null ? llmResponse : "参数缺失";
    }
}
