package com.oj.problem.service;

import com.oj.common.result.PageResult;
import com.oj.problem.dto.ProblemTypeDTO;
import com.oj.problem.dto.ProblemTypeQueryDTO;
import com.oj.problem.entity.ProblemType;

import java.util.List;

public interface ProblemTypeService {
    void save(ProblemTypeDTO problemTypeDTO);
    PageResult pageType(ProblemTypeQueryDTO problemTypeQueryDTO);
    ProblemType selectById(Long id);
    void deleteType(Long id);
    void updateType(ProblemTypeDTO problemTypeDTO);
    void TypeStatus(Integer status, Long id);
    List<ProblemType> slectAll();
}
