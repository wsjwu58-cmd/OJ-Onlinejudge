package com.oj.controller.User;

import com.oj.context.BaseContext;
import com.oj.dto.JudgeRunDTO;
import com.oj.dto.JudgeSubmitDTO;
import com.oj.result.Result;
import com.oj.service.JudgeService;
import com.oj.vo.JudgeResultVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 流程：AI语法检测 → Lua设置pending → MQ异步判题 → 立即返回Pending
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
