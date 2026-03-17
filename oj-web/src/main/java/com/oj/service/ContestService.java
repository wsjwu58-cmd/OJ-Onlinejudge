package com.oj.service;

import com.oj.dto.ContestDTO;
import com.oj.dto.ContestQueryDTO;
import com.oj.result.PageResult;
import com.oj.vo.ContestVO;

public interface ContestService {
    void saveContest(ContestDTO contestDTO);

    PageResult pageContest(ContestQueryDTO contestQueryDTO);

    ContestVO selectId(Long id);

    void update(ContestDTO contestDTO);

    void deleteId(Long id);

    void updateContestStatus();
}
