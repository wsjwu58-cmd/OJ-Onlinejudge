package com.oj.problem.service;

import com.oj.common.result.PageResult;
import com.oj.problem.dto.ProblemDTO;
import com.oj.problem.dto.ProblemQueryDTO;
import com.oj.problem.entity.Problem;
import com.oj.problem.vo.ProblemVO;

import java.util.List;

public interface ProblemService {
    void problemSave(ProblemDTO problemDTO);
    PageResult pageQuery(ProblemQueryDTO problemQueryDTO);
    ProblemVO problemById(Long id);
    void problemStatus(Integer status, Long id);
    void deleteProblem(Long id);
    void updateProblem(ProblemDTO problemDTO);
    List<Problem> selectAll();
    void deleteAll(List<Long> ids);
    PageResult queryType(ProblemQueryDTO problemQueryDTO);
}
