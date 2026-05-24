package com.oj.problem.service;

import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeImportService {
    int importPdf(MultipartFile file, String category);
    int importPdfDirectory(String directoryPath, String category);
    void clearKnowledgeBase();
}
