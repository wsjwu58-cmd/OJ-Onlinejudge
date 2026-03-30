package com.oj.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.entity.MaliciousCodeLog;
import com.oj.mapper.MaliciousCodeLogMapper;
import com.oj.result.PageResult;
import com.oj.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/malicious-code")
@Tag(name = "管理端-恶意代码检测")
public class MaliciousCodeController {

    @Autowired
    private MaliciousCodeLogMapper maliciousCodeLogMapper;

    @GetMapping("/page")
    @Operation(summary = "分页查询恶意代码记录")
    public Result<PageResult> pageQuery(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {

        Page<MaliciousCodeLog> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<MaliciousCodeLog> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(userId != null, MaliciousCodeLog::getUserId, userId)
                .eq(language != null && !language.isEmpty(), MaliciousCodeLog::getLanguage, language)
                .ge(startTime != null, MaliciousCodeLog::getCreateTime, startTime)
                .le(endTime != null, MaliciousCodeLog::getCreateTime, endTime)
                .orderByDesc(MaliciousCodeLog::getCreateTime);

        maliciousCodeLogMapper.selectPage(pageInfo, queryWrapper);

        return Result.success(new PageResult(pageInfo.getTotal(), pageInfo.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询恶意代码详情")
    public Result<MaliciousCodeLog> getById(@PathVariable Long id) {
        MaliciousCodeLog log = maliciousCodeLogMapper.selectById(id);
        if (log == null) {
            return Result.error("记录不存在");
        }
        return Result.success(log);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取恶意代码统计信息")
    public Result<MaliciousCodeStats> getStats(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {

        LambdaQueryWrapper<MaliciousCodeLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(startTime != null, MaliciousCodeLog::getCreateTime, startTime)
                .le(endTime != null, MaliciousCodeLog::getCreateTime, endTime);

        Long totalCount = maliciousCodeLogMapper.selectCount(queryWrapper);

        List<MaliciousCodeLog> allLogs = maliciousCodeLogMapper.selectList(queryWrapper);
        long javaCount = allLogs.stream().filter(log -> "java".equalsIgnoreCase(log.getLanguage())).count();
        long pythonCount = allLogs.stream().filter(log -> "python".equalsIgnoreCase(log.getLanguage())).count();
        long cppCount = allLogs.stream().filter(log -> "cpp".equalsIgnoreCase(log.getLanguage()) || "c++".equalsIgnoreCase(log.getLanguage())).count();
        long otherCount = totalCount - javaCount - pythonCount - cppCount;

        MaliciousCodeStats stats = MaliciousCodeStats.builder()
                .totalCount(totalCount)
                .javaCount(javaCount)
                .pythonCount(pythonCount)
                .cppCount(cppCount)
                .otherCount(otherCount)
                .build();

        return Result.success(stats);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除恶意代码记录")
    public Result<String> delete(@PathVariable Long id) {
        maliciousCodeLogMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MaliciousCodeStats {
        private Long totalCount;
        private Long javaCount;
        private Long pythonCount;
        private Long cppCount;
        private Long otherCount;
    }
}
