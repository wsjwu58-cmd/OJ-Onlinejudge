package com.oj.service.impl;

import com.oj.dto.AiChatDTO;
import com.oj.dto.AiJudgeDTO;
import com.oj.entity.Problem;
import com.oj.mapper.ProblemMapper;
import com.oj.service.RAGService;
import com.oj.service.KnowledgeRetrievalService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RAGServiceImpl implements RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGServiceImpl.class);

    @Autowired
    private StreamingChatLanguageModel streamingChatLanguageModel;

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
                
                StringBuilder contextBuilder = new StringBuilder();
                if (problem != null) {
                    contextBuilder.append("题目: ").append(problem.getTitle()).append("\n");
                }
                if (dto.getCode() != null) {
                    contextBuilder.append("代码: ").append(dto.getCode()).append("\n");
                }
                contextBuilder.append("问题: ").append(dto.getMessage());
                
                List<String> knowledge = knowledgeRetrievalService.retrieveKnowledge(
                        dto.getMessage(),
                        contextBuilder.toString(),
                        3
                );
                
                StringBuilder augmentedContext = new StringBuilder();
                augmentedContext.append("## 相关知识\n");
                for (int i = 0; i < knowledge.size(); i++) {
                    augmentedContext.append((i + 1)).append(". ").append(knowledge.get(i)).append("\n\n");
                }
                augmentedContext.append("## 用户问题\n").append(dto.getMessage());
                
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from("你是一位专业的编程导师，基于检索到的知识回答用户问题。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容\n3. 绝对不要使用'data:'作为前缀\n4. 代码块格式：```java\n代码\n```"));
                
                List<AiChatDTO.MessageHistory> history = dialogMemoryService.getDialogHistory(userId, dto.getProblemId());
                if (!history.isEmpty()) {
                    for (AiChatDTO.MessageHistory msg : history) {
                        if ("user".equals(msg.getRole())) {
                            messages.add(UserMessage.from(msg.getContent()));
                        } else {
                            messages.add(AiMessage.from(msg.getContent()));
                        }
                    }
                }
                
                messages.add(UserMessage.from(augmentedContext.toString()));
                
                dialogMemoryService.saveDialogHistory(userId, dto.getProblemId(), "user", dto.getMessage());
                
                StringBuilder fullResponse = new StringBuilder();
                
                ChatRequest request = ChatRequest.builder()
                        .messages(messages)
                        .build();
                
                streamingChatLanguageModel.chat(request, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        try {
                            if (partialResponse != null && !partialResponse.isEmpty()) {
                                emitter.send(SseEmitter.event().data(partialResponse));
                                fullResponse.append(partialResponse);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        if (!fullResponse.isEmpty()) {
                            dialogMemoryService.saveDialogHistory(userId, dto.getProblemId(), "assistant", fullResponse.toString());
                        }
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        try {
                            emitter.send(SseEmitter.event().data("❌ AI回复出错: " + error.getMessage()));
                        } catch (Exception ignored) {}
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("❌ 知识检索出错: " + e.getMessage()));
                } catch (Exception ignored) {}
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
                
                StringBuilder contextBuilder = new StringBuilder();
                contextBuilder.append("题目: ").append(problem.getTitle()).append("\n");
                contextBuilder.append("代码: ").append(dto.getCode()).append("\n");
                contextBuilder.append("错误: ").append(errorInfo);
                
                List<String> knowledge = knowledgeRetrievalService.retrieveKnowledge(
                        "代码错误分析: " + errorInfo,
                        contextBuilder.toString(),
                        3
                );
                
                StringBuilder augmentedContext = new StringBuilder();
                augmentedContext.append("## 相关知识\n");
                for (int i = 0; i < knowledge.size(); i++) {
                    augmentedContext.append((i + 1)).append(". ").append(knowledge.get(i)).append("\n\n");
                }
                augmentedContext.append("## 错误分析\n").append(errorInfo);
                
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from("你是一位专业的编程错误分析专家，基于检索到的知识分析代码错误。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容\n3. 绝对不要使用'data:'作为前缀\n4. 代码块格式：```java\n代码\n```"));
                messages.add(UserMessage.from(augmentedContext.toString()));
                
                ChatRequest request = ChatRequest.builder()
                        .messages(messages)
                        .build();
                
                streamingChatLanguageModel.chat(request, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        try {
                            if (partialResponse != null && !partialResponse.isEmpty()) {
                                emitter.send(SseEmitter.event().data(partialResponse));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        try {
                            emitter.send(SseEmitter.event().data("❌ 错误分析出错: " + error.getMessage()));
                        } catch (Exception ignored) {}
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("❌ 错误分析出错: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    @Override
    public SseEmitter getHintWithKnowledge(AiChatDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L);

        executor.execute(() -> {
            try {
                Problem problem = dto.getProblemId() != null ? problemMapper.selectById(dto.getProblemId()) : null;
                
                StringBuilder contextBuilder = new StringBuilder();
                if (problem != null) {
                    contextBuilder.append("题目: ").append(problem.getTitle()).append("\n");
                    if (problem.getContent() != null) {
                        contextBuilder.append("题目描述: ").append(problem.getContent()).append("\n");
                    }
                }
                
                List<String> knowledge = knowledgeRetrievalService.retrieveKnowledge(
                        "解题提示: " + (problem != null ? problem.getTitle() : ""),
                        contextBuilder.toString(),
                        3
                );
                
                StringBuilder augmentedContext = new StringBuilder();
                if (!knowledge.isEmpty()) {
                    augmentedContext.append("## 相关知识\n");
                    for (int i = 0; i < knowledge.size(); i++) {
                        augmentedContext.append((i + 1)).append(". ").append(knowledge.get(i)).append("\n\n");
                    }
                    augmentedContext.append("## 题目提示\n");
                }
                
                String prompt = String.format("""
                        请为以下编程题目提供解题思路提示（不要直接给出完整代码）：
                        
                        题目：%s
                        
                        题目描述：
                        %s
                        
                        请用中文回答，包括：
                        1. 问题分析
                        2. 解题思路提示
                        3. 可能用到的算法或数据结构
                        4. 注意事项
                        """,
                        problem != null ? problem.getTitle() : "未知题目",
                        problem != null && problem.getContent() != null ? problem.getContent() : dto.getMessage());
                
                augmentedContext.append(prompt);
                
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from("你是一位专业的编程导师，擅长引导学生思考，基于检索到的知识提供提示。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容\n3. 绝对不要使用'data:'作为前缀\n4. 只给提示，不要直接给出完整答案"));
                messages.add(UserMessage.from(augmentedContext.toString()));
                
                ChatRequest request = ChatRequest.builder()
                        .messages(messages)
                        .build();
                
                streamingChatLanguageModel.chat(request, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        try {
                            if (partialResponse != null && !partialResponse.isEmpty()) {
                                emitter.send(SseEmitter.event().data(partialResponse));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        try {
                            emitter.send(SseEmitter.event().data("❌ 获取提示出错: " + error.getMessage()));
                        } catch (Exception ignored) {}
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("❌ 获取提示出错: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }
}
