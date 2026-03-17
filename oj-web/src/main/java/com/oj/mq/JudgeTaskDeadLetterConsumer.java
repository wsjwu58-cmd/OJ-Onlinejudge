package com.oj.mq;

import com.oj.constant.MqConstant;
import com.oj.dto.JudgeTaskMessage;
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
        
        // 这里可以添加处理逻辑，例如：
        // 1. 记录到数据库的失败任务表
        // 2. 发送告警通知
        // 3. 人工干预处理
        
        // 示例：记录详细信息
        log.error("死信任务详细信息: 语言={}, 代码长度={}, 时间限制={}s, 内存限制={}kb, 比赛ID={}",
                msg.getLanguage(),
                msg.getCode() != null ? msg.getCode().length() : 0,
                msg.getTimeLimitSec(),
                msg.getMemoryLimitKb(),
                msg.getContestId());
    }
}
