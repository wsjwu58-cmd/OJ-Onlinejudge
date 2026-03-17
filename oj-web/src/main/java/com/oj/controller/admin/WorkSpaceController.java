package com.oj.controller.admin;

import com.oj.result.Result;
import com.oj.service.WorkSpaceService;
import com.oj.vo.ContestDataVO;
import com.oj.vo.ProblemDataVO;
import com.oj.vo.WorkDataVO;
import com.oj.vo.WorkSpaceVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/admin/workSpace")
@Slf4j
@Tag(name = "工作台接口")
public class WorkSpaceController {
    @Autowired
    private WorkSpaceService workSpaceService;

    @GetMapping("/recent")
    @Operation(summary = "获取最近活动")
    public Result<List<WorkSpaceVO>> getRecent(@RequestParam (defaultValue = "10") Integer limit){
        log.info("获取最近活动，条数：{}",limit);
        List<WorkSpaceVO> workSpaceVOList=workSpaceService.getWorkspace(limit);
        return Result.success(workSpaceVOList);
    }

    @GetMapping("/data")
    @Operation(summary = "获取营业数据")
    public Result<WorkDataVO> getDate(){
        log.info("获取营业数据");
        // 获取当天的开始时间（00:00:00）
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
        // 获取当天的结束时间（23:59:59.999999999）
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);
        WorkDataVO workDataVO=workSpaceService.getWorkData(begin,end);
        return Result.success(workDataVO);
    }

    @GetMapping("/problem")
    @Operation(summary = "获取题目")
    public Result<ProblemDataVO> getProblem(){
        log.info("获取题目数据");
        ProblemDataVO problemDataVO=workSpaceService.getProblem();
        return Result.success(problemDataVO);
    }

    @GetMapping("/context")
    @Operation(summary = "比赛数据")
    public Result<ContestDataVO> getContest(){
        log.info("获取比赛数据");
       ContestDataVO contestDataVO= workSpaceService.getContest();
       return Result.success(contestDataVO);
    }
    @GetMapping("/export")
    @Operation(summary = "导出订单数据")
    public void export(HttpServletResponse httpServletResponse){
        log.info("导出订单数据");
        workSpaceService.export(httpServletResponse);
    }

    }
