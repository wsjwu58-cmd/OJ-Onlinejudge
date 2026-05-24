package com.oj.contest.controller.user;

import com.oj.common.context.BaseContext;
import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.contest.dto.ContestQueryDTO;
import com.oj.contest.service.UserContestService;
import com.oj.contest.vo.ContestRankVO;
import com.oj.contest.vo.ContestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/contest")
@Slf4j
@Tag(name = "用户端-竞赛")
public class UserContestController {

    @Autowired
    private UserContestService userContestService;

    @GetMapping("/page")
    @Operation(summary = "分页查询竞赛列表")
    public Result<PageResult> page(ContestQueryDTO contestQueryDTO) {
        return Result.success(userContestService.pageContest(contestQueryDTO));
    }

    @GetMapping("/{contestId}")
    @Operation(summary = "查询竞赛详情")
    public Result<ContestVO> detail(@PathVariable Long contestId) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(userContestService.getContestDetail(contestId, userId));
    }

    @PostMapping("/{contestId}/join")
    @Operation(summary = "报名竞赛")
    public Result join(@PathVariable Long contestId) {
        Long userId = BaseContext.getCurrentId();
        userContestService.joinContest(contestId, userId);
        return Result.success();
    }

    @GetMapping("/{contestId}/problems")
    @Operation(summary = "获取竞赛题目列表")
    public Result<ContestVO> problems(@PathVariable Long contestId) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(userContestService.getContestProblems(contestId, userId));
    }

    @GetMapping("/{contestId}/rank")
    @Operation(summary = "获取竞赛排行榜")
    public Result<List<ContestRankVO>> rank(@PathVariable Long contestId) {
        return Result.success(userContestService.getContestRank(contestId));
    }
}
