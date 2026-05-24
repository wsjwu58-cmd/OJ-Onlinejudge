package com.oj.api.fallback;

import com.oj.api.ProblemClient;
import com.oj.api.dto.ProblemAcceptanceFeignDTO;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.api.dto.TestCaseFeignDTO;
import com.oj.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ProblemClientFallbackFactory implements FallbackFactory<ProblemClient> {
    @Override
    public ProblemClient create(Throwable cause) {
        log.error("题目服务调用失败: {}", cause.getMessage());
        return new ProblemClient() {
            @Override
            public Result<ProblemFeignDTO> getProblemById(Integer id) {
                return Result.error("题目服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<List<ProblemFeignDTO>> getProblemsByIds(List<Integer> ids) {
                return Result.error("题目服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<List<TestCaseFeignDTO>> getTestCasesByProblemId(Integer problemId) {
                return Result.error("题目服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Void> updateProblemAcceptance(Integer id) {
                return Result.error("题目服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Long> countProblems() {
                return Result.error("题目服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Integer> countProblemsByStatus(Integer status) {
                return Result.error("题目服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<Integer> countProblemsByDate(String begin, String end) {
                return Result.error("题目服务调用失败: " + cause.getMessage());
            }
            @Override
            public Result<List<ProblemAcceptanceFeignDTO>> selectAcceptanceTop10() {
                return Result.error("题目服务调用失败: " + cause.getMessage());
            }
        };
    }
}
