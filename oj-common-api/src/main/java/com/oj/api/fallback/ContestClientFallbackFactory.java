package com.oj.api.fallback;

import com.oj.api.ContestClient;
import com.oj.api.dto.ContestProblemFeignDTO;
import com.oj.api.dto.WorkspaceActivityFeignDTO;
import com.oj.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ContestClientFallbackFactory implements FallbackFactory<ContestClient> {
    @Override
    public ContestClient create(Throwable cause) {
        log.error("竞赛服务调用失败: {}", cause.getMessage());
        return new ContestClient() {
            @Override
            public Result<ContestProblemFeignDTO> getContestProblem(Integer contestId, Integer problemId) {
                return Result.error("竞赛服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Void> updateRankOnAccepted(Integer contestId, Long userId, Integer problemId, Integer score) {
                return Result.error("竞赛服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Void> recordWorkspaceActivity(WorkspaceActivityFeignDTO dto) {
                return Result.error("竞赛服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Long> countContestByProblemId(Integer problemId) {
                return Result.error("竞赛服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<List<Long>> countContestByProblemIds(List<Integer> problemIds) {
                return Result.error("竞赛服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Void> updateRankOnHackSuccess(Integer contestId, Long hackerId, Long targetUserId, Integer problemId, Integer score) {
                return Result.error("竞赛服务调用失败: " + cause.getMessage());
            }
        };
    }
}
