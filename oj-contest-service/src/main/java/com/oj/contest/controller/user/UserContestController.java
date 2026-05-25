package com.oj.contest.controller.user;

import com.oj.api.JudgeClient;
import com.oj.api.ProblemClient;
import com.oj.api.dto.HackAssetsDTO;
import com.oj.common.constant.MqConstant;
import com.oj.common.context.BaseContext;
import com.oj.common.mq.dto.HackTaskMessage;
import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.contest.dto.ContestQueryDTO;
import com.oj.contest.dto.HackSubmitDTO;
import com.oj.contest.entity.HackRecord;
import com.oj.contest.mapper.HackRecordMapper;
import com.oj.contest.service.UserContestService;
import com.oj.contest.vo.ContestRankVO;
import com.oj.contest.vo.ContestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user/contest")
@Slf4j
@Tag(name = "用户端-竞赛")
public class UserContestController {

    @Autowired
    private UserContestService userContestService;

    @Autowired
    private HackRecordMapper hackRecordMapper;

    @Autowired
    private ProblemClient problemClient;

    @Autowired
    private JudgeClient judgeClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @GetMapping("/page")
    @Operation(summary = "分页查询竞赛列表")
    public Result<PageResult> page(ContestQueryDTO contestQueryDTO) {
        return Result.success(userContestService.pageContest(contestQueryDTO));
    }

    @GetMapping("/{contestId}")
    @Operation(summary = "查询竞赛详情")
    public Result<ContestVO> detail(@PathVariable Long contestId) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(userContestService.getContestDetail(contestId, userId));
    }

    @PostMapping("/{contestId}/join")
    @Operation(summary = "报名竞赛")
    public Result join(@PathVariable Long contestId) {
        Long userId = BaseContext.getCurrentId();
        userContestService.joinContest(contestId, userId);
        return Result.success();
    }

    @GetMapping("/{contestId}/problems")
    @Operation(summary = "获取竞赛题目列表")
    public Result<ContestVO> problems(@PathVariable Long contestId) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(userContestService.getContestProblems(contestId, userId));
    }

    @GetMapping("/{contestId}/rank")
    @Operation(summary = "获取竞赛排行榜")
    public Result<List<ContestRankVO>> rank(@PathVariable Long contestId) {
        return Result.success(userContestService.getContestRank(contestId));
    }

    @PostMapping("/{contestId}/problem/{problemId}/lock")
    @Operation(summary = "锁定题目")
    public Result<Void> lockProblem(@PathVariable Long contestId, @PathVariable Integer problemId) {
        Long userId = BaseContext.getCurrentId();
        userContestService.lockProblem(contestId, userId, problemId);
        return Result.success();
    }

    @GetMapping("/{contestId}/problem/{problemId}/hack-status")
    @Operation(summary = "查询AC和锁定状态")
    public Result<Map<String, Object>> hackStatus(@PathVariable Long contestId, @PathVariable Integer problemId) {
        Long userId = BaseContext.getCurrentId();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("locked", userContestService.isProblemLocked(contestId, userId, problemId));

        String acKey = "contest:ac:" + contestId + ":" + userId + ":" + problemId;
        result.put("accepted", Boolean.TRUE.equals(stringRedisTemplate.hasKey(acKey)));
        return Result.success(result);
    }

    @PostMapping("/{contestId}/problem/{problemId}/unlock")
    @Operation(summary = "解锁题目")
    public Result<Void> unlockProblem(@PathVariable Long contestId, @PathVariable Integer problemId) {
        Long userId = BaseContext.getCurrentId();
        userContestService.unlockProblem(contestId, userId, problemId);
        return Result.success();
    }

    @GetMapping("/{contestId}/problem/{problemId}/ac-submissions")
    @Operation(summary = "获取AC提交列表")
    public Result<List<Map<String, Object>>> acSubmissions(@PathVariable Long contestId, @PathVariable Integer problemId) {
        Long userId = BaseContext.getCurrentId();
        if (!userContestService.isProblemLocked(contestId, userId, problemId)) {
            return Result.error("请先锁定该题目");
        }

        // 从judge-service获取该比赛该题目的所有AC提交
        Result<List<com.oj.api.dto.SubmissionFeignDTO>> result = judgeClient.getAcSubmissions(
                contestId.intValue(), problemId);
        if (result == null || result.getData() == null) {
            return Result.success(java.util.Collections.emptyList());
        }

        List<Map<String, Object>> list = result.getData().stream()
                .filter(s -> !s.getUserId().equals(userId))
                .map(s -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("id", s.getId());
                    map.put("userId", s.getUserId());
                    map.put("code", s.getCode());
                    map.put("language", s.getLanguage());
                    map.put("runtimeMs", s.getRuntimeMs());
                    map.put("memoryKb", s.getMemoryKb());
                    map.put("submitTime", s.getSubmitTime());
                    return map;
                }).toList();
        return Result.success(list);
    }

    @PostMapping("/{contestId}/hack")
    @Operation(summary = "提交Hack")
    public Result<Long> submitHack(@PathVariable Long contestId, @RequestBody HackSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();

        // 1. 检查是否已锁定该题
        if (!userContestService.isProblemLocked(contestId, userId, dto.getProblemId())) {
            return Result.error("请先锁定该题目");
        }

        // 2. 防止重复 Hack 同一人的同一题 (Redis去重)
        String dupKey = "contest:hack:" + contestId + ":" + userId + ":" + dto.getTargetUserId() + ":" + dto.getProblemId();
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(dupKey))) {
            return Result.error("已对该选手的此题发起过 Hack");
        }

        // 3. 创建 HackRecord (状态 Pending)
        HackRecord record = HackRecord.builder()
                .contestId(contestId.intValue())
                .problemId(dto.getProblemId())
                .hackerId(userId)
                .targetUserId(dto.getTargetUserId())
                .targetSubmissionId(dto.getTargetSubmissionId())
                .hackInput(dto.getHackInput())
                .status("Pending")
                .build();
        hackRecordMapper.insert(record);

        // 4. 获取题目Hack资源
        Result<HackAssetsDTO> assetsResult = problemClient.getHackAssets(dto.getProblemId());
        if (assetsResult == null || assetsResult.getData() == null) {
            record.setStatus("SystemError");
            record.setErrorInfo("获取题目Hack资源失败");
            hackRecordMapper.updateById(record);
            return Result.error("获取题目Hack资源失败");
        }
        HackAssetsDTO assets = assetsResult.getData();

        // 5. 发送 MQ 任务（只传ID，代码由Consumer按需从DB拉取）
        HackTaskMessage taskMsg = HackTaskMessage.builder()
                .hackId(record.getId())
                .contestId(contestId.intValue())
                .problemId(dto.getProblemId())
                .hackerId(userId)
                .targetUserId(dto.getTargetUserId())
                .targetSubmissionId(dto.getTargetSubmissionId())
                .validatorPath(assets.getValidatorPath())
                .validatorExePath(assets.getValidatorExePath())
                .validatorSrcHash(assets.getValidatorSrcHash())
                .referencePath(assets.getReferencePath())
                .referenceLanguage(assets.getReferenceLanguage())
                .hackInput(dto.getHackInput())
                .timeLimitMs(assets.getTimeLimitMs())
                .memoryLimitMb(assets.getMemoryLimitMb())
                .build();
        rocketMQTemplate.convertAndSend(MqConstant.HACK_TASK_TOPIC, taskMsg);

        log.info("Hack任务已发送: hackId={}, contest={}, hacker={}, target={}",
                record.getId(), contestId, userId, dto.getTargetUserId());
        return Result.success(record.getId());
    }

    @GetMapping("/{contestId}/hack/{hackId}/result")
    @Operation(summary = "查询Hack结果")
    public Result<HackRecord> hackResult(@PathVariable Long contestId, @PathVariable Long hackId) {
        HackRecord record = hackRecordMapper.selectById(hackId);
        if (record == null) {
            return Result.error("Hack记录不存在");
        }
        return Result.success(record);
    }

    @GetMapping("/{contestId}/hack/records")
    @Operation(summary = "查询Hack记录列表")
    public Result<List<HackRecord>> hackRecords(@PathVariable Long contestId) {
        Long userId = BaseContext.getCurrentId();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HackRecord> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(HackRecord::getContestId, contestId.intValue())
                .and(w -> w.eq(HackRecord::getHackerId, userId).or().eq(HackRecord::getTargetUserId, userId))
                .orderByDesc(HackRecord::getCreatedAt);
        return Result.success(hackRecordMapper.selectList(wrapper));
    }
}
