package com.oj.judge.mq;

import com.alibaba.fastjson.JSONObject;
import com.oj.api.ContestClient;
import com.oj.api.ProblemClient;
import com.oj.api.UserClient;
import com.oj.api.dto.ContestProblemFeignDTO;
import com.oj.common.constant.MqConstant;
import com.oj.common.constant.MessageConstant;
import com.oj.common.exception.BaseException;
import com.oj.common.result.Result;
import com.oj.judge.config.JudgeMetrics;
import com.oj.judge.dto.DatabaseUpdateMessage;
import com.oj.judge.entity.Submission;
import com.oj.judge.mapper.SubmissionMapper;
import com.oj.judge.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库消费者：从 MQ 消费判题结果
 * 1. 保存提交记录到 MySQL（含 contestId）
 * 2. 如果首次 AC，更新用户解题数（通过 UserClient Feign 调用）
 * 3. 更新题目通过率（通过 ProblemClient Feign 调用）
 * 4. 如果是比赛提交且 AC，更新 Redis 排行榜（通过 ContestClient Feign 调用）
 * 5. 通过 WebSocket 通知前端
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.DATABASE_UPDATE_TOPIC,
        consumerGroup = MqConstant.DATABASE_UPDATE_CONSUMER_GROUP,
        maxReconsumeTimes = 3
)
public class DatabaseUpdateConsumer implements RocketMQListener<DatabaseUpdateMessage> {

    @Autowired
    private SubmissionMapper subMissionMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private ProblemClient problemClient;

    @Autowired
    private ContestClient contestClient;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private JudgeMetrics judgeMetrics;

    @Override
    public void onMessage(DatabaseUpdateMessage msg) {
        log.info("收到数据库更新消息: userId={}, problemId={}, status={}, firstAc={}, contestId={}",
                msg.getUserId(), msg.getProblemId(), msg.getStatus(), msg.isFirstAc(), msg.getContestId());

        try {
            long startTime = System.currentTimeMillis();
            Submission submission = new Submission();
            submission.setUserId(msg.getUserId());
            submission.setProblemId(msg.getProblemId());
            submission.setContestId(msg.getContestId());
            submission.setCode(msg.getCode());
            submission.setLanguage(msg.getLanguage());
            submission.setStatus(msg.getStatus());
            submission.setRuntimeMs(msg.getRuntimeMs());
            submission.setMemoryKb(msg.getMemoryKb());
            submission.setTestCasesPassed(msg.getTestCasesPassed());
            submission.setTestCasesTotal(msg.getTestCasesTotal());
            submission.setErrorInfo(msg.getErrorInfo());
            submission.setSubmitTime(msg.getSubmitTime());
            subMissionMapper.insert(submission);
            judgeMetrics.recordDatabaseUpdate(System.currentTimeMillis() - startTime);

            log.info("提交记录已保存: submissionId={}", submission.getId());

            if (msg.isFirstAc()) {
                updateUserSolvedCount(msg.getUserId());
            }

            updateAcceptance(msg.getProblemId());

            if (msg.getContestId() != null && "Accepted".equals(msg.getStatus())) {
                int problemScore = getProblemScoreInContest(msg.getContestId(), msg.getProblemId());
                Result<Void> rankResult = contestClient.updateRankOnAccepted(
                        msg.getContestId(), msg.getUserId(), msg.getProblemId(), problemScore);
                if (rankResult == null || rankResult.getCode() != 1) {
                    log.warn("更新比赛排名失败: contestId={}, userId={}, problemId={}",
                            msg.getContestId(), msg.getUserId(), msg.getProblemId());
                }
            }

            startTime = System.currentTimeMillis();
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "judge_result");
            wsMessage.put("userId", msg.getUserId());
            wsMessage.put("problemId", msg.getProblemId());
            wsMessage.put("contestId", msg.getContestId());
            wsMessage.put("submissionId", submission.getId());
            wsMessage.put("status", msg.getStatus());
            wsMessage.put("testCasesPassed", msg.getTestCasesPassed());
            wsMessage.put("testCasesTotal", msg.getTestCasesTotal());
            wsMessage.put("runtimeMs", msg.getRuntimeMs());
            wsMessage.put("memoryKb", msg.getMemoryKb());
            wsMessage.put("errorInfo", msg.getErrorInfo());
            wsMessage.put("content", "判题完成 - " + msg.getStatus());

            webSocketServer.sendToAllClient(JSONObject.toJSONString(wsMessage));
            judgeMetrics.recordWebsocketPush(System.currentTimeMillis() - startTime);

            log.info("WebSocket 通知已发送: submissionId={}, status={}", submission.getId(), msg.getStatus());

        } catch (Exception e) {
            log.error("数据库更新失败: {}", e.getMessage(), e);
            throw new BaseException(MessageConstant.DATABASE_ERROR + ": " + e.getMessage());
        }
    }

    /**
     * 获取某题在比赛中的分值（通过 ContestClient Feign 调用）
     */
    private int getProblemScoreInContest(Integer contestId, Integer problemId) {
        try {
            Result<ContestProblemFeignDTO> result = contestClient.getContestProblem(contestId, problemId);
            if (result != null && result.getCode() == 1 && result.getData() != null) {
                ContestProblemFeignDTO cp = result.getData();
                return cp.getScore() != null ? cp.getScore() : 100;
            }
        } catch (Exception e) {
            log.error("获取比赛题目分值失败: contestId={}, problemId={}, error={}", contestId, problemId, e.getMessage());
        }
        return 100;
    }

    /**
     * 更新题目通过率（通过 ProblemClient Feign 调用）
     */
    private void updateAcceptance(Integer problemId) {
        try {
            Result<Void> result = problemClient.updateProblemAcceptance(problemId);
            if (result != null && result.getCode() == 1) {
                log.info("题目 {} 通过率已更新", problemId);
            } else {
                log.warn("更新题目通过率可能失败: problemId={}", problemId);
            }
        } catch (Exception e) {
            log.error("更新通过率失败: problemId={}, error={}", problemId, e.getMessage());
        }
    }

    /**
     * 更新用户解题数（通过 UserClient Feign 调用）
     */
    private void updateUserSolvedCount(Long userId) {
        try {
            Result<Void> result = userClient.updateUserSolvedCount(userId);
            if (result != null && result.getCode() == 1) {
                log.info("用户 {} 解题数已更新", userId);
            } else {
                log.warn("更新用户解题数可能失败: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("更新用户解题数失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
