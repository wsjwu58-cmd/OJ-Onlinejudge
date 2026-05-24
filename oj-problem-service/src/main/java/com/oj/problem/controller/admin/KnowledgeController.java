package com.oj.problem.controller.admin;

import com.oj.common.result.Result;
import com.oj.problem.service.KnowledgeImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/knowledge")
@Tag(name = "管理端-知识库管理")
public class KnowledgeController {

    @Autowired
    private KnowledgeImportService knowledgeImportService;

    @PostMapping("/import/pdf")
    @Operation(summary = "上传PDF导入知识库")
    public Result<Map<String, Object>> importPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "general") String category) {
        log.info("开始导入PDF文件: {}, 分类: {}", file.getOriginalFilename(), category);
        try {
            int count = knowledgeImportService.importPdf(file, category);
            Map<String, Object> result = new HashMap<>();
            result.put("filename", file.getOriginalFilename());
            result.put("category", category);
            result.put("documentCount", count);
            result.put("message", "成功导入 " + count + " 个文档片段");
            return Result.success(result);
        } catch (Exception e) {
            log.error("导入PDF失败: {}", e.getMessage(), e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/import/directory")
    @Operation(summary = "批量导入目录下的PDF文件")
    public Result<Map<String, Object>> importDirectory(
            @RequestParam("path") String directoryPath,
            @RequestParam(value = "category", defaultValue = "general") String category) {
        log.info("开始批量导入目录: {}, 分类: {}", directoryPath, category);

        if (!isAllowedDirectory(directoryPath)) {
            return Result.error("不允许访问该目录");
        }

        try {
            int count = knowledgeImportService.importPdfDirectory(directoryPath, category);
            Map<String, Object> result = new HashMap<>();
            result.put("directory", directoryPath);
            result.put("category", category);
            result.put("totalDocumentCount", count);
            result.put("message", "成功导入 " + count + " 个文档片段");
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量导入失败: {}", e.getMessage(), e);
            return Result.error("批量导入失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空知识库")
    public Result<String> clearKnowledgeBase() {
        log.warn("收到清空知识库请求");
        try {
            knowledgeImportService.clearKnowledgeBase();
            return Result.success("知识库已清空");
        } catch (Exception e) {
            log.error("清空知识库失败: {}", e.getMessage(), e);
            return Result.error("清空失败: " + e.getMessage());
        }
    }

    private boolean isAllowedDirectory(String path) {
        String[] allowedPrefixes = {
                "/data/knowledge/",
                "/home/knowledge/",
                "./knowledge/",
                "C:/knowledge/",
                "D:/knowledge/"
        };

        for (String prefix : allowedPrefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}
