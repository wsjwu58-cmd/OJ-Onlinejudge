package com.oj.service;

import com.oj.dto.ProblemDTO;
import com.oj.dto.ProblemQueryDTO;
import com.oj.entity.Problem;
import com.oj.result.PageResult;
import com.oj.vo.ProblemVO;

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
