package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.dto.ContestQueryDTO;
import com.oj.entity.Contest;
import com.oj.entity.ContestParticipant;
import com.oj.entity.ContestProblem;
import com.oj.entity.Problem;
import com.oj.mapper.ContestMapper;
import com.oj.mapper.ContestParticipantMapper;
import com.oj.mapper.ContestProblemMapper;
import com.oj.mapper.UserMapper;
import com.oj.entity.User;
import com.oj.result.PageResult;
import com.oj.service.UserContestService;
import com.oj.vo.ContestRankVO;
import com.oj.vo.ContestVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户端比赛 Service 实现
 * 排行榜使用 Redis ZSet：key = contest:rank:{contestId}，member = userId，score = 比赛得分
 */
@Slf4j
@Service
public class UserContestServiceImpl implements UserContestService {

    /** Redis 排行榜 key 前缀 */
    private static final String RANK_KEY_PREFIX = "contest:rank:";
    /** Redis 比赛某用户某题是否已AC：contest:{contestId}:user:{userId}:problem:{problemId}:ac */
    private static final String CONTEST_AC_PREFIX = "contest:ac:";

    @Autowired
    private ContestMapper contestMapper;

    @Autowired
    private ContestProblemMapper contestProblemMapper;

    @Autowired
    private ContestParticipantMapper contestParticipantMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ======================== 比赛列表 ========================

    @Override
    public PageResult pageContest(ContestQueryDTO contestQueryDTO) {
        Page<ContestVO> page = contestQueryDTO.ToPageDefaultSortByCreateTime("created_at");
        // 因为 ContestMapper.xml 的 selectPage 使用了 LEFT JOIN user，
        // 而 user 表也有 status 字段，LambdaQueryWrapper 生成的 WHERE status = ?
        // 会导致 "Column 'status' in where clause is ambiguous"，
        // 所以必须用 QueryWrapper 手动指定表别名 c.status / c.title
        QueryWrapper<Contest> wrapper = new QueryWrapper<>();

        String status = contestQueryDTO.getStatus();
        String title = contestQueryDTO.getTitle();
        wrapper.eq(status != null && !status.isEmpty(), "c.status", status)
                .like(title != null && !title.isEmpty(), "c.title", title);

        Page<ContestVO> result = contestMapper.selectPage(page,wrapper);

        // 为每条记录填充参赛人数
        for (ContestVO vo : result.getRecords()) {
            int count = contestParticipantMapper.countByContest(vo.getId());
            vo.setParticipantCount(count);
        }

        return new PageResult(result.getTotal(), result.getRecords());

    }

    // ======================== 比赛详情 ========================

    @Override
    public ContestVO getContestDetail(Long contestId, Long userId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new RuntimeException("比赛不存在");
        }

        ContestVO vo = new ContestVO();
        BeanUtils.copyProperties(contest, vo);

        // 参赛人数
        vo.setParticipantCount(contestParticipantMapper.countByContest(contest.getId()));

        // 当前用户是否已报名
        if (userId != null) {
            vo.setRegistered(contestParticipantMapper.countByContestAndUser(contest.getId(), userId) > 0);
        } else {
            vo.setRegistered(false);
        }

        return vo;
    }

    // ======================== 报名比赛 ========================

    @Override
    public void joinContest(Long contestId, Long userId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new RuntimeException("比赛不存在");
        }
        if ("Ended".equals(contest.getStatus())) {
            throw new RuntimeException("比赛已结束，无法报名");
        }

        // 检查是否已报名
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

        // 同时在 Redis 排行榜中初始化该用户分数为 0
        String rankKey = RANK_KEY_PREFIX + contestId;
        stringRedisTemplate.opsForZSet().addIfAbsent(rankKey, String.valueOf(userId), 0);

        log.info("用户 {} 成功报名比赛 {}", userId, contestId);
    }

    // ======================== 比赛题目列表 ========================

    @Override
    public ContestVO getContestProblems(Long contestId, Long userId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new RuntimeException("比赛不存在");
        }

        // 比赛未开始，不能查看题目
        if ("Upcoming".equals(contest.getStatus())) {
            ContestVO vo = new ContestVO();
            BeanUtils.copyProperties(contest, vo);
            vo.setProblemList(Collections.emptyList());
            return vo;
        }

        // 查询题目列表（复用已有的 mapper 方法）
        List<Problem> problemList = contestMapper.SelectById(Long.valueOf(contestId));

        // 查询每道题在比赛中的分值
        LambdaQueryWrapper<ContestProblem> cpWrapper = new LambdaQueryWrapper<>();
        cpWrapper.eq(ContestProblem::getContestId, contestId);
        List<ContestProblem> cpList = contestProblemMapper.selectList(cpWrapper);
        Map<Integer, Integer> scoreMap = cpList.stream()
                .collect(Collectors.toMap(ContestProblem::getProblemId, cp -> cp.getScore() != null ? cp.getScore() : 100));

        // 设置每道题的分值
        for (Problem p : problemList) {
            p.setScore(scoreMap.getOrDefault(p.getId(), 100));
        }

        ContestVO vo = new ContestVO();
        BeanUtils.copyProperties(contest, vo);
        vo.setProblemList(problemList);
        vo.setParticipantCount(contestParticipantMapper.countByContest(contest.getId()));

        return vo;
    }

    // ======================== Redis ZSet 排行榜 ========================

    @Override
    public List<ContestRankVO> getContestRank(Long contestId) {
        String rankKey = RANK_KEY_PREFIX + contestId;

        // 按分数从高到低取出所有成员（reverseRangeWithScores：分数降序）
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(rankKey, 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有 userId 批量查用户名
        List<Long> userIds = tuples.stream()
                .map(t -> Long.parseLong(t.getValue()))
                .collect(Collectors.toList());
        Map<Long, String> usernameMap = getUsernameMap(userIds);

        // 构建排行榜 VO
        List<ContestRankVO> rankList = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long uid = Long.parseLong(tuple.getValue());
            int score = tuple.getScore() != null ? tuple.getScore().intValue() : 0;

            // 从 Redis Hash 读取 solvedCount
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

    // ======================== 判题后更新排行榜 ========================

    @Override
    public void updateRankOnAccepted(Integer contestId, Long userId, Integer problemId, int problemScore) {
        // 1. 检查该用户在该比赛中此题是否已经AC过（防止重复加分）
        String acKey = CONTEST_AC_PREFIX + contestId + ":" + userId + ":" + problemId;
        Boolean alreadyAc = stringRedisTemplate.hasKey(acKey);
        if (Boolean.TRUE.equals(alreadyAc)) {
            log.info("比赛 {} 用户 {} 题目 {} 已AC过，跳过排行榜更新", contestId, userId, problemId);
            return;
        }

        // 2. 标记已AC（7天过期，比赛结束后自动清理）
        stringRedisTemplate.opsForValue().set(acKey, "1", java.time.Duration.ofDays(7));

        // 3. ZSet 加分：ZINCRBY contest:rank:{contestId} {problemScore} {userId}
        String rankKey = RANK_KEY_PREFIX + contestId;
        stringRedisTemplate.opsForZSet().incrementScore(rankKey, String.valueOf(userId), problemScore);

//        // 4. 通过题数 +1
//        String solvedKey = "contest:" + contestId + ":user:" + userId + ":solved_count";
//        stringRedisTemplate.opsForValue().increment(solvedKey);

        log.info("排行榜已更新: contest={}, user={}, problem={}, +{}分",
                contestId, userId, problemId, problemScore);
    }

    // ======================== 持久化排行榜到 MySQL ========================

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

        // 清除该比赛的 Redis 排行榜数据（可选，保留7天后自动过期也行）
        log.info("比赛 {} 排行榜已持久化到数据库，共 {} 条", contestId, rankList.size());
    }

    // ======================== 工具方法 ========================

    /**
     * 批量查询用户名
     */
    private Map<Long, String> getUsernameMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }
}
