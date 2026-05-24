package com.oj.ai.service.impl;

import com.oj.ai.service.KnowledgeRetrievalService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
}
