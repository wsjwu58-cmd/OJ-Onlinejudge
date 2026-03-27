package com.oj.service.impl;

import com.oj.service.KnowledgeRetrievalService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${knowledge.auto-import.enabled:false}")
    private boolean autoImportEnabled;

    @Value("${knowledge.auto-import.path:}")
    private String autoImportPath;

    @Override
    public List<String> retrieveKnowledge(String query, String context, int topK) {
        String fullQuery = query + " " + context;

        try {
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embeddingModel.embed(fullQuery).content())
                    .maxResults(topK)
                    .minScore(0.5)
                    .build();

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

            return result.matches().stream()
                    .map(match -> match.embedded().text())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("检索知识库失败", e);
            return List.of();
        }
    }

    @Override
    public void updateKnowledgeBase() {
        log.info("知识库更新方法已调用，请使用 KnowledgeImportService 导入PDF文件");
    }
}
