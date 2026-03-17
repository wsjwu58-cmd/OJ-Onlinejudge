package com.oj.service.impl;

import com.oj.dto.AiChatDTO;
import com.oj.dto.AiJudgeDTO;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.entity.TestCase;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubMissionMapper;
import com.oj.mapper.TestCaseMapper;
import com.oj.service.AiJudgeService;
import com.oj.service.RAGService;
import com.oj.vo.JudgeResultVO;
import com.oj.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class AiJudgeServiceImpl implements AiJudgeService {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private SubMissionMapper subMissionMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private DialogMemoryService dialogMemoryService;

    @Autowired
    private RAGService ragService;

    private final ExecutorService executor = Executors.newCachedThreadPool();



    @Override
    public SseEmitter judgeByAiStream(AiJudgeDTO dto, Long userId) {
        SseEmitter emitter = new SseEmitter(180_000L);
        CountDownLatch latch = new CountDownLatch(1);

        emitter.onCompletion(() -> {
            log.info("SSE连接完成");
            latch.countDown();
        });

        emitter.onTimeout(() -> {
            log.warn("SSE连接超时");
            latch.countDown();
        });

        executor.execute(() -> {
            StringBuilder fullResponse = new StringBuilder();
            AtomicReference<String> finalStatus = new AtomicReference<>("AI Judging");
            AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

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
                
                ChatClient chatClient = chatClientBuilder.build();
                chatClient.prompt()
                        .system("你是一位专业的OJ（在线判题系统）AI评审助手，回答简洁准确，使用中文。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容（不要用\"内容\"这种格式）\n3. 绝对不要使用'data:'作为前缀\n4. 代码块格式：```java\\n代码\\n```\n5. 每个部分之间用空行分隔")
                        .user(prompt)
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
                                        log.error("SSE发送失败", e);
                                        errorRef.set(e);
                                    }
                                },
                                error -> {
                                    log.error("AI流式响应错误", error);
                                    errorRef.set(error);
                                    try {
                                        emitter.send(SseEmitter.event().data("\n\n❌ AI判题出错: " + error.getMessage()));
                                    } catch (IOException ignored) {}
                                    emitter.complete();
                                },
                                () -> {
                                    if (errorRef.get() == null) {
                                        String status = extractStatus(fullResponse.toString());
                                        finalStatus.set(status);
                                        saveSubmission(dto, userId, finalStatus.get(), fullResponse.toString());
                                    }
                                    emitter.complete();
                                }
                        );

                latch.await(180, TimeUnit.SECONDS);

            } catch (Exception e) {
                log.error("AI 判题异常: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().data("\n\n❌ AI 判题出错: " + e.getMessage()));
                } catch (IOException ignored) {}
                saveSubmission(dto, userId, "AI Error", e.getMessage());
                emitter.complete();
            }
        });

        return emitter;
    }

    @Override
    public SseEmitter syntaxCheck(AiJudgeDTO dto) {
        SseEmitter emitter = new SseEmitter(60_000L);

        executor.execute(() -> {
            try {
                String prompt = buildSyntaxCheckPrompt(dto.getCode(), dto.getLanguage());
                
                ChatClient chatClient = chatClientBuilder.build();
                chatClient.prompt()
                        .system("你是一位专业的代码语法检查助手，回答简洁准确。只检查语法错误，不分析逻辑问题。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容\n3. 绝对不要使用'data:'作为前缀\n4. 代码块格式：```java\\n代码\\n```")
                        .user(prompt)
                        .stream()
                        .content()
                        .subscribe(
                                content -> {
                                    try {
                                        if (content != null && !content.isEmpty()) {
                                            emitter.send(SseEmitter.event().data(content));
                                        }
                                    } catch (IOException e) {
                                        log.error("SSE发送失败", e);
                                    }
                                },
                                error -> {
                                    log.error("语法检测错误", error);
                                    try {
                                        emitter.send(SseEmitter.event().data("❌ 语法检测出错: " + error.getMessage()));
                                    } catch (IOException ignored) {}
                                    emitter.complete();
                                },
                                emitter::complete
                        );
            } catch (Exception e) {
                log.error("语法检测异常: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().data("❌ 语法检测出错: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    @Override
    public SseEmitter analyzeError(AiJudgeDTO dto, JudgeResultVO judgeResult) {
        SseEmitter emitter = new SseEmitter(120_000L);

        executor.execute(() -> {
            try {
                Problem problem = problemMapper.selectById(dto.getProblemId());
                String prompt = buildErrorAnalysisPrompt(problem, dto.getCode(), dto.getLanguage(), judgeResult);
                
                ChatClient chatClient = chatClientBuilder.build();
                chatClient.prompt()
                        .system("你是一位专业的编程导师，帮助用户分析代码错误原因。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容\n3. 绝对不要使用'data:'作为前缀\n4. 代码块格式：```java\\n代码\\n```\n5. 每个部分之间用空行分隔")
                        .user(prompt)
                        .stream()
                        .content()
                        .subscribe(
                                content -> {
                                    try {
                                        if (content != null && !content.isEmpty()) {
                                            emitter.send(SseEmitter.event().data(content));
                                        }
                                    } catch (IOException e) {
                                        log.error("SSE发送失败", e);
                                    }
                                },
                                error -> {
                                    log.error("错误分析错误", error);
                                    try {
                                        emitter.send(SseEmitter.event().data("❌ 错误分析出错: " + error.getMessage()));
                                    } catch (IOException ignored) {}
                                    emitter.complete();
                                },
                                emitter::complete
                        );
            } catch (Exception e) {
                log.error("错误分析异常: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().data("❌ 错误分析出错: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    @Override
    public SseEmitter chat(AiChatDTO dto) {
        // 使用RAG服务进行增强对话
        return ragService.chatWithKnowledge(dto);
    }

    @Override
    public SseEmitter getHint(AiChatDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L);

        executor.execute(() -> {
            try {
                Problem problem = problemMapper.selectById(dto.getProblemId());
                String prompt = buildHintPrompt(problem, dto.getCode(), dto.getLanguage());
                
                ChatClient chatClient = chatClientBuilder.build();
                chatClient.prompt()
                        .system("你是一位专业的编程导师，给出解题提示但不直接给出完整答案。\n\n重要规则：\n1. 使用标准Markdown格式回复\n2. 绝对不要使用引号包裹内容\n3. 绝对不要使用'data:'作为前缀\n4. 代码块格式：```java\\n代码\\n```\n5. 每个部分之间用空行分隔")
                        .user(prompt)
                        .stream()
                        .content()
                        .subscribe(
                                content -> {
                                    try {
                                        if (content != null && !content.isEmpty()) {
                                            emitter.send(SseEmitter.event().data(content));
                                        }
                                    } catch (IOException e) {
                                        log.error("SSE发送失败", e);
                                    }
                                },
                                error -> {
                                    log.error("获取提示错误", error);
                                    try {
                                        emitter.send(SseEmitter.event().data("❌ 获取提示出错: " + error.getMessage()));
                                    } catch (IOException ignored) {}
                                    emitter.complete();
                                },
                                emitter::complete
                        );
            } catch (Exception e) {
                log.error("获取提示异常: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().data("❌ 获取提示出错: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    private String buildSyntaxCheckPrompt(String code, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的代码审查助手。请检查以下代码是否存在语法错误。\n\n");
        sb.append("**语言：** ").append(language).append("\n");
        sb.append("```").append(language.toLowerCase()).append("\n");
        sb.append(code).append("\n");
        sb.append("```\n\n");
        sb.append("## 回复格式要求（非常重要！！！）\n");
        sb.append("1. 请严格使用Markdown格式回复，使用**加粗**作为标题\n");
        sb.append("2. 绝对不要使用任何'data:'前缀或引号包裹内容\n");
        sb.append("3. 回复格式如下：\n\n");
        sb.append("**语法检查结果**：如果代码有语法错误，请指出具体错误位置和原因；如果没有语法错误，回复语法正确\n\n");
        sb.append("**潜在问题**：指出代码中可能存在的潜在问题（如未使用的变量、逻辑问题等）\n\n");
        sb.append("**改进建议**：给出代码改进建议\n\n");
        sb.append("请直接按照上述格式输出，不要添加任何其他前缀或格式标记。");
        return sb.toString();
    }

    private String buildErrorAnalysisPrompt(Problem problem, String code, String language, JudgeResultVO judgeResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的编程导师。用户提交的代码在判题时出现了错误，请帮助分析原因。\n\n");
        
        if (problem != null) {
            sb.append("## 题目信息\n");
            sb.append("**标题：** ").append(problem.getTitle()).append("\n");
            if (problem.getContent() != null) {
                String content = problem.getContent();
                if (content.length() > 1500) {
                    content = content.substring(0, 1500) + "...";
                }
                sb.append("**描述：**\n").append(content).append("\n\n");
            }
        }

        sb.append("## 用户提交的代码\n");
        sb.append("**语言：** ").append(language).append("\n");
        sb.append("```").append(language.toLowerCase()).append("\n");
        sb.append(code).append("\n");
        sb.append("```\n\n");

        sb.append("## 判题结果\n");
        sb.append("**状态：** ").append(judgeResult.getStatus()).append("\n");
        if (judgeResult.getRuntimeMs() != null && judgeResult.getRuntimeMs() > 0) {
            sb.append("**运行时间：** ").append(judgeResult.getRuntimeMs()).append(" ms\n");
        }
        if (judgeResult.getTestCasesPassed() != null && judgeResult.getTestCasesTotal() != null) {
            sb.append("**测试用例：** ").append(judgeResult.getTestCasesPassed()).append("/").append(judgeResult.getTestCasesTotal()).append("\n");
        }
        if (judgeResult.getErrorInfo() != null) {
            sb.append("**错误信息：**\n```\n").append(judgeResult.getErrorInfo()).append("\n```\n");
        }

        sb.append("\n## 回复格式要求（非常重要！！！）\n");
        sb.append("1. 请严格使用Markdown格式回复，使用**加粗**作为标题\n");
        sb.append("2. 绝对不要使用任何'data:'前缀或引号包裹内容\n");
        sb.append("3. 回复格式如下：\n\n");
        sb.append("**错误原因**：分析代码为什么会出现这个错误\n\n");
        sb.append("**问题定位**：指出具体是哪部分代码有问题\n\n");
        sb.append("**修复建议**：给出具体的修改方案\n\n");
        sb.append("**正确思路**：简要说明正确的解题思路\n\n");
        sb.append("请直接按照上述格式输出，不要添加任何其他前缀或格式标记。");

        return sb.toString();
    }

    private String buildChatPrompt(Problem problem, AiChatDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的编程导师，正在帮助用户解决编程问题。\n\n");
        
        if (problem != null) {
            sb.append("## 当前题目\n");
            sb.append("**标题：** ").append(problem.getTitle()).append("\n");
            if (problem.getDifficulty() != null) {
                sb.append("**难度：** ").append(problem.getDifficulty()).append("\n");
            }
        }

        if (dto.getCode() != null && !dto.getCode().isEmpty()) {
            sb.append("\n## 用户当前代码\n");
            sb.append("**语言：** ").append(dto.getLanguage() != null ? dto.getLanguage() : "未知").append("\n");
            sb.append("```").append(dto.getLanguage() != null ? dto.getLanguage().toLowerCase() : "").append("\n");
            sb.append(dto.getCode()).append("\n");
            sb.append("```\n");
        }

        sb.append("\n## 用户问题\n");
        sb.append(dto.getMessage()).append("\n\n");
        
        sb.append("## 回复格式要求\n");
        sb.append("1. 请使用Markdown格式回复\n");
        sb.append("2. 绝对不要使用任何'data:'前缀或引号包裹内容\n");

        return sb.toString();
    }

    private String buildHintPrompt(Problem problem, String code, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的编程导师。请根据题目和用户的代码，给出解题提示（不要直接给出完整答案）。\n\n");

        if (problem != null) {
            sb.append("## 题目信息\n");
            sb.append("**标题：** ").append(problem.getTitle()).append("\n");
            if (problem.getContent() != null) {
                String content = problem.getContent();
                if (content.length() > 1500) {
                    content = content.substring(0, 1500) + "...";
                }
                sb.append("**描述：**\n").append(content).append("\n\n");
            }
            if (problem.getDifficulty() != null) {
                sb.append("**难度：** ").append(problem.getDifficulty()).append("\n");
            }
        }

        if (code != null && !code.isEmpty()) {
            sb.append("## 用户当前代码\n");
            sb.append("```").append(language != null ? language.toLowerCase() : "").append("\n");
            sb.append(code).append("\n");
            sb.append("```\n\n");
        }

        sb.append("## 回复格式要求（非常重要！！！）\n");
        sb.append("1. 请严格使用Markdown格式回复，使用**加粗**作为标题\n");
        sb.append("2. 绝对不要使用任何'data:'前缀或引号包裹内容\n");
        sb.append("3. 回复格式如下：\n\n");
        sb.append("**解题思路提示**：引导用户思考正确的解题方向\n\n");
        sb.append("**关键点提示**：指出解题的关键点\n\n");
        sb.append("**可能的优化方向**：如果用户已有代码，指出可以优化的地方\n\n");
        sb.append("注意：不要直接给出完整代码答案，只给提示和引导。\n\n");
        sb.append("请直接按照上述格式输出，不要添加任何其他前缀或格式标记。");

        return sb.toString();
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

    private void saveSubmission(AiJudgeDTO dto, Long userId, String status, String aiResponse) {
        try {
            Submission submission = new Submission();
            submission.setUserId(userId);
            submission.setProblemId(dto.getProblemId());
            submission.setCode(dto.getCode());
            submission.setLanguage(dto.getLanguage());
            submission.setStatus(status);
            submission.setRuntimeMs(0);
            submission.setMemoryKb(0);
            submission.setTestCasesPassed("Accepted".equals(status) ? 1 : 0);
            submission.setTestCasesTotal(0);
            if (aiResponse != null && aiResponse.length() > 2000) {
                submission.setErrorInfo(aiResponse.substring(0, 2000));
            } else {
                submission.setErrorInfo(aiResponse);
            }
            submission.setSubmitTime(LocalDateTime.now());
            subMissionMapper.insert(submission);

            updateAcceptance(dto.getProblemId());

            Map<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            map.put("problemId", dto.getProblemId());
            map.put("content", "AI判题完成 记录ID" + submission.getId());
            webSocketServer.sendToAllClient(com.alibaba.fastjson.JSONObject.toJSONString(map));

            log.info("AI 判题记录已保存: submissionId={}, status={}", submission.getId(), status);
        } catch (Exception e) {
            log.error("保存 AI 判题记录失败: {}", e.getMessage(), e);
        }
    }

    private void updateAcceptance(Integer problemId) {
        try {
            Integer total = subMissionMapper.selectCountProblem(problemId, null);
            Integer accepted = subMissionMapper.selectCountProblem(problemId, "Accepted");
            if (total != null && total > 0) {
                double percent = (accepted.doubleValue() / total) * 100;
                Problem problem = problemMapper.selectById(problemId);
                problem.setAcceptance(BigDecimal.valueOf(percent));
                problemMapper.updateById(problem);
            }
        } catch (Exception e) {
            log.error("更新通过率失败: {}", e.getMessage());
        }
    }

    private String extractStatus(String aiResponse) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return "AI Error";
        }

        String text = aiResponse.toLowerCase();

        if (text.contains("accepted") || text.contains("通过") || text.contains("代码正确")) {
            if (text.contains("wrong answer") || text.contains("答案错误") || text.contains("不正确")) {
                return "Wrong Answer";
            }
            return "Accepted";
        }
        if (text.contains("wrong answer") || text.contains("答案错误")) {
            return "Wrong Answer";
        }
        if (text.contains("time limit") || text.contains("超时")) {
            return "Time Limit Exceeded";
        }
        if (text.contains("memory limit") || text.contains("内存超限")) {
            return "Memory Limit Exceeded";
        }
        if (text.contains("runtime error") || text.contains("运行错误")) {
            return "Runtime Error";
        }
        if (text.contains("compile error") || text.contains("编译错误")) {
            return "Compile Error";
        }

        return "AI Reviewed";
    }
}
