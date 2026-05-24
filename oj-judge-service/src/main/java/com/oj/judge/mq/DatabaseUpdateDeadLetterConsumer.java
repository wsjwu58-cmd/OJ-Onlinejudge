package com.oj.judge.mq;

import com.oj.common.constant.MqConstant;
import com.oj.judge.dto.DatabaseUpdateMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 数据库更新死信队列消费者：处理进入死信队列的数据库更新任务
 * 用于记录和处理失败的数据库更新任务
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "%DLQ%database-update-consumer-group",
        consumerGroup = MqConstant.DATABASE_UPDATE_DEAD_LETTER_CONSUMER_GROUP
)
public class DatabaseUpdateDeadLetterConsumer implements RocketMQListener<DatabaseUpdateMessage> {

    @Override
    public void onMessage(DatabaseUpdateMessage msg) {
        log.error("死信队列收到失败的数据库更新任务: userId={}, problemId={}, status={}, firstAc={}, contestId={}",
                msg.getUserId(), msg.getProblemId(), msg.getStatus(), msg.isFirstAc(), msg.getContestId());

        log.error("死信任务详细信息: 语言={}, 代码长度={}, 运行时间={}ms, 内存使用={}kb, 测试用例通过={}/{}",
                msg.getLanguage(),
                msg.getCode() != null ? msg.getCode().length() : 0,
                msg.getRuntimeMs(),
                msg.getMemoryKb(),
                msg.getTestCasesPassed(),
                msg.getTestCasesTotal());
    }
}
