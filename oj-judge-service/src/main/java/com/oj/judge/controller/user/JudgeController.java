package com.oj.judge.controller.user;

import com.oj.common.context.BaseContext;
import com.oj.common.result.Result;
import com.oj.judge.dto.JudgeRunDTO;
import com.oj.judge.dto.JudgeSubmitDTO;
import com.oj.judge.service.JudgeService;
import com.oj.judge.vo.JudgeResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 判题接口（用户端）
 * 提交判题已改为异步：Redis+Lua+RocketMQ
 * 提交后立即返回 Pending，最终结果通过 WebSocket 推送
 */
@Slf4j
@RestController
@RequestMapping("/user/judge")
@Tag(name = "用户端-判题接口")
public class JudgeController {

    @Autowired
    private JudgeService judgeService;

    /**
     * 提交代码判题（异步）
     * 流程：恶意代码检测 → Lua设置pending → MQ异步判题 → 立即返回Pending
     * 最终结果通过 WebSocket 推送到前端
     */
    @PostMapping("/submit")
    @Operation(summary = "提交代码判题（异步）")
    public Result<JudgeResultVO> submit(@Valid @RequestBody JudgeSubmitDTO dto) {
        log.info("提交判题: problemId={}, language={}", dto.getProblemId(), dto.getLanguage());
        Long userId = BaseContext.getCurrentId();
        JudgeResultVO result = judgeService.submit(dto, userId);
        return Result.success(result);
    }

    /**
     * 运行代码（同步，不入库，只跑用户输入或示例测试用例）
     */
    @PostMapping("/run")
    @Operation(summary = "运行代码")
    public Result<JudgeResultVO> run(@Valid @RequestBody JudgeRunDTO dto) {
        log.info("运行代码: problemId={}, language={}", dto.getProblemId(), dto.getLanguage());
        JudgeResultVO result = judgeService.run(dto);
        return Result.success(result);
    }
}
