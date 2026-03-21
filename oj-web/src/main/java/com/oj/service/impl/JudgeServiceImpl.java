package com.oj.service.impl;

import com.oj.config.Judge0Client;
import com.oj.constant.MqConstant;
import com.oj.constant.MessageConstant;
import com.oj.service.JudgeService;
import com.alibaba.fastjson.JSONObject;
import com.oj.dto.JudgeRunDTO;
import com.oj.dto.JudgeSubmitDTO;
import com.oj.dto.JudgeTaskMessage;
import com.oj.dto.MaliciousCodeDetectionResult;
import com.oj.entity.Problem;
import com.oj.entity.TestCase;
import com.oj.exception.BaseException;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubMissionMapper;
import com.oj.mapper.TestCaseMapper;
import com.oj.vo.JudgeResultVO;
import com.oj.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {
    @Autowired
    private Judge0Client judge0Client;

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private SubMissionMapper subMissionMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DefaultRedisScript<List> submitAndUpdateScript;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private com.oj.service.MaliciousCodeDetector maliciousCodeDetector;

    @Override
    public JudgeResultVO submit(JudgeSubmitDTO dto, Long userId) {
        Problem problem = problemMapper.selectById(dto.getProblemId());
        if (problem == null) {
            throw new BaseException("题目不存在: " + dto.getProblemId());
        }

//        // 1. AI 语法检测（同步，快速返回）
//        String syntaxError = checkSyntaxWithAi(dto.getCode(), dto.getLanguage());
//        if (syntaxError != null) {
//            log.info("AI语法检测发现错误: {}", syntaxError);
//            return JudgeResultVO.builder()
//                    .status("Syntax Error")
//                    .errorInfo("【AI语法检测】\n" + syntaxError)
//                    .testCasesPassed(0)
//                    .testCasesTotal(0)
//                    .problemId(dto.getProblemId())
//                    .build();
//        }

        // 2. 获取测试用例
        List<TestCase> testCases = testCaseMapper.selectByProblemId(dto.getProblemId());
        if (testCases == null || testCases.isEmpty()) {
            throw new BaseException(MessageConstant.TESTCASE_NOT_FOUND);
        }

        // 3. 恶意代码检测（正则检测，快速返回）
        MaliciousCodeDetectionResult maliciousResult = maliciousCodeDetector.detect(dto.getCode(), dto.getLanguage());
        if (!maliciousResult.isSafe()) {
            log.warn("检测到恶意代码: userId={}, problemId={}, message={}", userId, dto.getProblemId(), maliciousResult.getMessage());
            return JudgeResultVO.builder()
                    .status("Malicious Code")
                    .errorInfo("【安全检测】\n" + maliciousResult.getMessage())
                    .testCasesPassed(0)
                    .testCasesTotal(0)
                    .problemId(dto.getProblemId())
                    .build();
        }

        // 4. 生成本次提交的唯一 token
        String submissionToken = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis() / 1000;

        // 5. 执行 Lua 脚本：submit_and_update_v3
        // 比赛模式下 key 加 contest 前缀，与普通练习隔离
        String prefix = dto.getContestId() != null ? "contest:" + dto.getContestId() + ":" : "";
        String userStatusKey = prefix + "user:" + userId + ":problem:" + dto.getProblemId() + ":status";
        String solvedCountKey = prefix + "problem:" + dto.getProblemId() + ":solved_count";
        String submissionKey = "submission:" + submissionToken;
        String processingKey = prefix + "user:" + userId + ":problem:" + dto.getProblemId() + ":processing";
        String submitWindowKey = prefix + "user:" + userId + ":problem:" + dto.getProblemId() + ":submit_window";
        List<String> keys = Arrays.asList(
                userStatusKey,
                solvedCountKey,
                submissionKey,
                processingKey,
                submitWindowKey
        );

        // 根据用户角色调整限流参数
        int windowSize = 60;
        int maxSubmits = 5;

        List<Object> luaResult = stringRedisTemplate.execute(
                submitAndUpdateScript,
                keys,
                submissionToken,
                String.valueOf(currentTime),
                String.valueOf(windowSize),
                String.valueOf(maxSubmits)
        );

        log.info("Lua submit_and_update_v3 结果: {}", luaResult);

        // 6. 处理 Lua 返回结果
        if (luaResult == null || luaResult.size() < 2) {
            throw new BaseException(MessageConstant.LUA_EXECUTION_ERROR);
        }

        int success = Integer.parseInt(luaResult.get(0).toString());
        String status = luaResult.get(1).toString();

        // 限流
        if (success == 0 && "rate_limited".equals(status)) {
            int count = luaResult.size() >= 4 ? Integer.parseInt(luaResult.get(3).toString()) : 0;
            int waitTime = luaResult.size() >= 5 ? Integer.parseInt(luaResult.get(4).toString()) : 0;
            log.info("提交过于频繁，请" + waitTime + "秒后再试（窗口内已提交" + count + "次）");
            return JudgeResultVO.builder()
                    .status("Rate Limited")
                    .errorInfo("提交过于频繁，请" + waitTime + "秒后再试（窗口内已提交" + count + "次）")
                    .testCasesPassed(0)
                    .testCasesTotal(0)
                    .problemId(dto.getProblemId())
                    .build();
        }

        // 有提交正在判题中
        if (success == 0 && "already_processing".equals(status)) {
            String existingToken = luaResult.get(2) != null ? luaResult.get(2).toString() : "";
            return JudgeResultVO.builder()
                    .status("Processing")
                    .errorInfo("有提交正在判题中，请等待判题完成后再提交")
                    .testCasesPassed(0)
                    .testCasesTotal(0)
                    .problemId(dto.getProblemId())
                    .title(problem.getTitle())
                    .submitToken(existingToken)
                    .build();
        }

        // 7. 提交成功，获取 Judge0 语言ID 和限制
        int languageId = judge0Client.getLanguageId(dto.getLanguage());
        Float timeLimit = problem.getTimeLimitMs() != null ? problem.getTimeLimitMs() / 1000.0f : null;
        Integer memoryLimit = problem.getMemoryLimitMb() != null ? problem.getMemoryLimitMb() * 1024 : null;

        // 8. 构建判题任务消息
        JudgeTaskMessage taskMessage = JudgeTaskMessage.builder()
                .userId(userId)
                .problemId(dto.getProblemId())
                .code(dto.getCode())
                .language(dto.getLanguage())
                .submissionToken(submissionToken)
                .languageId(languageId)
                .testCasesTotal(testCases.size())
                .timeLimitSec(timeLimit)
                .memoryLimitKb(memoryLimit)
                .contestId(dto.getContestId())
                .build();

        // 9. 发送到判题任务队列
        rocketMQTemplate.convertAndSend(MqConstant.JUDGE_TASK_TOPIC, taskMessage);
        log.info("已发送判题任务到MQ: submissionToken={}, problemId={}, userId={}",
                submissionToken, dto.getProblemId(), userId);

        // 10. 立即返回 Pending 状态
        return JudgeResultVO.builder()
                .status("Pending")
                .testCasesPassed(0)
                .testCasesTotal(testCases.size())
                .problemId(dto.getProblemId())
                .title(problem.getTitle())
                .submitToken(submissionToken)
                .errorInfo("代码已提交，正在排队判题中...")
                .submitTime(LocalDateTime.now())
                .build();
    }

    @Override
    public JudgeResultVO run(JudgeRunDTO dto) {
        int languageId = judge0Client.getLanguageId(dto.getLanguage());

        String stdin = dto.getCustomInput();
        if (stdin == null || stdin.isEmpty()) {
            List<TestCase> testCases = testCaseMapper.selectByProblemId(dto.getProblemId());
            if (testCases != null && !testCases.isEmpty()) {
                TestCase sampleCase = testCases.stream()
                        .filter(tc -> Boolean.TRUE.equals(tc.getIsSample()))
                        .findFirst()
                        .orElse(testCases.get(0));
                stdin = sampleCase.getInputData();
            }
        }

        Problem problem = problemMapper.selectById(dto.getProblemId());
        Float timeLimit = (problem != null && problem.getTimeLimitMs() != null) ? problem.getTimeLimitMs() / 1000.0f : null;
        Integer memoryLimit = (problem != null && problem.getMemoryLimitMb() != null) ? problem.getMemoryLimitMb() * 1024 : null;

        try {
            JSONObject result = judge0Client.submitAndWait(dto.getCode(), languageId, stdin, null, timeLimit, memoryLimit);

            JSONObject status = result.getJSONObject("status");
            int statusId = status.getIntValue("id");

            String stdout = judge0Client.decodeField(result, "stdout");
            String stderr = judge0Client.decodeField(result, "stderr");
            String compileOutput = judge0Client.decodeField(result, "compile_output");

            String timeStr = result.getString("time");
            String memoryStr = result.getString("memory");
            int runtime = timeStr != null ? (int) (Float.parseFloat(timeStr) * 1000) : 0;
            int memory = memoryStr != null ? (int) Float.parseFloat(memoryStr) : 0;

            String errorMsg = null;
            if (compileOutput != null) errorMsg = compileOutput;
            else if (stderr != null) errorMsg = stderr;

            return JudgeResultVO.builder()
                    .status(judge0Client.parseStatus(statusId))
                    .runtimeMs(runtime)
                    .memoryKb(memory)
                    .stdout(stdout != null ? stdout.trim() : "")
                    .errorInfo(errorMsg)
                    .build();

        } catch (Exception e) {
            log.error("运行代码异常: {}", e.getMessage());
            return JudgeResultVO.builder()
                    .status("Runtime Error")
                    .errorInfo(MessageConstant.JUDGE0_ERROR + ": " + e.getMessage())
                    .build();
        }
    }

    private String checkSyntaxWithAi(String code, String language) {
        try {
            ChatClient chatClient = chatClientBuilder.build();

            String prompt = String.format("""
                你是一位专业的代码语法检查助手，回答简洁准确。只检查语法错误，不分析逻辑问题。
                
                请严格按照以下格式回复：
                - 如果代码有语法错误，请回复：语法错误：[具体错误描述]
                - 如果代码没有语法错误，请回复：语法正确
                
                代码：
```%s
                %s
```
                """, language.toLowerCase(), code);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response == null || response.isEmpty()) {
                return null;
            }

            String lowerResponse = response.toLowerCase();
            boolean hasError = lowerResponse.contains("语法错误")
                    || lowerResponse.contains("syntax error")
                    || lowerResponse.contains("编译错误")
                    || lowerResponse.contains("compile error")
                    || (lowerResponse.contains("错误") && !lowerResponse.contains("没有错误") && !lowerResponse.contains("无错误"));

            boolean isCorrect = lowerResponse.contains("语法正确")
                    || lowerResponse.contains("没有语法错误")
                    || lowerResponse.contains("无语法错误")
                    || lowerResponse.contains("代码语法正确");

            if (isCorrect && !hasError) {
                return null;
            }

            if (hasError) {
                return response;
            }

            return null;

        } catch (Exception e) {
            log.error("AI语法检测异常: {}", e.getMessage());
            return null;
        }
    }
}