package com.oj.service;

import com.oj.dto.ContestQueryDTO;
import com.oj.result.PageResult;
import com.oj.vo.ContestRankVO;
import com.oj.vo.ContestVO;

import java.util.List;

/**
 * 用户端比赛 Service
 */
public interface UserContestService {

    /** 分页查询比赛（用户端） */
    PageResult pageContest(ContestQueryDTO contestQueryDTO);

    /** 查询比赛详情（含参赛人数、当前用户是否已报名） */
    ContestVO getContestDetail(Long contestId, Long userId);

    /** 报名比赛 */
    void joinContest(Long contestId, Long userId);

    /** 获取比赛题目列表（比赛Running/Ended且用户已报名才可查看） */
    ContestVO getContestProblems(Long contestId, Long userId);

    /** 获取比赛排行榜（Redis ZSet 实时排行榜，按分数降序） */
    List<ContestRankVO> getContestRank(Long contestId);

    /**
     * 比赛提交AC后更新排行榜（由判题消费者调用）
     * @param contestId   比赛ID
     * @param userId      用户ID
     * @param problemId   题目ID
     * @param problemScore 该题分值
     */
    void updateRankOnAccepted(Integer contestId, Long userId, Integer problemId, int problemScore);

    /** 比赛结束后将 Redis 排行榜持久化到 MySQL */
    void persistRankToDb(Integer contestId);
}
