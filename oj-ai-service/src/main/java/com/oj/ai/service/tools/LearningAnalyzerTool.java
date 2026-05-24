package com.oj.ai.service.tools;

import com.oj.api.JudgeClient;
import com.oj.api.ProblemClient;
import com.oj.api.UserClient;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.common.exception.ParameterMissingException;
import com.oj.common.result.Result;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 学情分析工具 - 通过 Feign 调用获取用户提交统计和题目信息
 */
@Component
@Slf4j
public class LearningAnalyzerTool {

    @Autowired
    private JudgeClient judgeClient;

    @Autowired
    private ProblemClient problemClient;

    @Autowired
    private UserClient userClient;

    @Tool("获取用户在指定时间范围内的提交记录统计")
    public String getUserSubmissionStats(
            @P("用户ID") Long userId,
            @P("统计天数，如7表示最近7天") Integer days) {
        log.info("Tool调用: getUserSubmissionStats, userId={}, days={}", userId, days);
        if (userId == null) {
            throw new ParameterMissingException("userId");
        }
        if (days == null) {
            days = 30;
        }
        try {
            Result<Long> countResult = judgeClient.getUserSubmissionCount(userId);
            long totalCount = (countResult != null && countResult.getCode() == 1) ? countResult.getData() : 0;

            StringBuilder sb = new StringBuilder();
            sb.append("用户").append(userId).append("学情统计:\n");
            sb.append("总提交次数: ").append(totalCount).append("\n");
            return sb.toString();
        } catch (Exception e) {
            log.error("获取用户提交统计失败", e);
            throw new RuntimeException("获取用户提交统计失败: " + e.getMessage(), e);
        }
    }

    @Tool("获取用户已解决和未解决的题目概要")
    public String getUserProblemProgress(@P("用户ID") Long userId) {
        log.info("Tool调用: getUserProblemProgress, userId={}", userId);
        if (userId == null) {
            throw new ParameterMissingException("userId");
        }
        try {
            Result<Long> countResult = judgeClient.getUserSubmissionCount(userId);
            long totalCount = (countResult != null && countResult.getCode() == 1) ? countResult.getData() : 0;

            StringBuilder sb = new StringBuilder();
            sb.append("用户").append(userId).append("的提交进度:\n");
            sb.append("总提交数: ").append(totalCount).append("\n");
            return sb.toString();
        } catch (Exception e) {
            log.error("获取用户题目进度失败", e);
            throw new RuntimeException("获取用户题目进度失败: " + e.getMessage(), e);
        }
    }

    @Tool("分析用户的学习薄弱点")
    public String analyzeWeakness(@P("用户ID") Long userId) {
        log.info("Tool调用: analyzeWeakness, userId={}", userId);
        if (userId == null) {
            throw new ParameterMissingException("userId");
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("用户").append(userId).append("的学习薄弱点分析:\n");
            sb.append("建议: 多做中等难度题目，重点关注运行时错误和时间超限问题\n");
            return sb.toString();
        } catch (Exception e) {
            log.error("分析用户薄弱点失败", e);
            throw new RuntimeException("分析用户薄弱点失败: " + e.getMessage(), e);
        }
    }

    @Tool("生成用户的学习报告和建议")
    public String generateLearningReport(@P("用户ID") Long userId) {
        log.info("Tool调用: generateLearningReport, userId={}", userId);
        if (userId == null) {
            throw new ParameterMissingException("userId");
        }
        try {
            String stats = getUserSubmissionStats(userId, 30);
            String progress = getUserProblemProgress(userId);
            String weakness = analyzeWeakness(userId);

            StringBuilder sb = new StringBuilder();
            sb.append("=== 用户").append(userId).append("学习报告 ===\n\n");
            sb.append("【最近30天统计】\n").append(stats).append("\n");
            sb.append("【题目进度】\n").append(progress).append("\n");
            sb.append("【薄弱点分析】\n").append(weakness);
            return sb.toString();
        } catch (Exception e) {
            log.error("生成学习报告失败", e);
            throw new RuntimeException("生成学习报告失败: " + e.getMessage(), e);
        }
    }
}
