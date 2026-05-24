package com.oj.api;

import com.oj.api.dto.ProblemAcceptanceFeignDTO;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.api.dto.TestCaseFeignDTO;
import com.oj.api.fallback.ProblemClientFallbackFactory;
import com.oj.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "oj-problem-service", fallbackFactory = ProblemClientFallbackFactory.class)
public interface ProblemClient {
    @GetMapping("/internal/problem/{id}")
    Result<ProblemFeignDTO> getProblemById(@PathVariable("id") Integer id);

    @PostMapping("/internal/problem/batch")
    Result<List<ProblemFeignDTO>> getProblemsByIds(@RequestBody List<Integer> ids);

    @GetMapping("/internal/problem/{problemId}/test-cases")
    Result<List<TestCaseFeignDTO>> getTestCasesByProblemId(@PathVariable("problemId") Integer problemId);

    @PutMapping("/internal/problem/{id}/acceptance")
    Result<Void> updateProblemAcceptance(@PathVariable("id") Integer id);

    @GetMapping("/internal/problem/count")
    Result<Long> countProblems();

    @GetMapping("/internal/problem/count-by-status")
    Result<Integer> countProblemsByStatus(@RequestParam("status") Integer status);

    @GetMapping("/internal/problem/count-by-date")
    Result<Integer> countProblemsByDate(@RequestParam(value = "begin", required = false) String begin,
                                        @RequestParam(value = "end", required = false) String end);

    @GetMapping("/internal/problem/acceptance-top10")
    Result<List<ProblemAcceptanceFeignDTO>> selectAcceptanceTop10();
}
