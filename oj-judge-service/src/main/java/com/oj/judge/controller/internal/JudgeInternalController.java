package com.oj.judge.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oj.common.result.Result;
import com.oj.judge.entity.Submission;
import com.oj.judge.mapper.SubmissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 判题服务内部接口（供其他微服务通过 Feign 调用）
 */
@Slf4j
@RestController
@RequestMapping("/internal/judge")
public class JudgeInternalController {

    @Autowired
    private SubmissionMapper submissionMapper;

    /**
     * 统计提交数量（支持多种筛选条件）
     */
    @GetMapping("/submission/count")
    public Result<Long> countSubmissions(@RequestParam Map<String, Object> params) {
        log.info("内部调用-统计提交数量: {}", params);
        Integer count = submissionMapper.countSubmission(params);
        return Result.success(count != null ? count.longValue() : 0L);
    }

    /**
     * 获取用户提交数量
     */
    @GetMapping("/user/{userId}/submission-count")
    public Result<Long> getUserSubmissionCount(@PathVariable("userId") Long userId) {
        log.info("内部调用-获取用户提交数量: userId={}", userId);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getUserId, userId);
        long count = submissionMapper.selectCount(wrapper);
        return Result.success(count);
    }

    /**
     * 按日期范围统计提交数量
     */
    @GetMapping("/submission/count-by-date")
    public Result<Integer> countSubmissionsByDate(
            @RequestParam(value = "begin", required = false) String begin,
            @RequestParam(value = "end", required = false) String end) {
        log.info("内部调用-按日期统计提交数量: begin={}, end={}", begin, end);
        Map<String, Object> params = new HashMap<>();
        if (begin != null) params.put("begin", begin);
        if (end != null) params.put("end", end);
        Integer count = submissionMapper.countSubmission(params);
        return Result.success(count != null ? count : 0);
    }

    /**
     * 按日期范围和状态统计提交数量
     */
    @GetMapping("/submission/count-by-date-and-status")
    public Result<Integer> countSubmissionsByDateAndStatus(
            @RequestParam(value = "begin", required = false) String begin,
            @RequestParam(value = "end", required = false) String end,
            @RequestParam("status") String status) {
        log.info("内部调用-按日期和状态统计提交数量: begin={}, end={}, status={}", begin, end, status);
        Map<String, Object> params = new HashMap<>();
        if (begin != null) params.put("begin", begin);
        if (end != null) params.put("end", end);
        if (status != null) params.put("status", status);
        Integer count = submissionMapper.countSubmission(params);
        return Result.success(count != null ? count : 0);
    }
}
