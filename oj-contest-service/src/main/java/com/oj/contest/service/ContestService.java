package com.oj.contest.service;

import com.oj.common.result.PageResult;
import com.oj.contest.dto.ContestDTO;
import com.oj.contest.dto.ContestQueryDTO;
import com.oj.contest.vo.ContestVO;

public interface ContestService {
    void saveContest(ContestDTO contestDTO);
    PageResult pageContest(ContestQueryDTO contestQueryDTO);
    ContestVO selectId(Long id);
    void update(ContestDTO contestDTO);
    void deleteId(Long id);
    void updateContestStatus();
}
