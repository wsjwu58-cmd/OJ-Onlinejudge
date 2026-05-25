package com.oj.judge.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oj.api.dto.SubmissionFeignDTO;
import com.oj.common.result.Result;
import com.oj.judge.entity.Submission;
import com.oj.judge.mapper.SubmissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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

    @GetMapping("/submission/{id}/code")
    public Result<String> getSubmissionCodeById(@PathVariable Long id) {
        log.info("内部调用-获取提交代码: id={}", id);
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            return Result.error("提交记录不存在");
        }
        return Result.success(submission.getCode());
    }

    @GetMapping("/submission/ac-list")
    public Result<List<SubmissionFeignDTO>> getAcSubmissions(
            @RequestParam("contestId") Integer contestId,
            @RequestParam("problemId") Integer problemId) {
        log.info("内部调用-获取AC提交列表: contestId={}, problemId={}", contestId, problemId);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getContestId, contestId)
                .eq(Submission::getProblemId, problemId)
                .eq(Submission::getStatus, "Accepted")
                .orderByDesc(Submission::getSubmitTime);
        List<Submission> submissions = submissionMapper.selectList(wrapper);
        List<SubmissionFeignDTO> dtoList = submissions.stream().map(s -> {
            SubmissionFeignDTO dto = new SubmissionFeignDTO();
            BeanUtils.copyProperties(s, dto);
            return dto;
        }).toList();
        return Result.success(dtoList);
    }
}
