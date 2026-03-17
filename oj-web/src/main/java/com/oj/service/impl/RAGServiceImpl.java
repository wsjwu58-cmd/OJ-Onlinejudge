package com.oj.service.impl;

import com.oj.dto.AiChatDTO;
import com.oj.dto.AiJudgeDTO;
import com.oj.entity.Problem;
import com.oj.mapper.ProblemMapper;
import com.oj.service.RAGService;
import com.oj.service.KnowledgeRetrievalService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RAGServiceImpl implements RAGService {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private ProblemMapper problemMapper;

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
                Problem problem = dto.getProblemId() != null ? problemMapper.selectById(dto.getProblemId()) : null;
                
                // 构建查询上下文
                StringBuilder contextBuilder = new StringBuilder();
                if (problem != null) {
                    contextBuilder.append("题目: " + problem.getTitle() + "\n");
                }
                if (dto.getCode() != null) {
                    contextBuilder.append("代码: " + dto.getCode() + "\n");
                }
                contextBuilder.append("问题: " + dto.getMessage());
                
                // 检索相关知识
                List<String> knowledge = knowledgeRetrievalService.retrieveKnowledge(
                        dto.getMessage(),
                        contextBuilder.toString(),
                        3
                );
                
                // 构建增强上下文
                StringBuilder augmentedContext = new StringBuilder();
                augmentedContext.append("## 相关知识\n");
                for (int i = 0; i < knowledge.size(); i++) {
                    augmentedContext.append((i + 1) + ". " + knowledge.get(i) + "\n\n");
                }
                augmentedContext.append("## 用户问题\n" + dto.getMessage());
                
                // 构建消息列表
                List<Message> messages = new ArrayList<>();
                messages.add(new SystemMessage("你是一位专业的编程导师，基于检索到的知识回答用户问题。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容\n3. 绝对不要使用'data:'作为前缀\n4. 代码块格式：```java\n代码\n```"));
                
                // 获取对话历史
                List<AiChatDTO.MessageHistory> history = dialogMemoryService.getDialogHistory(userId, dto.getProblemId());
                if (!history.isEmpty()) {
                    for (AiChatDTO.MessageHistory msg : history) {
                        if ("user".equals(msg.getRole())) {
                            messages.add(new UserMessage(msg.getContent()));
                        } else {
                            messages.add(new SystemMessage(msg.getContent()));
                        }
                    }
                }
                
                // 添加增强上下文
                messages.add(new UserMessage(augmentedContext.toString()));
                
                // 保存用户消息到对话历史
                dialogMemoryService.saveDialogHistory(userId, dto.getProblemId(), "user", dto.getMessage());
                
                // 执行AI对话
                ChatClient chatClient = chatClientBuilder.build();
                StringBuilder fullResponse = new StringBuilder();
                
                chatClient.prompt()
                        .messages(messages)
                        .stream()
                        .content()
                        .subscribe(
                                content -> {
                                    try {
                                        if (content != null && !content.isEmpty()) {
                                            emitter.send(SseEmitter.event().data(content));
                                            fullResponse.append(content);
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                },
                                error -> {
                                    try {
                                        emitter.send(SseEmitter.event().data("❌ AI回复出错: " + error.getMessage()));
                                    } catch (IOException ignored) {}
                                    emitter.complete();
                                },
                                () -> {
                                    // 保存AI回复到对话历史
                                    if (!fullResponse.isEmpty()) {
                                        dialogMemoryService.saveDialogHistory(userId, dto.getProblemId(), "assistant", fullResponse.toString());
                                    }
                                    emitter.complete();
                                }
                        );
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("❌ 知识检索出错: " + e.getMessage()));
                } catch (IOException ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    @Override
    public SseEmitter analyzeErrorWithKnowledge(AiJudgeDTO dto, String errorInfo) {
        SseEmitter emitter = new SseEmitter(120_000L);

        executor.execute(() -> {
            try {
                Problem problem = problemMapper.selectById(dto.getProblemId());
                
                // 构建查询上下文
                StringBuilder contextBuilder = new StringBuilder();
                contextBuilder.append("题目: " + problem.getTitle() + "\n");
                contextBuilder.append("代码: " + dto.getCode() + "\n");
                contextBuilder.append("错误: " + errorInfo);
                
                // 检索相关知识
                List<String> knowledge = knowledgeRetrievalService.retrieveKnowledge(
                        "代码错误分析: " + errorInfo,
                        contextBuilder.toString(),
                        3
                );
                
                // 构建增强上下文
                StringBuilder augmentedContext = new StringBuilder();
                augmentedContext.append("## 相关知识\n");
                for (int i = 0; i < knowledge.size(); i++) {
                    augmentedContext.append((i + 1) + ". " + knowledge.get(i) + "\n\n");
                }
                augmentedContext.append("## 错误分析\n" + errorInfo);
                
                // 执行AI分析
                ChatClient chatClient = chatClientBuilder.build();
                chatClient.prompt()
                        .system("你是一位专业的编程错误分析专家，基于检索到的知识分析代码错误。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容\n3. 绝对不要使用'data:'作为前缀\n4. 代码块格式：```java\n代码\n```")
                        .user(augmentedContext.toString())
                        .stream()
                        .content()
                        .subscribe(
                                content -> {
                                    try {
                                        if (content != null && !content.isEmpty()) {
                                            emitter.send(SseEmitter.event().data(content));
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                },
                                error -> {
                                    try {
                                        emitter.send(SseEmitter.event().data("❌ 错误分析出错: " + error.getMessage()));
                                    } catch (IOException ignored) {}
                                    emitter.complete();
                                },
                                emitter::complete
                        );
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("❌ 错误分析出错: " + e.getMessage()));
                } catch (IOException ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }
}