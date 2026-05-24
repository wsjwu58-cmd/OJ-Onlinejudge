package com.oj.contest.service;

import com.oj.common.result.PageResult;
import com.oj.contest.dto.ContestQueryDTO;
import com.oj.contest.vo.ContestRankVO;
import com.oj.contest.vo.ContestVO;
import java.util.List;

public interface UserContestService {
    PageResult pageContest(ContestQueryDTO contestQueryDTO);
    ContestVO getContestDetail(Long contestId, Long userId);
    void joinContest(Long contestId, Long userId);
    ContestVO getContestProblems(Long contestId, Long userId);
    List<ContestRankVO> getContestRank(Long contestId);
    void updateRankOnAccepted(Integer contestId, Long userId, Integer problemId, int problemScore);
    void persistRankToDb(Integer contestId);
}
