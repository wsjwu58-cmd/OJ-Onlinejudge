package com.oj.service.tools;

import com.oj.entity.Problem;
import com.oj.entity.TestCase;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.TestCaseMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SolutionGeneratorTool {

    private static final Logger log = LoggerFactory.getLogger(SolutionGeneratorTool.class);

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Tool("获取题目详细信息，包括题目描述、难度、输入输出格式等")
    public String getProblemDetail(@P("题目ID") Integer problemId) {
        log.info("Tool调用: getProblemDetail, problemId={}", problemId);
        try {
            Problem problem = problemMapper.selectById(problemId);
            if (problem == null) {
                return "题目不存在，ID: " + problemId;
            }
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
            return "获取题目详情失败: " + e.getMessage();
        }
    }

    @Tool("获取题目的测试用例列表")
    public String getTestCases(@P("题目ID") Integer problemId) {
        log.info("Tool调用: getTestCases, problemId={}", problemId);
        try {
            List<TestCase> testCases = testCaseMapper.selectByProblemId(problemId);
            if (testCases == null || testCases.isEmpty()) {
                return "该题目暂无测试用例";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("题目 ").append(problemId).append(" 的测试用例:\n");
            for (int i = 0; i < testCases.size(); i++) {
                TestCase tc = testCases.get(i);
                sb.append("用例").append(i + 1).append(":\n");
                sb.append("  输入: ").append(tc.getInputData()).append("\n");
                sb.append("  输出: ").append(tc.getOutputData()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取测试用例失败", e);
            return "获取测试用例失败: " + e.getMessage();
        }
    }

    @Tool("生成题目的解题思路和参考代码")
    public String generateSolution(
            @P("题目ID") Integer problemId,
            @P("编程语言，如Java、Python、C++等") String language) {
        log.info("Tool调用: generateSolution, problemId={}, language={}", problemId, language);
        try {
            Problem problem = problemMapper.selectById(problemId);
            if (problem == null) {
                return "题目不存在";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("题目: ").append(problem.getTitle()).append("\n");
            sb.append("语言: ").append(language).append("\n");
            sb.append("请根据题目描述生成详细的解题思路和").append(language).append("参考代码。\n");
            sb.append("解题思路应包括：\n");
            sb.append("1. 问题分析\n");
            sb.append("2. 算法设计\n");
            sb.append("3. 复杂度分析\n");
            sb.append("4. 参考代码实现\n");
            return sb.toString();
        } catch (Exception e) {
            log.error("生成题解失败", e);
            return "生成题解失败: " + e.getMessage();
        }
    }
}
