package com.oj.api;

import com.oj.api.dto.ContestProblemFeignDTO;
import com.oj.api.dto.WorkspaceActivityFeignDTO;
import com.oj.api.fallback.ContestClientFallbackFactory;
import com.oj.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "oj-contest-service", fallbackFactory = ContestClientFallbackFactory.class)
public interface ContestClient {
    @GetMapping("/internal/contest/problem")
    Result<ContestProblemFeignDTO> getContestProblem(@RequestParam("contestId") Integer contestId, @RequestParam("problemId") Integer problemId);

    @PostMapping("/internal/contest/rank")
    Result<Void> updateRankOnAccepted(@RequestParam("contestId") Integer contestId, @RequestParam("userId") Long userId, @RequestParam("problemId") Integer problemId, @RequestParam("score") Integer score);

    @PostMapping("/internal/contest/workspace/activity")
    Result<Void> recordWorkspaceActivity(@RequestBody WorkspaceActivityFeignDTO dto);

    @GetMapping("/internal/contest/problem/count")
    Result<Long> countContestByProblemId(@RequestParam("problemId") Integer problemId);

    @PostMapping("/internal/contest/problem/count-batch")
    Result<List<Long>> countContestByProblemIds(@RequestBody List<Integer> problemIds);

    @PostMapping("/internal/contest/rank/hack")
    Result<Void> updateRankOnHackSuccess(@RequestParam("contestId") Integer contestId,
                                          @RequestParam("hackerId") Long hackerId,
                                          @RequestParam("targetUserId") Long targetUserId,
                                          @RequestParam("problemId") Integer problemId,
                                          @RequestParam("score") Integer score);
}
