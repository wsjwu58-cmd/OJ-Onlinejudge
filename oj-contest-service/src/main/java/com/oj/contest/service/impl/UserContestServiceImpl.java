package com.oj.contest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.api.ProblemClient;
import com.oj.api.UserClient;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.api.dto.UserFeignDTO;
import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.contest.dto.ContestQueryDTO;
import com.oj.contest.entity.Contest;
import com.oj.contest.entity.ContestParticipant;
import com.oj.contest.entity.ContestProblem;
import com.oj.contest.mapper.ContestMapper;
import com.oj.contest.mapper.ContestParticipantMapper;
import com.oj.contest.mapper.ContestProblemMapper;
import com.oj.contest.mapper.HackRecordMapper;
import com.oj.contest.service.UserContestService;
import com.oj.contest.vo.ContestRankVO;
import com.oj.contest.vo.ContestVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserContestServiceImpl implements UserContestService {

    private static final String RANK_KEY_PREFIX = "contest:rank:";
    private static final String CONTEST_AC_PREFIX = "contest:ac:";
    private static final String LOCK_KEY_PREFIX = "contest:lock:";
    private static final String HACK_DUP_PREFIX = "contest:hack:";

    @Autowired
    private ContestMapper contestMapper;

    @Autowired
    private ContestProblemMapper contestProblemMapper;

    @Autowired
    private ContestParticipantMapper contestParticipantMapper;

    @Autowired
    private HackRecordMapper hackRecordMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private ProblemClient problemClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DefaultRedisScript<Long> hackRankUpdateScript;

    @Override
    public PageResult pageContest(ContestQueryDTO contestQueryDTO) {
        Page<ContestVO> page = contestQueryDTO.toPageDefaultSortByCreateTime("created_at");
        QueryWrapper<Contest> wrapper = new QueryWrapper<>();
        String status = contestQueryDTO.getStatus();
        String title = contestQueryDTO.getTitle();
        wrapper.eq(status != null && !status.isEmpty(), "c.status", status)
                .like(title != null && !title.isEmpty(), "c.title", title);
        Page<ContestVO> result = contestMapper.selectPage(page, wrapper);

        // 填充创建者名称 + 参赛人数
        fillCreatedName(result.getRecords());
        for (ContestVO vo : result.getRecords()) {
            vo.setParticipantCount(contestParticipantMapper.countByContest(vo.getId()));
        }

        return new PageResult(result.getTotal(), result.getRecords());
    }

    @Override
    public ContestVO getContestDetail(Long contestId, Long userId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new RuntimeException("比赛不存在");
        }

        ContestVO vo = new ContestVO();
        BeanUtils.copyProperties(contest, vo);

        // 填充创建者名称
        if (contest.getCreatedBy() != null) {
            Result<UserFeignDTO> userResult = userClient.getUserById(contest.getCreatedBy());
            if (userResult != null && userResult.getData() != null) {
                vo.setCreatedName(userResult.getData().getUsername());
            }
        }

        vo.setParticipantCount(contestParticipantMapper.countByContest(contest.getId()));
        if (userId != null) {
            vo.setRegistered(contestParticipantMapper.countByContestAndUser(contest.getId(), userId) > 0);
        } else {
            vo.setRegistered(false);
        }

        return vo;
    }

    @Override
    public void joinContest(Long contestId, Long userId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new RuntimeException("比赛不存在");
        }
        if ("Ended".equals(contest.getStatus())) {
            throw new RuntimeException("比赛已结束，无法报名");
        }
        if (contestParticipantMapper.countByContestAndUser(contest.getId(), userId) > 0) {
            throw new RuntimeException("您已报名过该比赛");
        }

        ContestParticipant participant = ContestParticipant.builder()
                .contestId(contest.getId())
                .userId(userId)
                .score(0)
                .solvedCount(0)
                .registeredAt(LocalDateTime.now())
                .build();
        contestParticipantMapper.insert(participant);

        String rankKey = RANK_KEY_PREFIX + contestId;
        stringRedisTemplate.opsForZSet().addIfAbsent(rankKey, String.valueOf(userId), 0);

        log.info("用户 {} 成功报名比赛 {}", userId, contestId);
    }

    @Override
    public ContestVO getContestProblems(Long contestId, Long userId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new RuntimeException("比赛不存在");
        }

        ContestVO vo = new ContestVO();
        BeanUtils.copyProperties(contest, vo);

        if ("Upcoming".equals(contest.getStatus())) {
            vo.setProblemList(Collections.emptyList());
            return vo;
        }

        // 查询竞赛关联题目
        List<Long> problemIds = contestProblemMapper.selectListAll(contestId);
        if (!problemIds.isEmpty()) {
            Result<List<ProblemFeignDTO>> pResult = problemClient.getProblemsByIds(
                    problemIds.stream().map(Long::intValue).toList());
            if (pResult != null && pResult.getData() != null) {
                // 查询每道题在比赛中的分值
                LambdaQueryWrapper<ContestProblem> cpWrapper = new LambdaQueryWrapper<>();
                cpWrapper.eq(ContestProblem::getContestId, contestId);
                List<ContestProblem> cpList = contestProblemMapper.selectList(cpWrapper);
                Map<Integer, Integer> scoreMap = cpList.stream()
                        .collect(Collectors.toMap(ContestProblem::getProblemId,
                                cp -> cp.getScore() != null ? cp.getScore() : 100));

                List<ContestVO.ContestProblemVO> problemVOList = pResult.getData().stream().map(p -> {
                    ContestVO.ContestProblemVO pvo = new ContestVO.ContestProblemVO();
                    pvo.setId(p.getId());
                    pvo.setTitle(p.getTitle());
                    pvo.setDifficulty(p.getDifficulty());
                    pvo.setAcceptance(p.getAcceptance());
                    pvo.setStatus(p.getStatus());
                    pvo.setScore(scoreMap.getOrDefault(p.getId(), 100));
                    return pvo;
                }).toList();
                vo.setProblemList(problemVOList);
            }
        }

        vo.setParticipantCount(contestParticipantMapper.countByContest(contest.getId()));
        return vo;
    }

    @Override
    public List<ContestRankVO> getContestRank(Long contestId) {
        String rankKey = RANK_KEY_PREFIX + contestId;
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(rankKey, 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = tuples.stream()
                .map(t -> Long.parseLong(t.getValue()))
                .collect(Collectors.toList());
        Map<Long, String> usernameMap = getUsernameMap(userIds);

        List<ContestRankVO> rankList = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long uid = Long.parseLong(tuple.getValue());
            int score = tuple.getScore() != null ? tuple.getScore().intValue() : 0;

            String solvedKey = "contest:" + contestId + ":user:" + uid + ":solved_count";
            String solvedStr = stringRedisTemplate.opsForValue().get(solvedKey);
            int solvedCount = solvedStr != null ? Integer.parseInt(solvedStr) : 0;

            rankList.add(ContestRankVO.builder()
                    .rank(rank++)
                    .userId(uid)
                    .username(usernameMap.getOrDefault(uid, "用户" + uid))
                    .score(score)
                    .solvedCount(solvedCount)
                    .build());
        }

        return rankList;
    }

    @Override
    public void updateRankOnAccepted(Integer contestId, Long userId, Integer problemId, int problemScore) {
        String acKey = CONTEST_AC_PREFIX + contestId + ":" + userId + ":" + problemId;
        Boolean alreadyAc = stringRedisTemplate.hasKey(acKey);
        if (Boolean.TRUE.equals(alreadyAc)) {
            log.info("比赛 {} 用户 {} 题目 {} 已AC过，跳过排行榜更新", contestId, userId, problemId);
            return;
        }

        stringRedisTemplate.opsForValue().set(acKey, "1", java.time.Duration.ofDays(7));

        String rankKey = RANK_KEY_PREFIX + contestId;
        stringRedisTemplate.opsForZSet().incrementScore(rankKey, String.valueOf(userId), problemScore);

        log.info("排行榜已更新: contest={}, user={}, problem={}, +{}分",
                contestId, userId, problemId, problemScore);
    }

    @Override
    public void persistRankToDb(Integer contestId) {
        List<ContestRankVO> rankList = getContestRank(Long.valueOf(contestId));

        for (ContestRankVO rankVO : rankList) {
            LambdaQueryWrapper<ContestParticipant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ContestParticipant::getContestId, contestId)
                    .eq(ContestParticipant::getUserId, rankVO.getUserId());
            ContestParticipant participant = contestParticipantMapper.selectOne(wrapper);

            if (participant != null) {
                participant.setScore(rankVO.getScore());
                participant.setSolvedCount(rankVO.getSolvedCount());
                contestParticipantMapper.updateById(participant);
            }
        }

        log.info("比赛 {} 排行榜已持久化到数据库，共 {} 条", contestId, rankList.size());
    }

    @Override
    public void lockProblem(Long contestId, Long userId, Integer problemId) {
        String acKey = CONTEST_AC_PREFIX + contestId + ":" + userId + ":" + problemId;
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(acKey))) {
            throw new RuntimeException("请先AC该题目");
        }

        Contest contest = contestMapper.selectById(contestId.intValue());
        if (contest == null) {
            throw new RuntimeException("比赛不存在");
        }

        String lockKey = LOCK_KEY_PREFIX + contestId + ":" + userId + ":" + problemId;
        Duration ttl = Duration.between(LocalDateTime.now(), contest.getEndTime());
        if (ttl.isNegative() || ttl.isZero()) {
            throw new RuntimeException("比赛已结束");
        }
        stringRedisTemplate.opsForValue().set(lockKey, "1", ttl);
        log.info("用户 {} 锁定比赛 {} 题目 {}", userId, contestId, problemId);
    }

    @Override
    public void unlockProblem(Long contestId, Long userId, Integer problemId) {
        String lockKey = LOCK_KEY_PREFIX + contestId + ":" + userId + ":" + problemId;
        stringRedisTemplate.delete(lockKey);
        log.info("用户 {} 解锁比赛 {} 题目 {}", userId, contestId, problemId);
    }

    @Override
    public boolean isProblemLocked(Long contestId, Long userId, Integer problemId) {
        String lockKey = LOCK_KEY_PREFIX + contestId + ":" + userId + ":" + problemId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey));
    }

    @Override
    public List<Long> getAcSubmissionIds(Integer contestId, Integer problemId, Long excludeUserId) {
        String acKeyPrefix = CONTEST_AC_PREFIX + contestId + ":";
        String pattern = acKeyPrefix + "*:" + problemId;
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = new HashSet<>();
        for (String key : keys) {
            String[] parts = key.split(":");
            if (parts.length >= 4) {
                Long uid = Long.parseLong(parts[2]);
                if (!uid.equals(excludeUserId)) {
                    userIds.add(uid);
                }
            }
        }

        // 查询这些用户的AC提交ID（取最新的AC提交）
        LambdaQueryWrapper<ContestParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContestParticipant::getContestId, contestId);
        List<ContestParticipant> participants = contestParticipantMapper.selectList(wrapper);
        Set<Long> participantUserIds = participants.stream()
                .map(ContestParticipant::getUserId)
                .collect(Collectors.toSet());

        List<Long> result = new ArrayList<>();
        for (Long uid : userIds) {
            if (participantUserIds.contains(uid)) {
                result.add(uid);
            }
        }
        return result;
    }

    @Override
    public int getProblemScoreInContest(Integer contestId, Integer problemId) {
        LambdaQueryWrapper<ContestProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, problemId);
        ContestProblem cp = contestProblemMapper.selectOne(wrapper);
        return cp != null && cp.getScore() != null ? cp.getScore() : 100;
    }

    @Override
    public void updateRankOnHackSuccess(Integer contestId, Long hackerId, Long targetUserId, Integer problemId, int score) {
        List<String> keys = List.of(
                RANK_KEY_PREFIX + contestId,
                "contest:" + contestId + ":user:" + targetUserId + ":solved_count",
                CONTEST_AC_PREFIX + contestId + ":" + targetUserId + ":" + problemId,
                HACK_DUP_PREFIX + contestId + ":" + hackerId + ":" + targetUserId + ":" + problemId
        );
        stringRedisTemplate.execute(hackRankUpdateScript, keys,
                String.valueOf(hackerId), String.valueOf(targetUserId), String.valueOf(score));
        log.info("Hack排行更新: contest={}, hacker={}, target={}, problem={}, score={}",
                contestId, hackerId, targetUserId, problemId, score);
    }

    private Map<Long, String> getUsernameMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Result<List<UserFeignDTO>> result = userClient.getUsersByIds(userIds);
        if (result != null && result.getData() != null) {
            return result.getData().stream()
                    .collect(Collectors.toMap(UserFeignDTO::getId, UserFeignDTO::getUsername, (a, b) -> a));
        }
        return Collections.emptyMap();
    }

    private void fillCreatedName(List<ContestVO> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> createdByList = records.stream()
                .map(ContestVO::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (createdByList.isEmpty()) return;

        Result<List<UserFeignDTO>> result = userClient.getUsersByIds(createdByList);
        if (result != null && result.getData() != null) {
            Map<Long, String> nameMap = result.getData().stream()
                    .collect(Collectors.toMap(UserFeignDTO::getId, UserFeignDTO::getUsername, (a, b) -> a));
            records.forEach(vo -> {
                if (vo.getCreatedBy() != null) {
                    vo.setCreatedName(nameMap.get(vo.getCreatedBy()));
                }
            });
        }
    }
}
