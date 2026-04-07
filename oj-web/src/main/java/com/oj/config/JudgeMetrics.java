package com.oj.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class JudgeMetrics {

    private final MeterRegistry meterRegistry;
    private Timer judgeTotalTimer;
    private Timer judgeSubmitTimer;
    private Timer problemQueryTimer;
    private Timer testCaseQueryTimer;
    private Timer redisLimitTimer;
    private Timer mqSendTimer;
    private Timer judge0CallTimer;
    private Timer redisUpdateTimer;
    private Timer databaseUpdateTimer;
    private Timer websocketPushTimer;

    @Autowired
    public JudgeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.judgeTotalTimer = Timer.builder("oj.judge.total")
                .tag("phase", "total")
                .description("Total judge time")
                .register(meterRegistry);

        this.judgeSubmitTimer = Timer.builder("oj.judge.submit")
                .tag("phase", "submit")
                .description("Judge submit time")
                .register(meterRegistry);

        this.problemQueryTimer = Timer.builder("oj.judge.database")
                .tag("operation", "select_problem")
                .description("Problem query time")
                .register(meterRegistry);

        this.testCaseQueryTimer = Timer.builder("oj.judge.database")
                .tag("operation", "select_testcase")
                .description("Test case query time")
                .register(meterRegistry);

        this.redisLimitTimer = Timer.builder("oj.judge.redis")
                .tag("operation", "rate_limit")
                .description("Redis rate limit time")
                .register(meterRegistry);

        this.mqSendTimer = Timer.builder("oj.judge.mq")
                .tag("operation", "send_message")
                .description("MQ send time")
                .register(meterRegistry);

        this.judge0CallTimer = Timer.builder("oj.judge.judge0")
                .tag("operation", "judge0_call")
                .description("Judge0 API call time")
                .register(meterRegistry);

        this.redisUpdateTimer = Timer.builder("oj.judge.redis")
                .tag("operation", "update_result")
                .description("Redis update result time")
                .register(meterRegistry);

        this.databaseUpdateTimer = Timer.builder("oj.judge.database")
                .tag("operation", "update_submission")
                .description("Database update time")
                .register(meterRegistry);

        this.websocketPushTimer = Timer.builder("oj.judge.websocket")
                .tag("operation", "push_result")
                .description("WebSocket push time")
                .register(meterRegistry);
    }

    public void recordJudgeTotal(long durationMs) {
        judgeTotalTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordJudgeSubmit(long durationMs) {
        judgeSubmitTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordProblemQuery(long durationMs) {
        problemQueryTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordTestCaseQuery(long durationMs) {
        testCaseQueryTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordRedisLimit(long durationMs) {
        redisLimitTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordMqSend(long durationMs) {
        mqSendTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordJudge0Call(long durationMs) {
        judge0CallTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordRedisUpdate(long durationMs) {
        redisUpdateTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordDatabaseUpdate(long durationMs) {
        databaseUpdateTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordWebsocketPush(long durationMs) {
        websocketPushTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
