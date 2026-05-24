package com.oj.judge.mq;

import com.alibaba.fastjson.JSONObject;
import com.oj.api.ContestClient;
import com.oj.api.ProblemClient;
import com.oj.api.UserClient;
import com.oj.api.dto.TestCaseFeignDTO;
import com.oj.common.constant.MqConstant;
import com.oj.common.context.BaseContext;
import com.oj.common.result.Result;
import com.oj.judge.config.Judge0Client;
import com.oj.judge.dto.DatabaseUpdateMessage;
import com.oj.judge.dto.JudgeTaskMessage;
import com.oj.judge.websocket.WebSocketServer;
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
import java.util.stream.Collectors;

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
    private ProblemClient problemClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DefaultRedisScript<List> updateResultScript;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private Executor judgeExecutor;

    @Override
    public void onMessage(JudgeTaskMessage msg) {
        log.info("收到判题任务: token={}, problemId={}, userId={}",
                msg.getSubmissionToken(), msg.getProblemId(), msg.getUserId());

        // 通过Feign获取测试用例
        Result<List<TestCaseFeignDTO>> tcResult = problemClient.getTestCasesByProblemId(msg.getProblemId());
        if (tcResult == null || tcResult.getData() == null || tcResult.getData().isEmpty()) {
            sendDatabaseUpdate(msg, "Runtime Error", 0, 0, 0, 0, "该题目暂无测试用例", false);
            return;
        }

        List<TestCaseFeignDTO> testCases = tcResult.getData();
        List<CompletableFuture<TestCaseResult>> futures = new ArrayList<>();
        for (int i = 0; i < testCases.size(); i++) {
            final int index = i;
            TestCaseFeignDTO tc = testCases.get(i);
            futures.add(CompletableFuture.supplyAsync(() -> runSingleTestCase(msg, tc, index), judgeExecutor));
        }

        TestCaseResult finalResult;
        try {
            int timeoutSec = msg.getTimeLimitSec() != null ? msg.getTimeLimitSec().intValue() : 10;
            int totalTimeout = timeoutSec * testCases.size() + 30;
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(totalTimeout, java.util.concurrent.TimeUnit.SECONDS);
            List<TestCaseResult> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
            finalResult = aggregateResults(results);
        } catch (java.util.concurrent.TimeoutException e) {
            finalResult = new TestCaseResult();
            finalResult.setStatus("Time Limit Exceeded");
            finalResult.setErrorInfo("判题超时");
            finalResult.setPassed(0);
            finalResult.setTotal(testCases.size());
        } catch (Exception e) {
            finalResult = new TestCaseResult();
            finalResult.setStatus("Runtime Error");
            finalResult.setErrorInfo("判题异常: " + e.getMessage());
            finalResult.setPassed(0);
            finalResult.setTotal(testCases.size());
        }

        // Lua脚本更新Redis
        String prefix = msg.getContestId() != null ? "contest:" + msg.getContestId() + ":" : "";
        List<String> keys = Arrays.asList(
                prefix + "user:" + msg.getUserId() + ":problem:" + msg.getProblemId() + ":status",
                prefix + "problem:" + msg.getProblemId() + ":solved_count",
                "submission:" + msg.getSubmissionToken(),
                prefix + "user:" + msg.getUserId() + ":solved_count",
                prefix + "user:" + msg.getUserId() + ":problem:" + msg.getProblemId() + ":processing",
                prefix + "user:" + msg.getUserId() + ":problem:" + msg.getProblemId() + ":last_result"
        );

        List<Object> luaResult = stringRedisTemplate.execute(updateResultScript, keys,
                msg.getSubmissionToken(), finalResult.getStatus(),
                String.valueOf(finalResult.getMaxRuntime()), String.valueOf(finalResult.getMaxMemory()));

        if (luaResult == null || luaResult.size() < 2) return;

        int isFirstAC = Integer.parseInt(luaResult.get(0).toString());
        String resultStatus = luaResult.get(1).toString();
        if ("token_not_found".equals(resultStatus) || "already_processed".equals(resultStatus)) return;

        boolean firstAc = (isFirstAC == 1 && "first_ac".equals(resultStatus));

        sendDatabaseUpdate(msg, finalResult.getStatus(), finalResult.getPassed(), finalResult.getTotal(),
                finalResult.getMaxRuntime(), finalResult.getMaxMemory(), finalResult.getErrorInfo(), firstAc);

        log.info("判题完成: token={}, status={}, passed={}/{}, firstAc={}",
                msg.getSubmissionToken(), finalResult.getStatus(),
                finalResult.getPassed(), finalResult.getTotal(), firstAc);
    }

    private TestCaseResult runSingleTestCase(JudgeTaskMessage msg, TestCaseFeignDTO tc, int index) {
        try {
            JSONObject result = judge0Client.submitAndWait(msg.getCode(), msg.getLanguageId(),
                    tc.getInputData(), tc.getOutputData(), msg.getTimeLimitSec(), msg.getMemoryLimitKb());
            JSONObject status = result.getJSONObject("status");
            int statusId = status.getIntValue("id");
            String timeStr = result.getString("time");
            String memoryStr = result.getString("memory");
            int runtime = timeStr != null ? (int) (Float.parseFloat(timeStr) * 1000) : 0;
            int memory = memoryStr != null ? (int) Float.parseFloat(memoryStr) : 0;

            TestCaseResult r = new TestCaseResult();
            r.setStatusId(statusId);
            r.setRuntime(runtime);
            r.setMemory(memory);
            r.setTestCaseIndex(index);
            r.setStderr(judge0Client.decodeField(result, "stderr"));
            r.setCompileOutput(judge0Client.decodeField(result, "compile_output"));
            r.setStdout(judge0Client.decodeField(result, "stdout"));
            r.setInput(tc.getInputData());
            r.setExpectedOutput(tc.getOutputData());
            return r;
        } catch (Exception e) {
            TestCaseResult r = new TestCaseResult();
            r.setStatusId(-1);
            r.setErrorInfo("判题异常: " + e.getMessage());
            return r;
        }
    }

    private TestCaseResult aggregateResults(List<TestCaseResult> results) {
        TestCaseResult finalResult = new TestCaseResult();
        finalResult.setTotal(results.size());
        int passed = 0, maxRuntime = 0, maxMemory = 0;
        String finalStatus = "Accepted", errorInfo = null;
        results.sort(Comparator.comparingInt(TestCaseResult::getTestCaseIndex));

        for (int i = 0; i < results.size(); i++) {
            TestCaseResult r = results.get(i);
            maxRuntime = Math.max(maxRuntime, r.getRuntime());
            maxMemory = Math.max(maxMemory, r.getMemory());
            if (r.getStatusId() == -1) { finalStatus = "Runtime Error"; errorInfo = r.getErrorInfo(); break; }
            if (r.getStatusId() == 3) { passed++; }
            else {
                finalStatus = judge0Client.parseStatus(r.getStatusId());
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

    private void sendDatabaseUpdate(JudgeTaskMessage msg, String status, int passed, int total,
                                    int runtimeMs, int memoryKb, String errorInfo, boolean firstAc) {
        DatabaseUpdateMessage dbMsg = DatabaseUpdateMessage.builder()
                .userId(msg.getUserId()).problemId(msg.getProblemId()).code(msg.getCode())
                .language(msg.getLanguage()).status(status).testCasesPassed(passed).testCasesTotal(total)
                .runtimeMs(runtimeMs).memoryKb(memoryKb).errorInfo(errorInfo)
                .submitTime(LocalDateTime.now()).firstAc(firstAc)
                .submissionToken(msg.getSubmissionToken()).contestId(msg.getContestId()).build();
        rocketMQTemplate.convertAndSend(MqConstant.DATABASE_UPDATE_TOPIC, dbMsg);
    }

    @Data
    private static class TestCaseResult {
        private String status;
        private int statusId, passed, total, maxRuntime, maxMemory, runtime, memory, testCaseIndex;
        private String errorInfo, input, expectedOutput, stdout, stderr, compileOutput;
    }
}
