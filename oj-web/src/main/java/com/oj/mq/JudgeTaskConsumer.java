package com.oj.mq;

import com.alibaba.fastjson.JSONObject;
import com.oj.config.Judge0Client;
import com.oj.constant.MqConstant;
import com.oj.dto.DatabaseUpdateMessage;
import com.oj.dto.JudgeTaskMessage;
import com.oj.entity.TestCase;
import com.oj.mapper.TestCaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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

    @Override
    public void onMessage(JudgeTaskMessage msg) {
        log.info("收到判题任务: submissionToken={}, problemId={}, userId={}",
                msg.getSubmissionToken(), msg.getProblemId(), msg.getUserId());

        // 1. 获取测试用例
        List<TestCase> testCases = testCaseMapper.selectByProblemId(msg.getProblemId());
        if (testCases == null || testCases.isEmpty()) {
            log.error("题目 {} 没有测试用例，跳过", msg.getProblemId());
            sendDatabaseUpdate(msg, "Runtime Error", 0, 0, 0, 0, "该题目暂无测试用例", false);

            return;
        }

        int passed = 0;
        int total = testCases.size();
        String finalStatus = "Accepted";
        String errorInfo = null;
        int maxRuntime = 0;
        int maxMemory = 0;

        // 2. 逐个测试用例提交 Judge0 并轮询
        for (int i = 0; i < total; i++) {
            TestCase tc = testCases.get(i);
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
                maxRuntime = Math.max(maxRuntime, runtime);
                maxMemory = Math.max(maxMemory, memory);

                if (statusId == 3) {
                    passed++;
                } else {
                    finalStatus = judge0Client.parseStatus(statusId);
                    String stderr = judge0Client.decodeField(result, "stderr");
                    String compileOutput = judge0Client.decodeField(result, "compile_output");
                    String stdout = judge0Client.decodeField(result, "stdout");
                    StringBuilder sb = new StringBuilder();
                    sb.append("测试用例 #").append(i + 1).append(" 失败\n");
                    sb.append("输入: ").append(tc.getInputData()).append("\n");
                    sb.append("期望输出: ").append(tc.getOutputData()).append("\n");
                    if (stdout != null) sb.append("实际输出: ").append(stdout.trim()).append("\n");
                    if (compileOutput != null) sb.append("编译信息: ").append(compileOutput).append("\n");
                    if (stderr != null) sb.append("错误: ").append(stderr).append("\n");
                    errorInfo = sb.toString();
                    break;
                }

            } catch (Exception e) {
                log.error("Judge0 判题异常, 测试用例#{}: {}", i + 1, e.getMessage());
                finalStatus = "Runtime Error";
                errorInfo = "测试用例 #" + (i + 1) + " 判题异常: " + e.getMessage();
                break;
            }
        }

        // 3. 执行 Lua 脚本：update_result_v2（原子性更新 Redis）
        // 比赛模式下 key 加 contest 前缀，与普通练习隔离
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

        List<Object> luaResult = stringRedisTemplate.execute(
                updateResultScript,
                keys,
                msg.getSubmissionToken(),
                finalStatus,
                String.valueOf(maxRuntime),
                String.valueOf(maxMemory)
        );

        log.info("Lua update_result_v2 结果: {}, finalStatus={}", luaResult, finalStatus);

        // 4. 解析 Lua 返回值
        if (luaResult == null || luaResult.size() < 2) {
            log.error("Lua脚本返回值异常: {}", luaResult);
            return;
        }

        int isFirstAC = Integer.parseInt(luaResult.get(0).toString());
        String resultStatus = luaResult.get(1).toString();

        // token不存在
        if ("token_not_found".equals(resultStatus)) {
            log.warn("Token不存在: {}", msg.getSubmissionToken());
            return;
        }

        // token已处理过
        if ("already_processed".equals(resultStatus)) {
            log.info("Token已处理: {}", msg.getSubmissionToken());
            return;
        }

        // 判断是否首次AC
        boolean firstAc = (isFirstAC == 1 && "first_ac".equals(resultStatus));

        // 记录统计数据（用于日志）
        if (luaResult.size() >= 4) {
            int newProblemCount = Integer.parseInt(luaResult.get(2).toString());
            int newUserCount = Integer.parseInt(luaResult.get(3).toString());
            if (firstAc) {
                log.info("首次AC: userId={}, problemId={}, 题目解题数={}, 用户解题数={}",
                        msg.getUserId(), msg.getProblemId(), newProblemCount, newUserCount);
            }
        }

        // 5. 发送到数据库更新队列
        sendDatabaseUpdate(msg, finalStatus, passed, total, maxRuntime, maxMemory, errorInfo, firstAc);

        log.info("判题完成: submissionToken={}, status={}, passed={}/{}, firstAc={}",
                msg.getSubmissionToken(), finalStatus, passed, total, firstAc);
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