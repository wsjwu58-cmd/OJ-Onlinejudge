package com.oj.service.tools;

import com.oj.service.KnowledgeRetrievalService;
import com.oj.exception.ParameterMissingException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeRetrievalTool {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetrievalTool.class);

    @Autowired
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Tool("检索知识库，获取与用户问题相关的知识片段。当用户询问编程概念、算法、数据结构、错误分析等问题时使用。")
    public String searchKnowledge(
            @P("用户的问题或查询内容") String query,
            @P("上下文信息，如题目描述、代码片段等，可以为空") String context,
            @P("返回的相关知识数量，默认为3，范围1-5") Integer topK) {
        log.info("Tool调用: searchKnowledge, query={}, context={}, topK={}", query, context, topK);
        try {
            if (topK == null || topK < 1) {
                topK = 3;
            } else if (topK > 5) {
                topK = 5;
            }

            List<String> knowledgeList = knowledgeRetrievalService.retrieveKnowledge(
                    query,
                    context != null ? context : "",
                    topK
            );

            if (knowledgeList.isEmpty()) {
                return "未在知识库中找到相关知识";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("从知识库中检索到 ").append(knowledgeList.size()).append(" 条相关知识：\n\n");
            for (int i = 0; i < knowledgeList.size(); i++) {
                sb.append("【知识").append(i + 1).append("】\n");
                sb.append(knowledgeList.get(i)).append("\n\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("知识检索失败", e);
            return "知识检索失败: " + e.getMessage();
        }
    }

    @Tool("检索与特定题目相关的知识点。当用户询问某个题目的解题思路、相关知识时使用。")
    public String searchProblemKnowledge(
            @P("题目ID") Integer problemId,
            @P("用户的具体问题") String question) {
        log.info("Tool调用: searchProblemKnowledge, problemId={}, question={}", problemId, question);
        if (problemId == null) {
            throw new ParameterMissingException("problemId");
        }
        try {
            String context = "题目ID: " + problemId;
            List<String> knowledgeList = knowledgeRetrievalService.retrieveKnowledge(
                    question,
                    context,
                    3
            );

            if (knowledgeList.isEmpty()) {
                return "未找到与题目 " + problemId + " 相关的知识";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("题目 ").append(problemId).append(" 的相关知识：\n\n");
            for (int i = 0; i < knowledgeList.size(); i++) {
                sb.append((i + 1)).append(". ").append(knowledgeList.get(i)).append("\n\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("题目知识检索失败", e);
            throw new RuntimeException("题目知识检索失败: " + e.getMessage(), e);
        }
    }

    @Tool("检索与代码错误相关的解决方案。当用户遇到编译错误、运行时错误或逻辑错误时使用。")
    public String searchErrorSolution(
            @P("错误信息或错误类型") String errorInfo,
            @P("出错的代码片段") String code) {
        log.info("Tool调用: searchErrorSolution, errorInfo={}, code length={}", errorInfo, 
                code != null ? code.length() : 0);
        try {
            String context = "错误: " + errorInfo;
            if (code != null && !code.trim().isEmpty()) {
                context += "\n代码: " + code;
            }

            List<String> knowledgeList = knowledgeRetrievalService.retrieveKnowledge(
                    "错误分析: " + errorInfo,
                    context,
                    3
            );

            if (knowledgeList.isEmpty()) {
                return "未找到与该错误相关的解决方案";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("针对错误 \"").append(errorInfo).append("\" 的解决方案：\n\n");
            for (int i = 0; i < knowledgeList.size(); i++) {
                sb.append("【方案").append(i + 1).append("】\n");
                sb.append(knowledgeList.get(i)).append("\n\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("错误解决方案检索失败", e);
            return "错误解决方案检索失败: " + e.getMessage();
        }
    }
}
