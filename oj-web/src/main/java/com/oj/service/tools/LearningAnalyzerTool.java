package com.oj.service.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oj.exception.ParameterMissingException;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubMissionMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class LearningAnalyzerTool {

    private static final Logger log = LoggerFactory.getLogger(LearningAnalyzerTool.class);

    @Autowired
    private SubMissionMapper subMissionMapper;

    @Autowired
    private ProblemMapper problemMapper;

    @Tool("获取用户在指定时间范围内的提交记录统计")
    public String getUserSubmissionStats(
            @P("用户ID") Long userId,
            @P("统计天数，如7表示最近7天") Integer days) {
        log.info("Tool调用: getUserSubmissionStats, userId={}, days={}", userId, days);
        if (userId == null) {
            throw new ParameterMissingException("userId");
        }
        try {
            LocalDateTime startTime = LocalDateTime.now().minusDays(days);
            LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Submission::getUserId, userId)
                    .ge(Submission::getSubmitTime, startTime);
            List<Submission> submissions = subMissionMapper.selectList(wrapper);

            if (submissions.isEmpty()) {
                return "用户" + userId + "在最近" + days + "天内没有提交记录";
            }

            long totalCount = submissions.size();
            long acceptedCount = submissions.stream()
                    .filter(s -> "Accepted".equals(s.getStatus()))
                    .count();
            double acceptRate = totalCount > 0 ? (acceptedCount * 100.0 / totalCount) : 0;

            Map<String, Long> statusCount = submissions.stream()
                    .collect(Collectors.groupingBy(Submission::getStatus, Collectors.counting()));

            Map<String, Long> languageCount = submissions.stream()
                    .collect(Collectors.groupingBy(Submission::getLanguage, Collectors.counting()));

            StringBuilder sb = new StringBuilder();
            sb.append("用户").append(userId).append("最近").append(days).append("天学情统计:\n");
            sb.append("总提交次数: ").append(totalCount).append("\n");
            sb.append("通过次数: ").append(acceptedCount).append("\n");
            sb.append("通过率: ").append(String.format("%.2f", acceptRate)).append("%\n");
            sb.append("各状态分布:\n");
            statusCount.forEach((status, count) -> 
                    sb.append("  ").append(status).append(": ").append(count).append("\n"));
            sb.append("各语言使用:\n");
            languageCount.forEach((lang, count) -> 
                    sb.append("  ").append(lang).append(": ").append(count).append("\n"));

            return sb.toString();
        } catch (Exception e) {
            log.error("获取用户提交统计失败", e);
            throw new RuntimeException("获取用户提交统计失败: " + e.getMessage(), e);
        }
    }

    @Tool("获取用户已解决和未解决的题目列表")
    public String getUserProblemProgress(@P("用户ID") Long userId) {
        log.info("Tool调用: getUserProblemProgress, userId={}", userId);
        if (userId == null) {
            throw new ParameterMissingException("userId");
        }
        try {
            LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Submission::getUserId, userId);
            List<Submission> submissions = subMissionMapper.selectList(wrapper);

            Set<Integer> acceptedProblems = submissions.stream()
                    .filter(s -> "Accepted".equals(s.getStatus()))
                    .map(Submission::getProblemId)
                    .collect(Collectors.toSet());

            Set<Integer> attemptedProblems = submissions.stream()
                    .map(Submission::getProblemId)
                    .collect(Collectors.toSet());

            Set<Integer> failedProblems = new HashSet<>(attemptedProblems);
            failedProblems.removeAll(acceptedProblems);

            StringBuilder sb = new StringBuilder();
            sb.append("用户").append(userId).append("的题目进度:\n");
            sb.append("已解决题目数: ").append(acceptedProblems.size()).append("\n");
            sb.append("已解决题目ID: ").append(acceptedProblems).append("\n");
            sb.append("尝试但未解决题目数: ").append(failedProblems.size()).append("\n");
            sb.append("尝试但未解决题目ID: ").append(failedProblems).append("\n");

            return sb.toString();
        } catch (Exception e) {
            log.error("获取用户题目进度失败", e);
            throw new RuntimeException("获取用户题目进度失败: " + e.getMessage(), e);
        }
    }

    @Tool("分析用户的学习薄弱点，包括难度分布和错误类型")
    public String analyzeWeakness(@P("用户ID") Long userId) {
        log.info("Tool调用: analyzeWeakness, userId={}", userId);
        if (userId == null) {
            throw new ParameterMissingException("userId");
        }
        try {
            LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Submission::getUserId, userId);
            List<Submission> submissions = subMissionMapper.selectList(wrapper);

            if (submissions.isEmpty()) {
                return "用户" + userId + "暂无提交记录，无法分析薄弱点";
            }

            Map<String, Long> errorTypes = submissions.stream()
                    .filter(s -> !"Accepted".equals(s.getStatus()))
                    .collect(Collectors.groupingBy(Submission::getStatus, Collectors.counting()));

            Map<Integer, List<Submission>> problemSubmissions = submissions.stream()
                    .collect(Collectors.groupingBy(Submission::getProblemId));

            Map<String, Integer> difficultyFailCount = new HashMap<>();
            for (Map.Entry<Integer, List<Submission>> entry : problemSubmissions.entrySet()) {
                Problem problem = problemMapper.selectById(entry.getKey());
                if (problem != null && problem.getDifficulty() != null) {
                    long fails = entry.getValue().stream()
                            .filter(s -> !"Accepted".equals(s.getStatus()))
                            .count();
                    if (fails > 0) {
                        difficultyFailCount.merge(problem.getDifficulty(), (int) fails, Integer::sum);
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("用户").append(userId).append("的学习薄弱点分析:\n");
            sb.append("\n错误类型分布:\n");
            if (errorTypes.isEmpty()) {
                sb.append("  暂无错误记录\n");
            } else {
                errorTypes.forEach((type, count) -> 
                        sb.append("  ").append(type).append(": ").append(count).append("次\n"));
            }
            sb.append("\n各难度失败次数:\n");
            if (difficultyFailCount.isEmpty()) {
                sb.append("  暂无失败记录\n");
            } else {
                difficultyFailCount.forEach((diff, count) -> 
                        sb.append("  ").append(diff).append(": ").append(count).append("次\n"));
            }

            sb.append("\n建议:\n");
            if (!errorTypes.isEmpty()) {
                String topError = errorTypes.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("");
                sb.append("  重点关注").append(topError).append("类型的错误\n");
            }
            if (!difficultyFailCount.isEmpty()) {
                String weakDiff = difficultyFailCount.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("");
                sb.append("  建议多练习").append(weakDiff).append("难度的题目\n");
            }

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
