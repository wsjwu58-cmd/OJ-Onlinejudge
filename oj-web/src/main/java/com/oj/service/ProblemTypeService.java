package com.oj.service;

import com.oj.dto.ProblemTypeDTO;
import com.oj.dto.ProblemTypeQueryDTO;
import com.oj.entity.ProblemType;
import com.oj.result.PageResult;

import java.util.List;

public interface ProblemTypeService  {
    void save(ProblemTypeDTO problemTypeDTO);

    PageResult pageType(ProblemTypeQueryDTO problemTypeQueryDTO);

    ProblemType selectById(Long id);

    void deleteType(Long id);

    void updateType(ProblemTypeDTO problemTypeDTO);

    void TypeStatus(Integer status, Long id);

    List<ProblemType> slectAll();
}
