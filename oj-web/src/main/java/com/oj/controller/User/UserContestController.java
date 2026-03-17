package com.oj.controller.User;

import com.oj.context.BaseContext;
import com.oj.dto.ContestQueryDTO;
import com.oj.result.PageResult;
import com.oj.result.Result;
import com.oj.service.UserContestService;
import com.oj.vo.ContestRankVO;
import com.oj.vo.ContestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端 - 比赛接口
 */
@Slf4j
@RestController
@RequestMapping("/user/contest")
@Tag(name = "用户端-比赛接口")
public class UserContestController {

    @Autowired
    private UserContestService userContestService;

    /**
     * 分页查询比赛列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询比赛列表")
    public Result<PageResult> pageContest(ContestQueryDTO contestQueryDTO) {
        log.info("用户端分页查询比赛: {}", contestQueryDTO);
        PageResult pageResult = userContestService.pageContest(contestQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 获取比赛详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取比赛详情")
    public Result<ContestVO> getContestDetail(@PathVariable Long id) {
        log.info("用户端获取比赛详情: {}", id);
        Long userId = BaseContext.getCurrentId();
        ContestVO vo = userContestService.getContestDetail(id, userId);
        return Result.success(vo);
    }

    /**
     * 报名比赛
     */
    @PostMapping("/{id}/join")
    @Operation(summary = "报名比赛")
    public Result<Void> joinContest(@PathVariable Long id) {
        log.info("用户报名比赛: {}", id);
        Long userId = BaseContext.getCurrentId();
        userContestService.joinContest(id, userId);
        return Result.success();
    }

    /**
     * 获取比赛题目列表
     */
    @GetMapping("/{id}/problems")
    @Operation(summary = "获取比赛题目列表")
    public Result<ContestVO> getContestProblems(@PathVariable Long id) {
        log.info("用户端获取比赛题目: {}", id);
        Long userId = BaseContext.getCurrentId();
        ContestVO vo = userContestService.getContestProblems(id, userId);
        return Result.success(vo);
    }

    /**
     * 获取比赛排行榜（Redis 实时排行榜）
     */
    @GetMapping("/{id}/rank")
    @Operation(summary = "获取比赛排行榜")
    public Result<List<ContestRankVO>> getContestRank(@PathVariable Long id) {
        log.info("用户端获取比赛排行榜: {}", id);
        List<ContestRankVO> rankList = userContestService.getContestRank(id);
        return Result.success(rankList);
    }
}
