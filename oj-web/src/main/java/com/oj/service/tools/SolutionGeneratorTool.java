package com.oj.service.tools;

import com.oj.entity.Problem;
import com.oj.entity.TestCase;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.TestCaseMapper;
import com.oj.exception.ParameterMissingException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatLanguageModel;
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

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    @Tool("获取题目详细信息，包括题目描述、难度、输入输出格式等")
    public String getProblemDetail(@P("题目ID") Integer problemId) {
        log.info("Tool调用: getProblemDetail, problemId={}", problemId);
        if (problemId == null) {
            throw new ParameterMissingException("problemId");
        }
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
            Problem problem = problemMapper.selectById(problemId);
            if (problem == null) {
                return "题目不存在";
            }
            
            // 如果有LLM，直接生成完整题解
//            if (chatLanguageModel != null) {
//                String prompt = buildSolutionPrompt(problem, language);
//                String solution = chatLanguageModel.chat(prompt);
//                log.info("LLM生成题解完成，长度: {}", solution.length());
//                return solution;
//            }
            
            // 降级：返回题目信息让后续处理
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

    private String buildSolutionPrompt(Problem problem, String language) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下编程题目生成详细的解题思路和").append(language).append("参考代码。\n\n");
        prompt.append("【题目信息】\n");
        prompt.append("标题: ").append(problem.getTitle()).append("\n");
        prompt.append("难度: ").append(problem.getDifficulty()).append("\n");
        if (problem.getContent() != null) {
            prompt.append("描述: ").append(problem.getContent()).append("\n");
        }
        if (problem.getTimeLimitMs() != null) {
            prompt.append("时间限制: ").append(problem.getTimeLimitMs()).append("ms\n");
        }
        if (problem.getMemoryLimitMb() != null) {
            prompt.append("内存限制: ").append(problem.getMemoryLimitMb()).append("MB\n");
        }
        
        prompt.append("\n【输出要求】\n");
        prompt.append("请按以下格式输出：\n\n");
        prompt.append("## 问题分析\n");
        prompt.append("简要描述题目要求和关键点...\n\n");
        prompt.append("## 算法思路\n");
        prompt.append("详细的解题思路，包括关键算法步骤...\n\n");
        prompt.append("## 复杂度分析\n");
        prompt.append("- 时间复杂度：O(?)\n");
        prompt.append("- 空间复杂度：O(?)\n\n");
        prompt.append("## 参考代码（").append(language).append("）\n");
        prompt.append("```").append(language.toLowerCase()).append("\n");
        prompt.append("// 代码实现\n");
        prompt.append("```\n\n");
        prompt.append("## 代码说明\n");
        prompt.append("解释代码的关键部分...\n");
        
        return prompt.toString();
    }
}
