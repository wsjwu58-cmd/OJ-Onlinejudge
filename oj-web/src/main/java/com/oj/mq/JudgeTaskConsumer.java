package com.oj.mq;

import com.alibaba.fastjson.JSONObject;
import com.oj.config.Judge0Client;
import com.oj.config.JudgeMetrics;
import com.oj.constant.MqConstant;
import com.oj.dto.DatabaseUpdateMessage;
import com.oj.dto.JudgeTaskMessage;
import com.oj.entity.TestCase;
import com.oj.mapper.TestCaseMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 判题消费者：从 MQ 消费判题任务
 * 1. 逐个测试用例提交 Judge0 并轮询结果
 * 2. 执行 Lua 脚本原子性更新 Redis
 * 3. 如果首次 AC，发送消息到 database-update-topic
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.JUDGE_TASK_TOPIC,
        consumerGroup = MqConstant.JUDGE_TASK_CONSUMER_GROUP,
        maxReconsumeTimes = 3
)
public class JudgeTaskConsumer implements RocketMQListener<JudgeTaskMessage> {

    @Autowired
    private Judge0Client judge0Client;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DefaultRedisScript<List> updateResultScript;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private Executor judgeExecutor;

    @Autowired
    private JudgeMetrics judgeMetrics;

    @Override
    public void onMessage(JudgeTaskMessage msg) {
        long totalStart = System.currentTimeMillis();

        log.info("收到判题任务: submissionToken={}, problemId={}, userId={}",
                msg.getSubmissionToken(), msg.getProblemId(), msg.getUserId());

        long startTime = System.currentTimeMillis();
        List<TestCase> testCases = testCaseMapper.selectByProblemId(msg.getProblemId());
        judgeMetrics.recordTestCaseQuery(System.currentTimeMillis() - startTime);

        if (testCases == null || testCases.isEmpty()) {
            log.error("题目 {} 没有测试用例，跳过", msg.getProblemId());
            sendDatabaseUpdate(msg, "Runtime Error", 0, 0, 0, 0, "该题目暂无测试用例", false);
            return;
        }

        List<CompletableFuture<TestCaseResult>> futures = new ArrayList<>();
        for (int i = 0; i < testCases.size(); i++) {
            final int index = i;
            TestCase tc = testCases.get(i);
            futures.add(CompletableFuture.supplyAsync(() ->
                    runSingleTestCase(msg, tc, index), judgeExecutor));
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        TestCaseResult finalResult;
        try {
            int timeoutSec = msg.getTimeLimitSec() != null ? msg.getTimeLimitSec().intValue() : 10;
            int totalTimeout = timeoutSec * testCases.size() + 30;

            long judge0Start = System.currentTimeMillis();
            allFutures.get(totalTimeout, java.util.concurrent.TimeUnit.SECONDS);
            judgeMetrics.recordJudge0Call(System.currentTimeMillis() - judge0Start);

            List<TestCaseResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            finalResult = aggregateResults(results);

        } catch (java.util.concurrent.TimeoutException e) {
            log.error("判题超时: submissionToken={}", msg.getSubmissionToken());
            finalResult = new TestCaseResult();
            finalResult.setStatus("Time Limit Exceeded");
            finalResult.setErrorInfo("判题超时，可能存在无限循环");
            finalResult.setMaxRuntime(0);
            finalResult.setMaxMemory(0);
            finalResult.setPassed(0);
            finalResult.setTotal(testCases.size());
        } catch (Exception e) {
            log.error("判题异常: {}", e.getMessage(), e);
            finalResult = new TestCaseResult();
            finalResult.setStatus("Runtime Error");
            finalResult.setErrorInfo("判题异常: " + e.getMessage());
            finalResult.setMaxRuntime(0);
            finalResult.setMaxMemory(0);
            finalResult.setPassed(0);
            finalResult.setTotal(testCases.size());
        }

        String prefix = msg.getContestId() != null ? "contest:" + msg.getContestId() + ":" : "";
        String userStatusKey = prefix + "user:" + msg.getUserId() + ":problem:" + msg.getProblemId() + ":status";
        String solvedCountKey = prefix + "problem:" + msg.getProblemId() + ":solved_count";
        String submissionKey = "submission:" + msg.getSubmissionToken();
        String userSolvedCountKey = prefix + "user:" + msg.getUserId() + ":solved_count";
        String processingKey = prefix + "user:" + msg.getUserId() + ":problem:" + msg.getProblemId() + ":processing";
        String lastResultKey = prefix + "user:" + msg.getUserId() + ":problem:" + msg.getProblemId() + ":last_result";

        List<String> keys = Arrays.asList(
                userStatusKey,
                solvedCountKey,
                submissionKey,
                userSolvedCountKey,
                processingKey,
                lastResultKey
        );

        startTime = System.currentTimeMillis();
        List<Object> luaResult = stringRedisTemplate.execute(
                updateResultScript,
                keys,
                msg.getSubmissionToken(),
                finalResult.getStatus(),
                String.valueOf(finalResult.getMaxRuntime()),
                String.valueOf(finalResult.getMaxMemory())
        );
        judgeMetrics.recordRedisUpdate(System.currentTimeMillis() - startTime);

        log.info("Lua update_result_v2 结果: {}, finalStatus={}", luaResult, finalResult.getStatus());

        if (luaResult == null || luaResult.size() < 2) {
            log.error("Lua脚本返回值异常: {}", luaResult);
            return;
        }

        int isFirstAC = Integer.parseInt(luaResult.get(0).toString());
        String resultStatus = luaResult.get(1).toString();

        if ("token_not_found".equals(resultStatus)) {
            log.warn("Token不存在: {}", msg.getSubmissionToken());
            return;
        }

        if ("already_processed".equals(resultStatus)) {
            log.info("Token已处理: {}", msg.getSubmissionToken());
            return;
        }

        boolean firstAc = (isFirstAC == 1 && "first_ac".equals(resultStatus));

        if (luaResult.size() >= 4) {
            int newProblemCount = Integer.parseInt(luaResult.get(2).toString());
            int newUserCount = Integer.parseInt(luaResult.get(3).toString());
            if (firstAc) {
                log.info("首次AC: userId={}, problemId={}, 题目解题数={}, 用户解题数={}",
                        msg.getUserId(), msg.getProblemId(), newProblemCount, newUserCount);
            }
        }

        sendDatabaseUpdate(msg, finalResult.getStatus(), finalResult.getPassed(), finalResult.getTotal(),
                finalResult.getMaxRuntime(), finalResult.getMaxMemory(), finalResult.getErrorInfo(), firstAc);

        judgeMetrics.recordJudgeTotal(System.currentTimeMillis() - totalStart);

        log.info("判题完成: submissionToken={}, status={}, passed={}/{}, firstAc={}",
                msg.getSubmissionToken(), finalResult.getStatus(),
                finalResult.getPassed(), finalResult.getTotal(), firstAc);
    }

    /**
     * 运行单个测试用例
     */
    private TestCaseResult runSingleTestCase(JudgeTaskMessage msg, TestCase tc, int index) {
        try {
            JSONObject result = judge0Client.submitAndWait(
                    msg.getCode(),
                    msg.getLanguageId(),
                    tc.getInputData(),
                    tc.getOutputData(),
                    msg.getTimeLimitSec(),
                    msg.getMemoryLimitKb()
            );

            JSONObject status = result.getJSONObject("status");
            int statusId = status.getIntValue("id");

            String timeStr = result.getString("time");
            String memoryStr = result.getString("memory");
            int runtime = timeStr != null ? (int) (Float.parseFloat(timeStr) * 1000) : 0;
            int memory = memoryStr != null ? (int) Float.parseFloat(memoryStr) : 0;

            TestCaseResult testCaseResult = new TestCaseResult();
            testCaseResult.setStatusId(statusId);
            testCaseResult.setRuntime(runtime);
            testCaseResult.setMemory(memory);
            testCaseResult.setTestCaseIndex(index);

            // 获取详细信息
            String stderr = judge0Client.decodeField(result, "stderr");
            String compileOutput = judge0Client.decodeField(result, "compile_output");
            String stdout = judge0Client.decodeField(result, "stdout");
            testCaseResult.setStderr(stderr);
            testCaseResult.setCompileOutput(compileOutput);
            testCaseResult.setStdout(stdout);
            testCaseResult.setInput(tc.getInputData());
            testCaseResult.setExpectedOutput(tc.getOutputData());

            return testCaseResult;

        } catch (Exception e) {
            log.error("Judge0 判题异常: {}", e.getMessage(), e);
            TestCaseResult errorResult = new TestCaseResult();
            errorResult.setStatusId(-1);  // -1 表示异常
            errorResult.setErrorInfo("判题异常: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 汇总所有测试用例结果
     */
    private TestCaseResult aggregateResults(List<TestCaseResult> results) {
        TestCaseResult finalResult = new TestCaseResult();
        finalResult.setTotal(results.size());

        int passed = 0;
        int maxRuntime = 0;
        int maxMemory = 0;
        String finalStatus = "Accepted";
        String errorInfo = null;

        // 按测试用例索引排序，保证结果顺序
        results.sort(Comparator.comparingInt(TestCaseResult::getTestCaseIndex));

        for (int i = 0; i < results.size(); i++) {
            TestCaseResult r = results.get(i);

            // 更新最大运行时间和内存
            maxRuntime = Math.max(maxRuntime, r.getRuntime());
            maxMemory = Math.max(maxMemory, r.getMemory());

            // 如果有异常或错误，直接返回
            if (r.getStatusId() == -1) {
                finalStatus = "Runtime Error";
                errorInfo = r.getErrorInfo();
                break;
            }

            // Judge0 status.id: 3=Accepted, 其他为失败
            if (r.getStatusId() == 3) {
                passed++;
            } else {
                // 第一个失败的测试用例决定最终状态
                finalStatus = judge0Client.parseStatus(r.getStatusId());

                // 构建错误信息
                StringBuilder sb = new StringBuilder();
                sb.append("测试用例 #").append(i + 1).append(" 失败\n");
                sb.append("输入: ").append(r.getInput()).append("\n");
                sb.append("期望输出: ").append(r.getExpectedOutput()).append("\n");
                if (r.getStdout() != null) sb.append("实际输出: ").append(r.getStdout().trim()).append("\n");
                if (r.getCompileOutput() != null) sb.append("编译信息: ").append(r.getCompileOutput()).append("\n");
                if (r.getStderr() != null) sb.append("错误: ").append(r.getStderr()).append("\n");
                errorInfo = sb.toString();
                break;
            }
        }

        finalResult.setStatus(finalStatus);
        finalResult.setPassed(passed);
        finalResult.setMaxRuntime(maxRuntime);
        finalResult.setMaxMemory(maxMemory);
        finalResult.setErrorInfo(errorInfo);

        return finalResult;
    }

    /**
     * 测试用例结果内部类
     */
    @Data
    private static class TestCaseResult {
        private String status;
        private int statusId;
        private int passed;
        private int total;
        private int maxRuntime;
        private int maxMemory;
        private String errorInfo;
        private int runtime;
        private int memory;
        private int testCaseIndex;
        private String input;
        private String expectedOutput;
        private String stdout;
        private String stderr;
        private String compileOutput;
    }

    /**
     * 发送数据库更新消息
     */
    private void sendDatabaseUpdate(JudgeTaskMessage msg, String status, int passed, int total,
                                    int runtimeMs, int memoryKb, String errorInfo, boolean firstAc) {
        DatabaseUpdateMessage dbMsg = DatabaseUpdateMessage.builder()
                .userId(msg.getUserId())
                .problemId(msg.getProblemId())
                .code(msg.getCode())
                .language(msg.getLanguage())
                .status(status)
                .testCasesPassed(passed)
                .testCasesTotal(total)
                .runtimeMs(runtimeMs)
                .memoryKb(memoryKb)
                .errorInfo(errorInfo)
                .submitTime(LocalDateTime.now())
                .firstAc(firstAc)
                .submissionToken(msg.getSubmissionToken())
                .contestId(msg.getContestId())
                .build();

        rocketMQTemplate.convertAndSend(MqConstant.DATABASE_UPDATE_TOPIC, dbMsg);
    }
}