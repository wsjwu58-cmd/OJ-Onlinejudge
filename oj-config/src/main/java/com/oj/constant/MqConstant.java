package com.oj.constant;

/**
 * RocketMQ Topic / Tag 常量
 */
public class MqConstant {
    /** 判题任务 Topic */
    public static final String JUDGE_TASK_TOPIC = "judge-task-topic";

    /** 数据库更新 Topic */
    public static final String DATABASE_UPDATE_TOPIC = "database-update-topic";

    /** 判题任务死信 Topic */
    public static final String JUDGE_TASK_DEAD_LETTER_TOPIC = "judge-task-dead-letter-topic";

    /** 数据库更新死信 Topic */
    public static final String DATABASE_UPDATE_DEAD_LETTER_TOPIC = "database-update-dead-letter-topic";

    /** 判题任务消费者组 */
    public static final String JUDGE_TASK_CONSUMER_GROUP = "judge-task-consumer-group";

    /** 判题任务死信消费者组 */
    public static final String JUDGE_TASK_DEAD_LETTER_CONSUMER_GROUP = "judge-task-dead-letter-consumer-group";

    /** 数据库更新消费者组 */
    public static final String DATABASE_UPDATE_CONSUMER_GROUP = "database-update-consumer-group";

    /** 数据库更新死信消费者组 */
    public static final String DATABASE_UPDATE_DEAD_LETTER_CONSUMER_GROUP = "database-update-dead-letter-consumer-group";
}
