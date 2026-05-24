package com.oj.judge.service.impl;

import com.oj.api.ProblemClient;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.api.dto.TestCaseFeignDTO;
import com.oj.common.constant.MqConstant;
import com.oj.common.constant.MessageConstant;
import com.oj.common.context.BaseContext;
import com.oj.common.exception.BaseException;
import com.oj.common.result.Result;
import com.oj.judge.config.Judge0Client;
import com.oj.judge.dto.*;
import com.oj.judge.service.EnhancedMaliciousCodeDetector;
import com.oj.judge.service.JudgeService;
import com.oj.judge.vo.JudgeResultVO;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
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
    private ProblemClient problemClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DefaultRedisScript<List> submitAndUpdateScript;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private EnhancedMaliciousCodeDetector maliciousCodeDetector;

    @Override
    public JudgeResultVO submit(JudgeSubmitDTO dto, Long userId) {
        // 通过Feign获取题目信息
        Result<ProblemFeignDTO> problemResult = problemClient.getProblemById(dto.getProblemId());
        if (problemResult == null || problemResult.getData() == null) {
            throw new BaseException("题目不存在: " + dto.getProblemId());
        }
        ProblemFeignDTO problem = problemResult.getData();

        // 通过Feign获取测试用例
        Result<List<TestCaseFeignDTO>> tcResult = problemClient.getTestCasesByProblemId(dto.getProblemId());
        if (tcResult == null || tcResult.getData() == null || tcResult.getData().isEmpty()) {
            throw new BaseException(MessageConstant.TESTCASE_NOT_FOUND);
        }
        int testCasesTotal = tcResult.getData().size();

        // 恶意代码检测
        MaliciousCodeDetectionResult maliciousResult = maliciousCodeDetector.detect(dto.getCode(), dto.getLanguage());
        if (!maliciousResult.isSafe()) {
            return JudgeResultVO.builder()
                    .status("Malicious Code").errorInfo("【安全检测】\n" + maliciousResult.getMessage())
                    .testCasesPassed(0).testCasesTotal(0).problemId(dto.getProblemId()).build();
        }

        String submissionToken = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis() / 1000;

        String prefix = dto.getContestId() != null ? "contest:" + dto.getContestId() + ":" : "";
        List<String> keys = Arrays.asList(
                prefix + "user:" + userId + ":problem:" + dto.getProblemId() + ":status",
                prefix + "problem:" + dto.getProblemId() + ":solved_count",
                "submission:" + submissionToken,
                prefix + "user:" + userId + ":problem:" + dto.getProblemId() + ":processing",
                prefix + "user:" + userId + ":problem:" + dto.getProblemId() + ":submit_window"
        );

        List<Object> luaResult = stringRedisTemplate.execute(submitAndUpdateScript, keys,
                submissionToken, String.valueOf(currentTime), "60", "5");

        if (luaResult == null || luaResult.size() < 2) {
            throw new BaseException(MessageConstant.LUA_EXECUTION_ERROR);
        }

        int success = Integer.parseInt(luaResult.get(0).toString());
        String status = luaResult.get(1).toString();

        if (success == 0 && "rate_limited".equals(status)) {
            int waitTime = luaResult.size() >= 5 ? Integer.parseInt(luaResult.get(4).toString()) : 0;
            return JudgeResultVO.builder().status("Rate Limited")
                    .errorInfo("提交过于频繁，请" + waitTime + "秒后再试")
                    .testCasesPassed(0).testCasesTotal(0).problemId(dto.getProblemId()).build();
        }

        if (success == 0 && "already_processing".equals(status)) {
            String existingToken = luaResult.get(2) != null ? luaResult.get(2).toString() : "";
            return JudgeResultVO.builder().status("Processing")
                    .errorInfo("有提交正在判题中").testCasesPassed(0).testCasesTotal(0)
                    .problemId(dto.getProblemId()).title(problem.getTitle()).submitToken(existingToken).build();
        }

        int languageId = judge0Client.getLanguageId(dto.getLanguage());
        Float timeLimit = problem.getTimeLimitMs() != null ? problem.getTimeLimitMs() / 1000.0f : null;
        Integer memoryLimit = problem.getMemoryLimitMb() != null ? problem.getMemoryLimitMb() * 1024 : null;

        JudgeTaskMessage taskMessage = JudgeTaskMessage.builder()
                .userId(userId).problemId(dto.getProblemId()).code(dto.getCode())
                .language(dto.getLanguage()).submissionToken(submissionToken)
                .languageId(languageId).testCasesTotal(testCasesTotal)
                .timeLimitSec(timeLimit).memoryLimitKb(memoryLimit)
                .contestId(dto.getContestId()).build();

        rocketMQTemplate.convertAndSend(MqConstant.JUDGE_TASK_TOPIC, taskMessage);

        return JudgeResultVO.builder().status("Pending").testCasesPassed(0).testCasesTotal(testCasesTotal)
                .problemId(dto.getProblemId()).title(problem.getTitle()).submitToken(submissionToken)
                .errorInfo("代码已提交，正在排队判题中...").submitTime(LocalDateTime.now()).build();
    }

    @Override
    public JudgeResultVO run(JudgeRunDTO dto) {
        int languageId = judge0Client.getLanguageId(dto.getLanguage());
        String stdin = dto.getCustomInput();

        if (stdin == null || stdin.isEmpty()) {
            Result<List<TestCaseFeignDTO>> tcResult = problemClient.getTestCasesByProblemId(dto.getProblemId());
            if (tcResult != null && tcResult.getData() != null && !tcResult.getData().isEmpty()) {
                TestCaseFeignDTO sample = tcResult.getData().stream()
                        .filter(tc -> Boolean.TRUE.equals(tc.getIsSample()))
                        .findFirst().orElse(tcResult.getData().get(0));
                stdin = sample.getInputData();
            }
        }

        Result<ProblemFeignDTO> problemResult = problemClient.getProblemById(dto.getProblemId());
        Float timeLimit = null;
        Integer memoryLimit = null;
        if (problemResult != null && problemResult.getData() != null) {
            ProblemFeignDTO p = problemResult.getData();
            timeLimit = p.getTimeLimitMs() != null ? p.getTimeLimitMs() / 1000.0f : null;
            memoryLimit = p.getMemoryLimitMb() != null ? p.getMemoryLimitMb() * 1024 : null;
        }

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
            String errorMsg = compileOutput != null ? compileOutput : stderr;
            return JudgeResultVO.builder().status(judge0Client.parseStatus(statusId))
                    .runtimeMs(runtime).memoryKb(memory).stdout(stdout != null ? stdout.trim() : "")
                    .errorInfo(errorMsg).build();
        } catch (Exception e) {
            return JudgeResultVO.builder().status("Runtime Error")
                    .errorInfo(MessageConstant.JUDGE0_ERROR + ": " + e.getMessage()).build();
        }
    }
}
