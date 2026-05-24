package com.oj.judge.mq;

import com.oj.common.constant.MqConstant;
import com.oj.judge.dto.JudgeTaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 判题任务死信队列消费者：处理进入死信队列的判题任务
 * 用于记录和处理失败的判题任务
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "%DLQ%judge-task-consumer-group",
        consumerGroup = MqConstant.JUDGE_TASK_DEAD_LETTER_CONSUMER_GROUP
)
public class JudgeTaskDeadLetterConsumer implements RocketMQListener<JudgeTaskMessage> {

    @Override
    public void onMessage(JudgeTaskMessage msg) {
        log.error("死信队列收到失败的判题任务: submissionToken={}, problemId={}, userId={}",
                msg.getSubmissionToken(), msg.getProblemId(), msg.getUserId());

        log.error("死信任务详细信息: 语言={}, 代码长度={}, 时间限制={}s, 内存限制={}kb, 比赛ID={}",
                msg.getLanguage(),
                msg.getCode() != null ? msg.getCode().length() : 0,
                msg.getTimeLimitSec(),
                msg.getMemoryLimitKb(),
                msg.getContestId());
    }
}
