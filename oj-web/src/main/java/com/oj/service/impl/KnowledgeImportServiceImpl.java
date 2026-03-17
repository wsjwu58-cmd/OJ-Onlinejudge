package com.oj.service.impl;

import com.oj.service.KnowledgeImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private VectorStore vectorStore;

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
            Resource pdfResource = new FileSystemResource(pdfPath.toFile());
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
            List<Document> rawDocuments = pdfReader.get();

            // Embedding 模型最多支持 512 token，使用 TokenTextSplitter 分块
            // defaultChunkSize=350 token，保留 overlap 避免语义截断
            TokenTextSplitter splitter = new TokenTextSplitter(350, 50, 5, 10000, true);

            List<Document> chunkedDocuments = new ArrayList<>();

            for (Document doc : rawDocuments) {
                String content = doc.getText();
                if (!StringUtils.hasText(content)) {
                    continue;
                }

                // 先分块
                List<Document> chunks = splitter.apply(List.of(doc));

                for (Document chunk : chunks) {
                    // 补充元数据
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("category", category);
                    metadata.put("source", sourceName);
                    metadata.put("importTime", System.currentTimeMillis());
                    if (doc.getMetadata() != null) {
                        metadata.putAll(doc.getMetadata());
                    }
                    if (chunk.getMetadata() != null) {
                        metadata.putAll(chunk.getMetadata());
                    }
                    chunkedDocuments.add(new Document(chunk.getText(), metadata));
                }
            }

            if (chunkedDocuments.isEmpty()) {
                log.warn("PDF文件 {} 未提取到有效内容", sourceName);
                return 0;
            }

            // 分批写入，每批 10 个，避免单次请求过大
            int batchSize = 10;
            for (int i = 0; i < chunkedDocuments.size(); i += batchSize) {
                List<Document> batch = chunkedDocuments.subList(i, Math.min(i + batchSize, chunkedDocuments.size()));
                vectorStore.add(batch);
                log.info("已写入批次 {}/{}, 共 {} 个分块", (i / batchSize + 1),
                        (int) Math.ceil((double) chunkedDocuments.size() / batchSize), batch.size());
            }

            log.info("PDF文件 {} 分块完成，共 {} 个分块", sourceName, chunkedDocuments.size());
            return chunkedDocuments.size();

        } catch (Exception e) {
            log.error("处理PDF文件失败: {}", pdfPath, e);
            throw new RuntimeException("处理PDF文件失败: " + e.getMessage());
        }
    }
}
