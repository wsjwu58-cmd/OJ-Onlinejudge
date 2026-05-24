package com.oj.contest.controller.admin;

import com.oj.common.result.Result;
import com.oj.contest.service.ReportService;
import com.oj.contest.vo.ProblemAcceptanceVO;
import com.oj.contest.vo.ProblemTrendVO;
import com.oj.contest.vo.RecordTrendVO;
import com.oj.contest.vo.UserTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/report")
@Slf4j
@Tag(name = "管理端-数据报告")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/userTrend")
    @Operation(summary = "用户趋势")
    public Result<UserTrendVO> userTrend(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return Result.success(reportService.getUserTrend(begin, end));
    }

    @GetMapping("/problemTrend")
    @Operation(summary = "题目趋势")
    public Result<ProblemTrendVO> problemTrend(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return Result.success(reportService.problemTrend(begin, end));
    }

    @GetMapping("/ProblemRecord")
    @Operation(summary = "提交记录趋势")
    public Result<RecordTrendVO> recordTrend(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return Result.success(reportService.recordTrend(begin, end));
    }

    @GetMapping("/Percent")
    @Operation(summary = "题目通过率Top10")
    public Result<List<ProblemAcceptanceVO>> problemAccept() {
        return Result.success(reportService.problemAccept());
    }
}
