package com.oj.service.impl;

import com.oj.dto.AiChatDTO;
import com.oj.dto.AiJudgeDTO;
import com.oj.entity.Problem;
import com.oj.entity.TestCase;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.TestCaseMapper;
import com.oj.service.AiJudgeService;
import com.oj.service.RAGService;
import com.oj.vo.JudgeResultVO;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
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
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private StreamingChatLanguageModel streamingChatLanguageModel;

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private RAGService ragService;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    @Autowired
    private TestCaseMapper testCaseMapper;

    @Override
    public String judgeCode(AiJudgeDTO dto) {
        log.info("AI判题请求: problemId={}, language={}", dto.getProblemId(), dto.getLanguage());
        String prompt = buildJudgePrompt(dto);
        try {
            return chatLanguageModel.chat(prompt);
        } catch (Exception e) {
            log.error("AI判题失败", e);
            return "AI判题失败: " + e.getMessage();
        }
    }

    @Override
    public String checkSyntax(String code, String language) {
        log.info("语法检查请求: language={}", language);
        String prompt = String.format("""
                请检查以下%s代码是否存在语法错误，如果存在请指出具体位置和修改建议：
                
                ```%s
                %s
                ```
                
                请用中文回答，格式如下：
                1. 是否存在语法错误
                2. 错误位置（如果有）
                3. 修改建议（如果有）
                """, language, language.toLowerCase(), code);
        try {
            return chatLanguageModel.chat(prompt);
        } catch (Exception e) {
            log.error("语法检查失败", e);
            return "语法检查失败: " + e.getMessage();
        }
    }

    @Override
    public String analyzeError(String code, String errorMessage, String language) {
        log.info("错误分析请求: language={}", language);
        String prompt = String.format("""
                请分析以下%s代码运行时出现的错误，并给出修改建议：
                
                代码：
                ```%s
                %s
                ```
                
                错误信息：
                %s
                
                请用中文回答，包括：
                1. 错误原因分析
                2. 具体修改建议
                3. 修改后的代码示例
                """, language, language.toLowerCase(), code, errorMessage);
        try {
            return chatLanguageModel.chat(prompt);
        } catch (Exception e) {
            log.error("错误分析失败", e);
            return "错误分析失败: " + e.getMessage();
        }
    }

    @Override
    public String getHint(String problemTitle, String problemContent, String language) {
        log.info("获取提示请求: problemTitle={}", problemTitle);
        String prompt = String.format("""
                请为以下编程题目提供解题思路提示（不要直接给出完整代码）：
                
                题目：%s
                
                题目描述：
                %s
                
                编程语言：%s
                
                请用中文回答，包括：
                1. 问题分析
                2. 解题思路提示
                3. 可能用到的算法或数据结构
                4. 注意事项
                """, problemTitle, problemContent, language);
        try {
            return chatLanguageModel.chat(prompt);
        } catch (Exception e) {
            log.error("获取提示失败", e);
            return "获取提示失败: " + e.getMessage();
        }
    }

    @Override
    public String chat(String message) {
        log.info("AI对话请求: message={}", message);
        try {
            return chatLanguageModel.chat(message);
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return "AI对话失败: " + e.getMessage();
        }
    }

    @Override
    public SseEmitter judgeByAiStream(AiJudgeDTO dto, Long userId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> {
            try {
                Problem problem = problemMapper.selectById(dto.getProblemId());
                if (problem == null) {
                    emitter.send(SseEmitter.event().data("题目不存在"));
                    emitter.complete();
                    return;
                }
                List<TestCase> testCases = testCaseMapper.selectByProblemId(dto.getProblemId());
                boolean hasTestCases = testCases != null && !testCases.isEmpty();
                String prompt = buildJudgePrompt(problem, dto.getCode(), dto.getLanguage(), hasTestCases);

                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from("你是一位专业的OJ（在线判题系统）AI评审助手，回答简洁准确，使用中文。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容（不要用\"内容\"这种格式）\n3. 绝对不要使用'data:'作为前缀\n4. 代码块格式：```java\\n代码\\n```\n5. 每个部分之间用空行分隔"));
                messages.add(UserMessage.from(prompt));

                ChatRequest request = ChatRequest.builder().messages(messages).build();

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
                            emitter.send(SseEmitter.event().data("❌ AI判题出错: " + error.getMessage()));
                        } catch (Exception ignored) {
                        }
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("❌ AI判题出错: " + e.getMessage()));
                } catch (Exception ignored) {
                }
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
                String prompt = String.format("""
                        请检查以下%s代码是否存在语法错误：
                        
                        ```%s
                        %s
                        ```
                        
                        请用中文回答，包括：
                        1. 是否存在语法错误
                        2. 错误位置和原因
                        3. 修改建议
                        """, dto.getLanguage(), dto.getLanguage().toLowerCase(), dto.getCode());

                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from("你是一位专业的代码审查专家。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容\n3. 绝对不要使用'data:'作为前缀"));
                messages.add(UserMessage.from(prompt));

                ChatRequest request = ChatRequest.builder().messages(messages).build();

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
                            emitter.send(SseEmitter.event().data("❌ 语法检查出错: " + error.getMessage()));
                        } catch (Exception ignored) {
                        }
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("❌ 语法检查出错: " + e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    @Override
    public SseEmitter analyzeError(AiJudgeDTO dto, JudgeResultVO judgeResult) {
        String errorInfo = judgeResult != null ? judgeResult.getErrorInfo() : "未知错误";
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
        return buildJudgePrompt(dto, problemMapper.selectById(dto.getProblemId()));
    }

    private String buildJudgePrompt(AiJudgeDTO dto, Problem problem) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请作为编程题目的判题系统，分析以下代码的正确性：\n\n");

        if (problem != null) {
            prompt.append(String.format("题目：%s\n\n", problem.getTitle()));
            if (problem.getContent() != null) {
                prompt.append(String.format("题目描述：\n%s\n\n", problem.getContent()));
            }
        }

        prompt.append(String.format("编程语言：%s\n\n", dto.getLanguage()));
        prompt.append(String.format("代码：\n```%s\n%s\n```\n\n", dto.getLanguage().toLowerCase(), dto.getCode()));

        prompt.append("""
                请用中文回答，包括以下内容：
                1. 代码正确性评估（正确/部分正确/错误）
                2. 代码逻辑分析
                3. 可能存在的问题
                4. 改进建议
                5. 如果代码有错误，给出修改后的代码示例
                """);

        return prompt.toString();
    }
    private String buildJudgePrompt(Problem problem, String code, String language, boolean hasTestCases) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的编程竞赛评审老师。请对以下代码进行判题分析。\n\n");

        sb.append("## 题目信息\n");
        sb.append("**标题：** ").append(problem.getTitle()).append("\n");

        String content = problem.getContent();
        if (content != null && !content.isEmpty()) {
            if (content.length() > 2000) {
                content = content.substring(0, 2000) + "...";
            }
            sb.append("**描述：**\n").append(content).append("\n\n");
        }

        if (problem.getDifficulty() != null) {
            sb.append("**难度：** ").append(problem.getDifficulty()).append("\n");
        }

        sb.append("\n## 用户提交的代码\n");
        sb.append("**语言：** ").append(language).append("\n");
        sb.append("```").append(language.toLowerCase()).append("\n");
        sb.append(code).append("\n");
        sb.append("```\n\n");

        if (!hasTestCases) {
            sb.append("注意：该题目暂无测试用例，请你根据题目描述自行构造测试数据进行分析。\n\n");
        }

        sb.append("## 回复格式要求（非常重要！！！）\n");
        sb.append("1. 请严格使用Markdown格式回复\n");
        sb.append("2. 绝对不要使用任何'data:'前缀或引号包裹内容\n");
        sb.append("3. 代码块必须使用正确的格式：```java\\n代码内容\\n```\n");
        sb.append("4. 每个测试用例之间用空行分隔\n");
        sb.append("5. 回复格式示例如下：\n\n");
        sb.append("**判定结果**：Accepted 或 Wrong Answer\n\n");
        sb.append("**代码分析**：\n分析代码的思路是否正确，每个要点换行显示\n\n");
        sb.append("**测试验证**：\n");
        sb.append("测试用例1：\n");
        sb.append("- 输入：xxx\n");
        sb.append("- 预期输出：xxx\n");
        sb.append("- 实际输出：xxx\n\n");
        sb.append("测试用例2：\n");
        sb.append("- 输入：xxx\n");
        sb.append("- 预期输出：xxx\n");
        sb.append("- 实际输出：xxx\n\n");
        sb.append("**复杂度分析**：\n");
        sb.append("- 时间复杂度：O(n)\n");
        sb.append("- 空间复杂度：O(n)\n\n");
        sb.append("**改进建议**：\n");
        sb.append("如果代码有问题，给出修改建议和示例代码\n\n");
        sb.append("请直接按照上述格式输出，确保格式清晰易读。");

        return sb.toString();
    }
}

