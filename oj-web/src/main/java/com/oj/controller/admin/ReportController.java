package com.oj.controller.admin;

import com.oj.result.Result;
import com.oj.service.ReportService;
import com.oj.vo.ProblemAcceptanceVO;
import com.oj.vo.ProblemTrendVO;
import com.oj.vo.RecordTrendVO;
import com.oj.vo.UserTrendVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/report")
@Tag(name = "数据统计接口")
@Slf4j
public class ReportController {
    @Autowired
    private ReportService reportService;

    @GetMapping("/userTrend")
    @Operation(summary = "用户注册趋势")
    public Result<UserTrendVO> userTrend(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                             LocalDate begin,
                                         @DateTimeFormat(pattern = "yyyy-MM-dd")
                                             LocalDate end){
        log.info("用户注册趋势：{},{}",begin,end);
        UserTrendVO userTrendVO=reportService.getUserTrend(begin,end);
        return Result.success(userTrendVO);
    }

    @GetMapping("/problemTrend")
    @Operation(summary = "题目数量趋势")
    public  Result<ProblemTrendVO> problemTrend(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                    LocalDate begin,
                                                @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                    LocalDate end){
        log.info("题目数量趋势:{},{}",begin,end);
        ProblemTrendVO problemTrendVO=reportService.problemTrend(begin,end);
        return Result.success(problemTrendVO);

    }

    @GetMapping("/ProblemRecord")
    @Operation(summary = "提交记录趋势")
    public Result<RecordTrendVO> recordTrend(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                 LocalDate begin,
                                             @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                 LocalDate end){
        log.info("提交记录趋势：{},{}",begin,end);
        RecordTrendVO recordTrendVO=reportService.recordTrend(begin,end);
        return Result.success(recordTrendVO);
    }
    @GetMapping("/Percent")
    @Operation(summary = "题目通过率排行")
    public Result<List<ProblemAcceptanceVO>> Accept(){
        log.info("题目通过率top10");
        List<ProblemAcceptanceVO> acceptanceVOList=reportService.problemAccept();
        return Result.success(acceptanceVOList);
    }


}
