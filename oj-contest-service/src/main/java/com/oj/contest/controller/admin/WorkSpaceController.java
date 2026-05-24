package com.oj.contest.controller.admin;

import com.oj.common.result.Result;
import com.oj.contest.service.WorkSpaceService;
import com.oj.contest.vo.ContestDataVO;
import com.oj.contest.vo.ProblemDataVO;
import com.oj.contest.vo.WorkDataVO;
import com.oj.contest.vo.WorkSpaceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/workSpace")
@Slf4j
@Tag(name = "管理端-工作台")
public class WorkSpaceController {

    @Autowired
    private WorkSpaceService workSpaceService;

    @GetMapping("/recent")
    @Operation(summary = "获取最近活动")
    public Result<List<WorkSpaceVO>> getRecent(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(workSpaceService.getWorkspace(limit));
    }

    @GetMapping("/data")
    @Operation(summary = "获取营业数据")
    public Result<WorkDataVO> getDate() {
        java.time.LocalDateTime begin = java.time.LocalDateTime.now().with(java.time.LocalTime.MIN);
        java.time.LocalDateTime end = java.time.LocalDateTime.now().with(java.time.LocalTime.MAX);
        return Result.success(workSpaceService.getWorkData(begin, end));
    }

    @GetMapping("/problem")
    @Operation(summary = "获取题目数据")
    public Result<ProblemDataVO> getProblem() {
        return Result.success(workSpaceService.getProblem());
    }

    @GetMapping("/context")
    @Operation(summary = "比赛数据")
    public Result<ContestDataVO> getContest() {
        return Result.success(workSpaceService.getContest());
    }

    @GetMapping("/export")
    @Operation(summary = "导出运营数据Excel")
    public void export(HttpServletResponse response) {
        workSpaceService.export(response);
    }
}
