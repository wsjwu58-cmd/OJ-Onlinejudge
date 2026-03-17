package com.oj.mq;

import com.alibaba.fastjson.JSONObject;
import com.oj.constant.MqConstant;
import com.oj.constant.MessageConstant;
import com.oj.dto.DatabaseUpdateMessage;
import com.oj.entity.ContestProblem;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.entity.User;
import com.oj.exception.BaseException;
import com.oj.mapper.ContestProblemMapper;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubMissionMapper;
import com.oj.mapper.UserMapper;
import com.oj.service.UserContestService;
import com.oj.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库消费者：从 MQ 消费判题结果
 * 1. 保存提交记录到 MySQL（含 contestId）
 * 2. 如果首次 AC，更新题目通过率
 * 3. 如果是比赛提交且 AC，更新 Redis 排行榜
 * 4. 通过 WebSocket 通知前端
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
    private SubMissionMapper subMissionMapper;

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private UserContestService userContestService;

    @Autowired
    private ContestProblemMapper contestProblemMapper;

    @Override
    public void onMessage(DatabaseUpdateMessage msg) {
        log.info("收到数据库更新消息: userId={}, problemId={}, status={}, firstAc={}, contestId={}",
                msg.getUserId(), msg.getProblemId(), msg.getStatus(), msg.isFirstAc(), msg.getContestId());

        try {
            // 1. 保存提交记录（含 contestId）
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

            log.info("提交记录已保存: submissionId={}", submission.getId());

            // 2. 如果首次 AC（普通练习维度），更新用户解题数
            if (msg.isFirstAc()) {
                updateUserSolvedCount(msg.getUserId());
            }

            // 3. 更新题目通过率
            updateAcceptance(msg.getProblemId());

            // 4. ★ 如果是比赛提交且 Accepted，更新 Redis 排行榜
            if (msg.getContestId() != null && "Accepted".equals(msg.getStatus())) {
                int problemScore = getProblemScoreInContest(msg.getContestId(), msg.getProblemId());
                userContestService.updateRankOnAccepted(
                        msg.getContestId(), msg.getUserId(), msg.getProblemId(), problemScore);
            }

            // 5. WebSocket 通知前端（推送判题结果）
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
            log.info("WebSocket 通知已发送: submissionId={}, status={}", submission.getId(), msg.getStatus());

        } catch (Exception e) {
            log.error("数据库更新失败: {}", e.getMessage(), e);
            throw new BaseException(MessageConstant.DATABASE_ERROR + ": " + e.getMessage());
        }
    }

    /**
     * 获取某题在比赛中的分值
     */
    private int getProblemScoreInContest(Integer contestId, Integer problemId) {
        LambdaQueryWrapper<ContestProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, problemId);
        ContestProblem cp = contestProblemMapper.selectOne(wrapper);
        return cp != null && cp.getScore() != null ? cp.getScore() : 100;
    }

    /**
     * 更新题目通过率
     */
    private void updateAcceptance(Integer problemId) {
        try {
            Integer totalCount = subMissionMapper.selectCountProblem(problemId, null);
            Integer acceptedCount = subMissionMapper.selectCountProblem(problemId, "Accepted");

            if (totalCount != null && totalCount > 0) {
                double percent = (acceptedCount.doubleValue() / totalCount) * 100;
                BigDecimal acceptance = BigDecimal.valueOf(percent).setScale(1, RoundingMode.HALF_UP);

                Problem problem = problemMapper.selectById(problemId);
                if (problem != null) {
                    problem.setAcceptance(acceptance);
                    problemMapper.updateById(problem);
                    log.info("题目 {} 通过率已更新: {}%", problemId, acceptance);
                }
            }
        } catch (Exception e) {
            log.error("更新通过率失败: problemId={}, error={}", problemId, e.getMessage());
        }
    }

    /**
     * 更新用户解题数（使用 points 字段）
     */
    private void updateUserSolvedCount(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + 1);
                userMapper.updateById(user);
                log.info("用户 {} 解题数(points)已更新: {}", userId, user.getPoints());
            }
        } catch (Exception e) {
            log.error("更新用户解题数失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
