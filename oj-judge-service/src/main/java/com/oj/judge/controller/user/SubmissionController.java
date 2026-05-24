package com.oj.judge.controller.user;

import com.oj.common.result.Result;
import com.oj.judge.service.SubmissionService;
import com.oj.judge.vo.JudgeResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/submission")
@Slf4j
@Tag(name = "用户端-提交记录")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    @GetMapping
    @Operation(summary = "获取当前用户的提交记录")
    public Result<List<JudgeResultVO>> selectSubmission(Long problemId) {
        log.info("获取当前用户提交记录:{}", problemId);
        List<JudgeResultVO> judgeResultVO = submissionService.getSubmission(problemId);
        return Result.success(judgeResultVO);
    }
}
