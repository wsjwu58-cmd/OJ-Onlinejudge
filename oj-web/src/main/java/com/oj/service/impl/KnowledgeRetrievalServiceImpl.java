package com.oj.service.impl;

import com.oj.service.KnowledgeRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    @Autowired
    private VectorStore vectorStore;
    
    @Value("${knowledge.auto-import.enabled:false}")
    private boolean autoImportEnabled;
    
    @Value("${knowledge.auto-import.path:}")
    private String autoImportPath;

    @Override
    public List<String> retrieveKnowledge(String query, String context, int topK) {
        String fullQuery = query + " " + context;
        
        SearchRequest request = SearchRequest.builder()
                .query(fullQuery)
                .topK(topK)
                .build();
        
        List<Document> documents = vectorStore.similaritySearch(request);
        
        return documents.stream()
                .map(Document::getContent)
                .collect(Collectors.toList());
    }

    @Override
    public void updateKnowledgeBase() {
        log.info("知识库更新方法已调用，请使用 KnowledgeImportService 导入PDF文件");
    }
}