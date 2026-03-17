package com.oj.service;

import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeImportService {
    
    /**
     * 导入PDF文件到向量数据库
     * @param file PDF文件
     * @param category 知识分类
     * @return 导入的文档数量
     */
    int importPdf(MultipartFile file, String category);
    
    /**
     * 导入指定目录下的所有PDF文件
     * @param directoryPath 目录路径
     * @param category 知识分类
     * @return 导入的文档总数
     */
    int importPdfDirectory(String directoryPath, String category);
    
    /**
     * 清空向量数据库中的所有知识
     */
    void clearKnowledgeBase();
}
