package com.oj.service.impl;

import com.oj.service.KnowledgeImportService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
public class KnowledgeImportServiceImpl implements KnowledgeImportService {

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Override
    public int importPdf(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF文件不能为空");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("只支持PDF文件格式");
        }
        
        try {
            Path tempDir = Files.createTempDirectory("pdf_import_");
            Path tempFile = tempDir.resolve(filename);
            file.transferTo(tempFile.toFile());
            
            int count = processPdfFile(tempFile, category, filename);
            
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);
            
            log.info("成功导入PDF文件: {}, 共 {} 个文档片段", filename, count);
            return count;
            
        } catch (IOException e) {
            log.error("导入PDF文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("导入PDF文件失败: " + e.getMessage());
        }
    }

    @Override
    public int importPdfDirectory(String directoryPath, String category) {
        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("目录不存在或不是有效目录: " + directoryPath);
        }
        
        int totalCount = 0;
        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> pdfFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .toList();
            
            log.info("发现 {} 个PDF文件待导入", pdfFiles.size());
            
            for (Path pdfFile : pdfFiles) {
                try {
                    int count = processPdfFile(pdfFile, category, pdfFile.getFileName().toString());
                    totalCount += count;
                    log.info("已导入: {} ({} 个文档片段)", pdfFile.getFileName(), count);
                } catch (Exception e) {
                    log.error("导入文件失败 {}: {}", pdfFile.getFileName(), e.getMessage());
                }
            }
            
            log.info("目录导入完成，共导入 {} 个文档片段", totalCount);
            return totalCount;
            
        } catch (IOException e) {
            log.error("遍历目录失败: {}", e.getMessage(), e);
            throw new RuntimeException("遍历目录失败: " + e.getMessage());
        }
    }

    @Override
    public void clearKnowledgeBase() {
        log.warn("清空向量数据库功能需要根据具体向量存储实现");
    }
    
    private int processPdfFile(Path pdfPath, String category, String sourceName) {
        try {
            DocumentParser parser = new ApachePdfBoxDocumentParser();
            Document document = null;
            
            try (InputStream inputStream = Files.newInputStream(pdfPath)) {
                document = parser.parse(inputStream);
            }
            
            if (document == null || !StringUtils.hasText(document.text())) {
                log.warn("PDF文件 {} 未提取到有效内容", sourceName);
                return 0;
            }

            DocumentSplitter splitter = DocumentSplitters.recursive(350, 50);
            
            List<TextSegment> segments = splitter.split(document);
            List<Document> documentsToIngest = new ArrayList<>();
            
            for (TextSegment segment : segments) {
                Map<String, Object> metadataMap = new HashMap<>();
                metadataMap.put("category", category);
                metadataMap.put("source", sourceName);
                metadataMap.put("importTime", System.currentTimeMillis());
                // 【关键修改】使用 Metadata 构造函数包装 Map，而不是强转
                Metadata metadata = new Metadata(metadataMap);

                Document doc = Document.from(segment.text(), metadata);
                documentsToIngest.add(doc);
            }


            EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build()
                    .ingest(documentsToIngest);

            log.info("PDF文件 {} 分块完成，共 {} 个分块", sourceName, documentsToIngest.size());
            return documentsToIngest.size();

        } catch (Exception e) {
            log.error("处理PDF文件失败: {}", pdfPath, e);
            throw new RuntimeException("处理PDF文件失败: " + e.getMessage());
        }
    }
}
