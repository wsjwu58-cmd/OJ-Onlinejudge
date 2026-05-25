package com.oj.contest.mq;

import com.oj.api.ProblemClient;
import com.oj.common.constant.MqConstant;
import com.oj.common.mq.dto.HackResultMessage;
import com.oj.contest.entity.HackRecord;
import com.oj.contest.mapper.HackRecordMapper;
import com.oj.contest.service.UserContestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.HACK_RESULT_TOPIC,
        consumerGroup = MqConstant.HACK_RESULT_CONSUMER_GROUP,
        maxReconsumeTimes = 3
)
public class HackResultConsumer implements RocketMQListener<HackResultMessage> {

    @Autowired
    private HackRecordMapper hackRecordMapper;

    @Autowired
    private UserContestService userContestService;

    @Autowired
    private ProblemClient problemClient;

    @Override
    public void onMessage(HackResultMessage msg) {
        log.info("收到 Hack 结果: hackId={}, status={}", msg.getHackId(), msg.getStatus());

        HackRecord record = HackRecord.builder()
                .id(msg.getHackId())
                .contestId(msg.getContestId())
                .problemId(msg.getProblemId())
                .hackerId(msg.getHackerId())
                .targetUserId(msg.getTargetUserId())
                .targetSubmissionId(msg.getTargetSubmissionId())
                .hackInput(msg.getHackInput())
                .hackOutput(msg.getHackOutput())
                .status(msg.getStatus())
                .errorInfo(msg.getErrorInfo())
                .targetResult(msg.getTargetResult())
                .updatedAt(LocalDateTime.now())
                .build();

        hackRecordMapper.updateById(record);

        if ("HackSuccess".equals(msg.getStatus())) {
            int problemScore = userContestService.getProblemScoreInContest(
                    msg.getContestId(), msg.getProblemId());
            userContestService.updateRankOnHackSuccess(
                    msg.getContestId(), msg.getHackerId(),
                    msg.getTargetUserId(), msg.getProblemId(), problemScore);

            try {
                problemClient.addHackTestCase(msg.getProblemId(), msg.getHackId(),
                        msg.getHackInput(), msg.getHackOutput());
            } catch (Exception e) {
                log.error("添加 Hack 测试用例失败: {}", e.getMessage());
            }
        }

        log.info("Hack 结果处理完成: hackId={}, status={}", msg.getHackId(), msg.getStatus());
    }
}
