package com.oj.ai.service.tools;

import com.oj.api.ProblemClient;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.api.dto.TestCaseFeignDTO;
import com.oj.common.exception.ParameterMissingException;
import com.oj.common.result.Result;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 题解生成工具 - 通过 ProblemClient Feign 调用获取题目信息
 */
@Component
@Slf4j
public class SolutionGeneratorTool {

    @Autowired
    private ProblemClient problemClient;

    @Tool("获取题目详细信息，包括题目描述、难度、输入输出格式等")
    public String getProblemDetail(@P("题目ID") Integer problemId) {
        log.info("Tool调用: getProblemDetail, problemId={}", problemId);
        if (problemId == null) {
            throw new ParameterMissingException("problemId");
        }
        try {
            Result<ProblemFeignDTO> result = problemClient.getProblemById(problemId);
            if (result == null || result.getCode() != 1 || result.getData() == null) {
                return "题目不存在，ID: " + problemId;
            }
            ProblemFeignDTO problem = result.getData();
            StringBuilder sb = new StringBuilder();
            sb.append("题目ID: ").append(problem.getId()).append("\n");
            sb.append("标题: ").append(problem.getTitle()).append("\n");
            sb.append("难度: ").append(problem.getDifficulty()).append("\n");
            if (problem.getContent() != null) {
                sb.append("描述: ").append(problem.getContent()).append("\n");
            }
            if (problem.getTimeLimitMs() != null) {
                sb.append("时间限制: ").append(problem.getTimeLimitMs()).append("ms\n");
            }
            if (problem.getMemoryLimitMb() != null) {
                sb.append("内存限制: ").append(problem.getMemoryLimitMb()).append("MB\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取题目详情失败", e);
            throw new RuntimeException("获取题目详情失败: " + e.getMessage(), e);
        }
    }

    @Tool("获取题目的测试用例列表")
    public String getTestCases(@P("题目ID") Integer problemId) {
        log.info("Tool调用: getTestCases, problemId={}", problemId);
        if (problemId == null) {
            throw new ParameterMissingException("problemId");
        }
        try {
            Result<List<TestCaseFeignDTO>> result = problemClient.getTestCasesByProblemId(problemId);
            if (result == null || result.getCode() != 1 || result.getData() == null || result.getData().isEmpty()) {
                return "该题目暂无测试用例";
            }
            List<TestCaseFeignDTO> testCases = result.getData();
            StringBuilder sb = new StringBuilder();
            sb.append("题目 ").append(problemId).append(" 的测试用例:\n");
            for (int i = 0; i < testCases.size(); i++) {
                TestCaseFeignDTO tc = testCases.get(i);
                sb.append("用例").append(i + 1).append(":\n");
                sb.append("  输入: ").append(tc.getInputData()).append("\n");
                sb.append("  输出: ").append(tc.getOutputData()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取测试用例失败", e);
            throw new RuntimeException("获取测试用例失败: " + e.getMessage(), e);
        }
    }

    @Tool("生成题目的解题思路和参考代码")
    public String generateSolution(
            @P("题目ID") Integer problemId,
            @P("编程语言，如Java、Python、C++等") String language) {
        log.info("Tool调用: generateSolution, problemId={}, language={}", problemId, language);
        if (problemId == null) {
            throw new ParameterMissingException("problemId");
        }
        try {
            Result<ProblemFeignDTO> result = problemClient.getProblemById(problemId);
            if (result == null || result.getCode() != 1 || result.getData() == null) {
                return "题目不存在";
            }
            ProblemFeignDTO problem = result.getData();
            StringBuilder sb = new StringBuilder();
            sb.append("【题目信息】\n");
            sb.append("题目ID: ").append(problem.getId()).append("\n");
            sb.append("标题: ").append(problem.getTitle()).append("\n");
            sb.append("难度: ").append(problem.getDifficulty()).append("\n");
            if (problem.getContent() != null) {
                sb.append("描述: ").append(problem.getContent()).append("\n");
            }
            if (problem.getTimeLimitMs() != null) {
                sb.append("时间限制: ").append(problem.getTimeLimitMs()).append("ms\n");
            }
            if (problem.getMemoryLimitMb() != null) {
                sb.append("内存限制: ").append(problem.getMemoryLimitMb()).append("MB\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("生成题解失败", e);
            throw new RuntimeException("生成题解失败: " + e.getMessage(), e);
        }
    }
}
