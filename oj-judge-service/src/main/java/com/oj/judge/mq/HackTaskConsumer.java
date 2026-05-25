package com.oj.judge.mq;

import com.oj.common.constant.MqConstant;
import com.oj.common.mq.dto.HackResultMessage;
import com.oj.common.mq.dto.HackTaskMessage;
import com.oj.judge.config.Judge0Client;
import com.oj.judge.entity.Submission;
import com.oj.judge.hack.ValidatorRunner;
import com.oj.judge.mapper.SubmissionMapper;
import com.oj.judge.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.HACK_TASK_TOPIC,
        consumerGroup = MqConstant.HACK_TASK_CONSUMER_GROUP,
        maxReconsumeTimes = 3
)
public class HackTaskConsumer implements RocketMQListener<HackTaskMessage> {

    @Autowired
    private ValidatorRunner validatorRunner;

    @Autowired
    private Judge0Client judge0Client;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public void onMessage(HackTaskMessage msg) {
        log.info("收到 Hack 任务: hackId={}, hacker={}, target={}, problem={}",
                msg.getHackId(), msg.getHackerId(), msg.getTargetUserId(), msg.getProblemId());

        float timeLimitSec = msg.getTimeLimitMs() != null ? msg.getTimeLimitMs() / 1000.0f : 2.0f;
        int memoryLimitKb = msg.getMemoryLimitMb() != null ? msg.getMemoryLimitMb() * 1024 : 256 * 1024;

        // 阶段①: Validator 校验（优先使用编译缓存）
        ValidatorRunner.ValidatorResult validation = validatorRunner.validate(
                msg.getValidatorPath(), msg.getValidatorExePath(),
                msg.getValidatorSrcHash(), msg.getHackInput());
        if (!validation.isValid()) {
            sendHackResult(msg, "InvalidData", null, null, null, validation.getStderr());
            return;
        }

        // 阶段②: 从数据库拉取目标代码 → Judge0 沙箱运行
        Submission targetSubmission = submissionMapper.selectById(msg.getTargetSubmissionId());
        if (targetSubmission == null) {
            sendHackResult(msg, "HackFailed", null, null, null, "目标提交记录不存在");
            return;
        }
        String targetCode = targetSubmission.getCode();
        String targetLanguage = msg.getTargetLanguage() != null ? msg.getTargetLanguage() : targetSubmission.getLanguage();
        int targetLangId = judge0Client.getLanguageId(targetLanguage);
        log.info("Hack 目标代码: lang={}, langId={}, codeLength={}", targetLanguage, targetLangId, targetCode != null ? targetCode.length() : 0);

        String targetStatus;
        String targetOutput = null;
        try {
            var targetResult = judge0Client.submitAndWait(
                    targetCode, targetLangId,
                    msg.getHackInput(), null,
                    timeLimitSec, memoryLimitKb);
            int statusId = targetResult.getJSONObject("status").getIntValue("id");
            targetStatus = judge0Client.parseStatus(statusId);
            if (statusId == 6) targetStatus = "Compile Error";
            targetOutput = judge0Client.decodeField(targetResult, "stdout");
            log.info("Hack 目标代码运行结果: status={}, output={}", targetStatus, targetOutput);
        } catch (Exception e) {
            log.error("目标代码运行异常", e);
            sendHackResult(msg, "HackFailed", "Runtime Error", null, null, "目标代码运行异常: " + e.getMessage());
            return;
        }

        // 阶段③: 读取标准解答文件 → Judge0 沙箱运行
        String referenceCode;
        try {
            referenceCode = Files.readString(Paths.get(msg.getReferencePath()));
        } catch (Exception e) {
            sendHackResult(msg, "HackFailed", targetStatus, targetOutput, null, "标准解答文件读取失败: " + e.getMessage());
            return;
        }

        String refLang = msg.getReferenceLanguage() != null ? msg.getReferenceLanguage() : "C++";
        int refLangId = judge0Client.getLanguageId(refLang);
        float refTimeSec = msg.getTimeLimitMs() != null ? msg.getTimeLimitMs() / 1000.0f * 2 : 5.0f;
        int refMemoryKb = msg.getMemoryLimitMb() != null ? Math.min(msg.getMemoryLimitMb() * 1024, 512000) : 256 * 1024;
        log.info("Hack 标准解答: lang={}, langId={}", refLang, refLangId);

        String refStatus;
        String refOutput = null;
        try {
            var refResult = judge0Client.submitAndWait(
                    referenceCode, refLangId,
                    msg.getHackInput(), null,
                    refTimeSec, refMemoryKb);
            int refStatusId = refResult.getJSONObject("status").getIntValue("id");
            refStatus = judge0Client.parseStatus(refStatusId);
            if (refStatusId == 6) refStatus = "Compile Error";
            refOutput = judge0Client.decodeField(refResult, "stdout");
            log.info("Hack 标准解答运行结果: status={}, output={}", refStatus, refOutput);
        } catch (Exception e) {
            log.error("标准解答运行异常", e);
            sendHackResult(msg, "HackFailed", targetStatus, targetOutput, null, "标准解答运行异常: " + e.getMessage());
            return;
        }

        // 阶段④: 比对输出 —— 输出不同则 Hack 成功
        String targetOutputNorm = targetOutput != null ? targetOutput.trim().replaceAll("\\s+", " ") : "";
        String refOutputNorm = refOutput != null ? refOutput.trim().replaceAll("\\s+", " ") : "";
        boolean outputsSame = targetOutputNorm.equals(refOutputNorm);

        log.info("Hack 结果判定: targetOutput=[{}], refOutput=[{}], same={}",
                targetOutputNorm, refOutputNorm, outputsSame);

        if (!outputsSame) {
            sendHackResult(msg, "HackSuccess", targetStatus, targetOutput, refOutput,
                    "目标输出与标准输出不一致 (target=" + targetOutputNorm + ", ref=" + refOutputNorm + ")");
        } else {
            sendHackResult(msg, "HackFailed", targetStatus, targetOutput, refOutput,
                    "目标输出与标准输出一致");
        }
    }

    private void sendHackResult(HackTaskMessage msg, String status,
                                 String targetResult, String targetOutput, String refOutput, String errorInfo) {
        HackResultMessage result = HackResultMessage.builder()
                .hackId(msg.getHackId())
                .contestId(msg.getContestId())
                .problemId(msg.getProblemId())
                .hackerId(msg.getHackerId())
                .targetUserId(msg.getTargetUserId())
                .targetSubmissionId(msg.getTargetSubmissionId())
                .status(status)
                .targetResult(targetResult)
                .hackOutput(refOutput)
                .hackInput(msg.getHackInput())
                .errorInfo(errorInfo)
                .build();
        rocketMQTemplate.convertAndSend(MqConstant.HACK_RESULT_TOPIC, result);
        log.info("Hack 结果已发送: hackId={}, status={}", msg.getHackId(), status);

        // WebSocket 广播 Hack 结果
        try {
            String wsMsg = com.alibaba.fastjson.JSON.toJSONString(result);
            webSocketServer.sendToAllClient("HACK_RESULT:" + wsMsg);
        } catch (Exception e) {
            log.warn("WebSocket 广播 Hack 结果失败: {}", e.getMessage());
        }
    }
}
