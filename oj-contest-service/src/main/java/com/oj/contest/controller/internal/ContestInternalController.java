package com.oj.contest.controller.internal;

import com.oj.api.dto.ContestProblemFeignDTO;
import com.oj.api.dto.WorkspaceActivityFeignDTO;
import com.oj.common.result.Result;
import com.oj.contest.entity.ContestProblem;
import com.oj.contest.mapper.ContestProblemMapper;
import com.oj.contest.service.ContestService;
import com.oj.contest.service.UserContestService;
import com.oj.contest.service.WorkSpaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/contest")
@Slf4j
public class ContestInternalController {

    @Autowired
    private ContestProblemMapper contestProblemMapper;

    @Autowired
    private UserContestService userContestService;

    @Autowired
    private WorkSpaceService workSpaceService;

    @Autowired
    private ContestService contestService;

    @GetMapping("/problem")
    public Result<ContestProblemFeignDTO> getContestProblem(
            @RequestParam("contestId") Integer contestId,
            @RequestParam("problemId") Integer problemId) {
        ContestProblemFeignDTO dto = new ContestProblemFeignDTO();
        // 查询contest_problems表获取分数和排序
        List<Long> problemIds = contestProblemMapper.selectListAll(Long.valueOf(contestId));
        boolean found = false;
        for (Long pid : problemIds) {
            if (pid.equals(Long.valueOf(problemId))) {
                found = true;
                break;
            }
        }
        if (found) {
            dto.setContestId(contestId);
            dto.setProblemId(problemId);
        }
        return Result.success(dto);
    }

    @PostMapping("/rank")
    public Result<Void> updateRankOnAccepted(
            @RequestParam("contestId") Integer contestId,
            @RequestParam("userId") Long userId,
            @RequestParam("problemId") Integer problemId,
            @RequestParam("score") Integer score) {
        userContestService.updateRankOnAccepted(contestId, userId, problemId, score);
        return Result.success();
    }

    @PostMapping("/workspace/activity")
    public Result<Void> recordWorkspaceActivity(@RequestBody WorkspaceActivityFeignDTO dto) {
        workSpaceService.recordWorkSpace(dto.getUserId(), dto.getActivityType(),
                dto.getTitle(), dto.getDescription(), dto.getTargetId(), dto.getTargetType());
        return Result.success();
    }

    @GetMapping("/problem/count")
    public Result<Long> countContestByProblemId(@RequestParam("problemId") Integer problemId) {
        return Result.success(contestProblemMapper.selectContentCount(Long.valueOf(problemId)));
    }

    @PostMapping("/problem/count-batch")
    public Result<List<Long>> countContestByProblemIds(@RequestBody List<Integer> problemIds) {
        return Result.success(contestProblemMapper.selectContentCountBatch(problemIds));
    }

    @PostMapping("/persist-rank")
    public Result<Void> persistRank(@RequestParam("contestId") Integer contestId) {
        userContestService.persistRankToDb(contestId);
        return Result.success();
    }

    @PostMapping("/rank/hack")
    public Result<Void> updateRankOnHackSuccess(
            @RequestParam("contestId") Integer contestId,
            @RequestParam("hackerId") Long hackerId,
            @RequestParam("targetUserId") Long targetUserId,
            @RequestParam("problemId") Integer problemId,
            @RequestParam("score") Integer score) {
        userContestService.updateRankOnHackSuccess(contestId, hackerId, targetUserId, problemId, score);
        return Result.success();
    }

    @PostMapping("/status/update")
    public Result<Void> updateContestStatus() {
        contestService.updateContestStatus();
        return Result.success();
    }

    /**
     * 定时任务：每5分钟更新比赛状态
     */
    @Scheduled(fixedRate = 300000)
    public void scheduledUpdateContestStatus() {
        log.info("定时更新比赛状态");
        contestService.updateContestStatus();
    }
}
