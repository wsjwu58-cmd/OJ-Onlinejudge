package com.oj.ai.service.impl;

import com.oj.ai.dto.AiChatDTO;
import com.oj.ai.dto.AiJudgeDTO;
import com.oj.ai.service.AiJudgeService;
import com.oj.ai.service.RAGService;
import com.oj.api.ProblemClient;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.api.dto.TestCaseFeignDTO;
import com.oj.common.result.Result;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
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
public class AiJudgeServiceImpl implements AiJudgeService {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private StreamingChatModel streamingChatModel;

    @Autowired
    private ProblemClient problemClient;

    @Autowired
    private RAGService ragService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public String judgeCode(AiJudgeDTO dto) {
        log.info("AI判题请求: problemId={}, language={}", dto.getProblemId(), dto.getLanguage());
        try {
            String prompt = buildJudgePrompt(dto);
            return chatModel.chat(prompt);
        } catch (Exception e) {
            log.error("AI判题失败", e);
            return "AI判题失败: " + e.getMessage();
        }
    }

    @Override
    public String checkSyntax(String code, String language) {
        String prompt = String.format("""
                请检查以下%s代码是否存在语法错误，如果存在请指出具体位置和修改建议：
                ```%s
                %s
                ```
                请用中文回答。
                """, language, language.toLowerCase(), code);
        try { return chatModel.chat(prompt); }
        catch (Exception e) { return "语法检查失败: " + e.getMessage(); }
    }

    @Override
    public String analyzeError(String code, String errorMessage, String language) {
        String prompt = String.format("""
                请分析以下%s代码运行时出现的错误，并给出修改建议：
                代码：```%s\n%s\n```
                错误信息：%s
                请用中文回答。
                """, language, language.toLowerCase(), code, errorMessage);
        try { return chatModel.chat(prompt); }
        catch (Exception e) { return "错误分析失败: " + e.getMessage(); }
    }

    @Override
    public String getHint(String problemTitle, String problemContent, String language) {
        String prompt = String.format("""
                请为以下编程题目提供解题思路提示（不要直接给出完整代码）：
                题目：%s\n题目描述：%s\n编程语言：%s
                请用中文回答。
                """, problemTitle, problemContent, language);
        try { return chatModel.chat(prompt); }
        catch (Exception e) { return "获取提示失败: " + e.getMessage(); }
    }

    @Override
    public String chat(String message) {
        try { return chatModel.chat(message); }
        catch (Exception e) { return "AI对话失败: " + e.getMessage(); }
    }

    @Override
    public SseEmitter judgeByAiStream(AiJudgeDTO dto, Long userId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> {
            try {
                Result<ProblemFeignDTO> result = problemClient.getProblemById(dto.getProblemId());
                if (result == null || result.getCode() != 1 || result.getData() == null) {
                    emitter.send(SseEmitter.event().data("题目不存在"));
                    emitter.complete();
                    return;
                }
                ProblemFeignDTO problem = result.getData();

                Result<List<TestCaseFeignDTO>> tcResult = problemClient.getTestCasesByProblemId(dto.getProblemId());
                boolean hasTestCases = tcResult != null && tcResult.getCode() == 1 && tcResult.getData() != null && !tcResult.getData().isEmpty();

                String prompt = buildJudgePrompt(problem, dto.getCode(), dto.getLanguage(), hasTestCases);
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from("你是一位专业的OJ AI评审助手，回答简洁准确，使用中文。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容\n3. 绝对不要使用'data:'作为前缀\n4. 代码块格式：```java\n代码\n```"));
                messages.add(UserMessage.from(prompt));

                ChatRequest request = ChatRequest.builder().messages(messages).build();
                streamingChatModel.chat(request, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        try { if (partialResponse != null && !partialResponse.isEmpty()) emitter.send(SseEmitter.event().data(partialResponse)); }
                        catch (Exception ignored) {}
                    }
                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) { emitter.complete(); }
                    @Override
                    public void onError(Throwable error) {
                        try { emitter.send(SseEmitter.event().data("AI判题出错: " + error.getMessage())); } catch (Exception ignored) {}
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                try { emitter.send(SseEmitter.event().data("AI判题出错: " + e.getMessage())); } catch (Exception ignored) {}
                emitter.complete();
            }
        });
        return emitter;
    }

    @Override
    public SseEmitter syntaxCheck(AiJudgeDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> {
            try {
                String prompt = String.format("请检查以下%s代码是否存在语法错误：\n```%s\n%s\n```\n请用中文回答。", dto.getLanguage(), dto.getLanguage().toLowerCase(), dto.getCode());
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from("你是一位专业的代码审查专家。使用标准Markdown格式回复，不要使用引号包裹内容。"));
                messages.add(UserMessage.from(prompt));
                ChatRequest request = ChatRequest.builder().messages(messages).build();
                streamingChatModel.chat(request, new StreamingChatResponseHandler() {
                    @Override public void onPartialResponse(String partialResponse) { try { if (partialResponse != null && !partialResponse.isEmpty()) emitter.send(SseEmitter.event().data(partialResponse)); } catch (Exception ignored) {} }
                    @Override public void onCompleteResponse(ChatResponse completeResponse) { emitter.complete(); }
                    @Override public void onError(Throwable error) { try { emitter.send(SseEmitter.event().data("语法检查出错: " + error.getMessage())); } catch (Exception ignored) {} emitter.complete(); }
                });
            } catch (Exception e) { try { emitter.send(SseEmitter.event().data("语法检查出错: " + e.getMessage())); } catch (Exception ignored) {} emitter.complete(); }
        });
        return emitter;
    }

    @Override
    public SseEmitter analyzeError(AiJudgeDTO dto, String errorInfo) {
        return ragService.analyzeErrorWithKnowledge(dto, errorInfo);
    }

    @Override
    public SseEmitter chat(AiChatDTO dto) {
        return ragService.chatWithKnowledge(dto);
    }

    @Override
    public SseEmitter getHint(AiChatDTO dto) {
        return ragService.getHintWithKnowledge(dto);
    }

    private String buildJudgePrompt(AiJudgeDTO dto) {
        StringBuilder prompt = new StringBuilder("请作为编程题目的判题系统，分析以下代码的正确性：\n\n");
        try {
            Result<ProblemFeignDTO> result = problemClient.getProblemById(dto.getProblemId());
            if (result != null && result.getCode() == 1 && result.getData() != null) {
                ProblemFeignDTO problem = result.getData();
                prompt.append(String.format("题目：%s\n\n", problem.getTitle()));
                if (problem.getContent() != null) prompt.append(String.format("题目描述：\n%s\n\n", problem.getContent()));
            }
        } catch (Exception ignored) {}
        prompt.append(String.format("编程语言：%s\n\n", dto.getLanguage()));
        prompt.append(String.format("代码：\n```%s\n%s\n```\n\n", dto.getLanguage().toLowerCase(), dto.getCode()));
        prompt.append("请用中文回答，包括：1. 代码正确性评估 2. 代码逻辑分析 3. 可能存在的问题 4. 改进建议");
        return prompt.toString();
    }

    private String buildJudgePrompt(ProblemFeignDTO problem, String code, String language, boolean hasTestCases) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的编程竞赛评审老师。请对以下代码进行判题分析。\n\n");
        sb.append("## 题目信息\n**标题：** ").append(problem.getTitle()).append("\n");
        String content = problem.getContent();
        if (content != null && !content.isEmpty()) {
            if (content.length() > 2000) content = content.substring(0, 2000) + "...";
            sb.append("**描述：**\n").append(content).append("\n\n");
        }
        if (problem.getDifficulty() != null) sb.append("**难度：** ").append(problem.getDifficulty()).append("\n");
        sb.append("\n## 用户提交的代码\n**语言：** ").append(language).append("\n```").append(language.toLowerCase()).append("\n").append(code).append("\n```\n\n");
        if (!hasTestCases) sb.append("注意：该题目暂无测试用例，请自行构造测试数据进行分析。\n\n");
        sb.append("## 回复格式要求\n1. 使用Markdown格式回复\n2. 包含判定结果、代码分析、测试验证、复杂度分析、改进建议\n");
        return sb.toString();
    }
}
