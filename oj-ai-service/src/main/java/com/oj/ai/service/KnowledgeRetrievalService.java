package com.oj.ai.service;

import java.util.List;

public interface KnowledgeRetrievalService {
    List<String> retrieveKnowledge(String query, String context, int topK);
}
