package com.oj.judge.mq;

import com.oj.common.constant.MqConstant;
import com.oj.common.mq.dto.HackResultMessage;
import com.oj.common.mq.dto.HackTaskMessage;
import com.oj.judge.entity.Submission;
import com.oj.judge.mapper.SubmissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "%DLQ%" + MqConstant.HACK_TASK_CONSUMER_GROUP,
        consumerGroup = MqConstant.HACK_TASK_CONSUMER_GROUP + "-dlq"
)
public class HackTaskDeadLetterConsumer implements RocketMQListener<HackTaskMessage> {

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(HackTaskMessage msg) {
        log.error("Hack任务进入死信队列: hackId={}, hacker={}, target={}, problem={}",
                msg.getHackId(), msg.getHackerId(), msg.getTargetUserId(), msg.getProblemId());

        HackResultMessage result = HackResultMessage.builder()
                .hackId(msg.getHackId())
                .contestId(msg.getContestId())
                .problemId(msg.getProblemId())
                .hackerId(msg.getHackerId())
                .targetUserId(msg.getTargetUserId())
                .targetSubmissionId(msg.getTargetSubmissionId())
                .status("SystemError")
                .hackInput(msg.getHackInput())
                .errorInfo("Hack判题系统异常，重试耗尽")
                .build();
        rocketMQTemplate.convertAndSend(MqConstant.HACK_RESULT_TOPIC, result);
        log.info("Hack SystemError 结果已发送: hackId={}", msg.getHackId());
    }
}
