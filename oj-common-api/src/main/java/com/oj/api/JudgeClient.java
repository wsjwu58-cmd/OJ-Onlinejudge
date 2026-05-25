package com.oj.api;

import com.oj.api.dto.SubmissionFeignDTO;
import com.oj.api.fallback.JudgeClientFallbackFactory;
import com.oj.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@FeignClient(name = "oj-judge-service", fallbackFactory = JudgeClientFallbackFactory.class)
public interface JudgeClient {
    @GetMapping("/internal/judge/submission/count")
    Result<Long> countSubmissions(@RequestParam Map<String, Object> params);

    @GetMapping("/internal/judge/user/{userId}/submission-count")
    Result<Long> getUserSubmissionCount(@PathVariable("userId") Long userId);

    @GetMapping("/internal/judge/submission/count-by-date")
    Result<Integer> countSubmissionsByDate(@RequestParam(value = "begin", required = false) String begin,
                                           @RequestParam(value = "end", required = false) String end);

    @GetMapping("/internal/judge/submission/count-by-date-and-status")
    Result<Integer> countSubmissionsByDateAndStatus(@RequestParam(value = "begin", required = false) String begin,
                                                    @RequestParam(value = "end", required = false) String end,
                                                    @RequestParam("status") String status);

    @GetMapping("/internal/judge/submission/{id}/code")
    Result<String> getSubmissionCodeById(@PathVariable("id") Long id);

    @GetMapping("/internal/judge/submission/ac-list")
    Result<List<SubmissionFeignDTO>> getAcSubmissions(@RequestParam("contestId") Integer contestId,
                                                       @RequestParam("problemId") Integer problemId);
}
