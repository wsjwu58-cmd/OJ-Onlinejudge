package com.oj.ai.service.impl;

import com.oj.ai.dto.AiChatDTO;
import com.oj.ai.dto.AiJudgeDTO;
import com.oj.ai.service.DialogMemoryService;
import com.oj.ai.service.KnowledgeRetrievalService;
import com.oj.ai.service.RAGService;
import com.oj.api.ProblemClient;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.common.result.Result;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class RAGServiceImpl implements RAGService {

    @Autowired
    private StreamingChatModel streamingChatModel;

    @Autowired
    private ProblemClient problemClient;

    @Autowired
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Autowired
    private DialogMemoryService dialogMemoryService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public SseEmitter chatWithKnowledge(AiChatDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> {
            try {
                Long userId = dto.getUserId();
                ProblemFeignDTO problem = null;
                if (dto.getProblemId() != null) {
                    Result<ProblemFeignDTO> result = problemClient.getProblemById(dto.getProblemId());
                    if (result != null && result.getCode() == 1) problem = result.getData();
                }

                StringBuilder contextBuilder = new StringBuilder();
                if (problem != null) contextBuilder.append("题目: ").append(problem.getTitle()).append("\n");
                if (dto.getCode() != null) contextBuilder.append("代码: ").append(dto.getCode()).append("\n");
                contextBuilder.append("问题: ").append(dto.getMessage());

                List<String> knowledge = knowledgeRetrievalService.retrieveKnowledge(dto.getMessage(), contextBuilder.toString(), 3);

                StringBuilder augmentedContext = new StringBuilder();
                augmentedContext.append("## 相关知识\n");
                for (int i = 0; i < knowledge.size(); i++) augmentedContext.append((i + 1)).append(". ").append(knowledge.get(i)).append("\n\n");
                augmentedContext.append("## 用户问题\n").append(dto.getMessage());

                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from("你是一位专业的编程导师，基于检索到的知识回答用户问题。使用Markdown格式回复，不要使用引号包裹内容。"));

                List<AiChatDTO.MessageHistory> history = dialogMemoryService.getDialogHistory(userId, dto.getProblemId());
                if (!history.isEmpty()) {
                    for (AiChatDTO.MessageHistory msg : history) {
                        if ("user".equals(msg.getRole())) messages.add(UserMessage.from(msg.getContent()));
                        else messages.add(AiMessage.from(msg.getContent()));
                    }
                }
                messages.add(UserMessage.from(augmentedContext.toString()));
                dialogMemoryService.saveDialogHistory(userId, dto.getProblemId(), "user", dto.getMessage());

                StringBuilder fullResponse = new StringBuilder();
                ChatRequest request = ChatRequest.builder().messages(messages).build();
                streamingChatModel.chat(request, new StreamingChatResponseHandler() {
                    @Override public void onPartialResponse(String partialResponse) { try { if (partialResponse != null && !partialResponse.isEmpty()) { emitter.send(SseEmitter.event().data(partialResponse)); fullResponse.append(partialResponse); } } catch (Exception ignored) {} }
                    @Override public void onCompleteResponse(ChatResponse completeResponse) { if (!fullResponse.isEmpty()) dialogMemoryService.saveDialogHistory(userId, dto.getProblemId(), "assistant", fullResponse.toString()); emitter.complete(); }
                    @Override public void onError(Throwable error) { try { emitter.send(SseEmitter.event().data("AI回复出错: " + error.getMessage())); } catch (Exception ignored) {} emitter.complete(); }
                });
            } catch (Exception e) { try { emitter.send(SseEmitter.event().data("知识检索出错: " + e.getMessage())); } catch (Exception ignored) {} emitter.complete(); }
        });
        return emitter;
    }

    @Override
    public SseEmitter analyzeErrorWithKnowledge(AiJudgeDTO dto, String errorInfo) {
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> {
            try {
                ProblemFeignDTO problem = null;
                if (dto.getProblemId() != null) {
                    Result<ProblemFeignDTO> result = problemClient.getProblemById(dto.getProblemId());
                    if (result != null && result.getCode() == 1) problem = result.getData();
                }

                StringBuilder contextBuilder = new StringBuilder();
                if (problem != null) contextBuilder.append("题目: ").append(problem.getTitle()).append("\n");
                contextBuilder.append("代码: ").append(dto.getCode()).append("\n错误: ").append(errorInfo);

                List<String> knowledge = knowledgeRetrievalService.retrieveKnowledge("代码错误分析: " + errorInfo, contextBuilder.toString(), 3);
                StringBuilder augmentedContext = new StringBuilder();
                augmentedContext.append("## 相关知识\n");
                for (int i = 0; i < knowledge.size(); i++) augmentedContext.append((i + 1)).append(". ").append(knowledge.get(i)).append("\n\n");
                augmentedContext.append("## 错误分析\n").append(errorInfo);

                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from("你是一位专业的编程错误分析专家，基于检索到的知识分析代码错误。使用Markdown格式回复。"));
                messages.add(UserMessage.from(augmentedContext.toString()));

                ChatRequest request = ChatRequest.builder().messages(messages).build();
                streamingChatModel.chat(request, new StreamingChatResponseHandler() {
                    @Override public void onPartialResponse(String partialResponse) { try { if (partialResponse != null && !partialResponse.isEmpty()) emitter.send(SseEmitter.event().data(partialResponse)); } catch (Exception ignored) {} }
                    @Override public void onCompleteResponse(ChatResponse completeResponse) { emitter.complete(); }
                    @Override public void onError(Throwable error) { try { emitter.send(SseEmitter.event().data("错误分析出错: " + error.getMessage())); } catch (Exception ignored) {} emitter.complete(); }
                });
            } catch (Exception e) { try { emitter.send(SseEmitter.event().data("错误分析出错: " + e.getMessage())); } catch (Exception ignored) {} emitter.complete(); }
        });
        return emitter;
    }

    @Override
    public SseEmitter getHintWithKnowledge(AiChatDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> {
            try {
                ProblemFeignDTO problem = null;
                if (dto.getProblemId() != null) {
                    Result<ProblemFeignDTO> result = problemClient.getProblemById(dto.getProblemId());
                    if (result != null && result.getCode() == 1) problem = result.getData();
                }

                StringBuilder contextBuilder = new StringBuilder();
                if (problem != null) {
                    contextBuilder.append("题目: ").append(problem.getTitle()).append("\n");
                    if (problem.getContent() != null) contextBuilder.append("题目描述: ").append(problem.getContent()).append("\n");
                }

                List<String> knowledge = knowledgeRetrievalService.retrieveKnowledge("解题提示: " + (problem != null ? problem.getTitle() : ""), contextBuilder.toString(), 3);
                StringBuilder augmentedContext = new StringBuilder();
                if (!knowledge.isEmpty()) {
                    augmentedContext.append("## 相关知识\n");
                    for (int i = 0; i < knowledge.size(); i++) augmentedContext.append((i + 1)).append(". ").append(knowledge.get(i)).append("\n\n");
                }

                String prompt = String.format("请为以下编程题目提供解题思路提示（不要直接给出完整代码）：\n题目：%s\n题目描述：%s\n请用中文回答，包括问题分析、解题思路、可能用到的算法或数据结构、注意事项。",
                        problem != null ? problem.getTitle() : "未知题目",
                        problem != null && problem.getContent() != null ? problem.getContent() : dto.getMessage());
                augmentedContext.append(prompt);

                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from("你是一位专业的编程导师，擅长引导学生思考。只给提示，不要直接给出完整答案。使用Markdown格式回复。"));
                messages.add(UserMessage.from(augmentedContext.toString()));

                ChatRequest request = ChatRequest.builder().messages(messages).build();
                streamingChatModel.chat(request, new StreamingChatResponseHandler() {
                    @Override public void onPartialResponse(String partialResponse) { try { if (partialResponse != null && !partialResponse.isEmpty()) emitter.send(SseEmitter.event().data(partialResponse)); } catch (Exception ignored) {} }
                    @Override public void onCompleteResponse(ChatResponse completeResponse) { emitter.complete(); }
                    @Override public void onError(Throwable error) { try { emitter.send(SseEmitter.event().data("获取提示出错: " + error.getMessage())); } catch (Exception ignored) {} emitter.complete(); }
                });
            } catch (Exception e) { try { emitter.send(SseEmitter.event().data("获取提示出错: " + e.getMessage())); } catch (Exception ignored) {} emitter.complete(); }
        });
        return emitter;
    }
}
