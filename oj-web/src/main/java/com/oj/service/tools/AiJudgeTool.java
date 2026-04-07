package com.oj.service.tools;

import com.oj.exception.ParameterMissingException;
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
public class AiJudgeTool {

    private static final Logger log = LoggerFactory.getLogger(AiJudgeTool.class);

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Tool("分析代码的正确性，判断是否能通过题目要求")
    public String analyzeCodeCorrectness(
            @P("题目ID") Integer problemId,
            @P("用户提交的代码") String code,
            @P("编程语言") String language) {
        log.info("Tool调用: analyzeCodeCorrectness, problemId={}, language={}", problemId, language);
        if (problemId == null) {
            throw new ParameterMissingException("problemId");
        }
        try {
            Problem problem = problemMapper.selectById(problemId);
            if (problem == null) {
                return "题目不存在，ID: " + problemId;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("代码分析请求:\n");
            sb.append("题目: ").append(problem.getTitle()).append("\n");
            sb.append("难度: ").append(problem.getDifficulty()).append("\n");
            sb.append("语言: ").append(language).append("\n");
            sb.append("代码长度: ").append(code.length()).append(" 字符\n");
            sb.append("\n请分析以下代码:\n```").append(language.toLowerCase()).append("\n");
            sb.append(code).append("\n```\n");

            return sb.toString();
        } catch (Exception e) {
            log.error("分析代码正确性失败", e);
            throw new RuntimeException("分析代码正确性失败: " + e.getMessage(), e);
        }
    }

    @Tool("获取题目的测试用例用于验证代码")
    public String getTestCasesForJudge(@P("题目ID") Integer problemId) {
        log.info("Tool调用: getTestCasesForJudge, problemId={}", problemId);
        if (problemId == null) {
            throw new ParameterMissingException("problemId");
        }
        try {
            List<TestCase> testCases = testCaseMapper.selectByProblemId(problemId);
            if (testCases == null || testCases.isEmpty()) {
                return "该题目暂无测试用例，请根据题目描述自行构造测试数据";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("题目").append(problemId).append("的测试用例:\n");
            for (int i = 0; i < Math.min(testCases.size(), 5); i++) {
                TestCase tc = testCases.get(i);
                sb.append("测试用例").append(i + 1).append(":\n");
                sb.append("输入:\n").append(tc.getInputData()).append("\n");
                sb.append("预期输出:\n").append(tc.getOutputData()).append("\n\n");
            }
            if (testCases.size() > 5) {
                sb.append("... 共").append(testCases.size()).append("个测试用例\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("获取测试用例失败", e);
            throw new RuntimeException("获取测试用例失败: " + e.getMessage(), e);
        }
    }

    @Tool("检查代码是否存在语法错误")
    public String checkSyntax(
            @P("代码内容") String code,
            @P("编程语言") String language) {
        log.info("Tool调用: checkSyntax, language={}", language);
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("语法检查请求:\n");
            sb.append("语言: ").append(language).append("\n");
            sb.append("代码:\n```").append(language.toLowerCase()).append("\n");
            sb.append(code).append("\n```\n");
            sb.append("\n请检查以上代码是否存在语法错误，并给出详细说明。\n");

            return sb.toString();
        } catch (Exception e) {
            log.error("语法检查失败", e);
            return "语法检查失败: " + e.getMessage();
        }
    }

    @Tool("分析代码的时间复杂度和空间复杂度")
    public String analyzeComplexity(
            @P("代码内容") String code,
            @P("编程语言") String language) {
        log.info("Tool调用: analyzeComplexity, language={}", language);
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("复杂度分析请求:\n");
            sb.append("语言: ").append(language).append("\n");
            sb.append("代码:\n```").append(language.toLowerCase()).append("\n");
            sb.append(code).append("\n```\n");
            sb.append("\n请分析以上代码的时间复杂度和空间复杂度。\n");

            return sb.toString();
        } catch (Exception e) {
            log.error("复杂度分析失败", e);
            return "复杂度分析失败: " + e.getMessage();
        }
    }

    @Tool("给出代码的改进建议")
    public String suggestImprovements(
            @P("题目ID") Integer problemId,
            @P("代码内容") String code,
            @P("编程语言") String language) {
        log.info("Tool调用: suggestImprovements, problemId={}, language={}", problemId, language);
        if (problemId == null) {
            throw new ParameterMissingException("problemId");
        }
        try {
            Problem problem = problemMapper.selectById(problemId);
            StringBuilder sb = new StringBuilder();
            sb.append("代码改进建议请求:\n");
            if (problem != null) {
                sb.append("题目: ").append(problem.getTitle()).append("\n");
                sb.append("时间限制: ").append(problem.getTimeLimitMs()).append("ms\n");
                sb.append("内存限制: ").append(problem.getMemoryLimitMb()).append("MB\n");
            }
            sb.append("语言: ").append(language).append("\n");
            sb.append("代码:\n```").append(language.toLowerCase()).append("\n");
            sb.append(code).append("\n```\n");
            sb.append("\n请给出代码的改进建议，包括:\n");
            sb.append("1. 代码风格改进\n");
            sb.append("2. 性能优化建议\n");
            sb.append("3. 可能的边界情况处理\n");

            return sb.toString();
        } catch (Exception e) {
            log.error("获取改进建议失败", e);
            throw new RuntimeException("获取改进建议失败: " + e.getMessage(), e);
        }
    }
}
