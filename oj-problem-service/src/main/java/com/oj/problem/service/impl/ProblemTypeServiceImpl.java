package com.oj.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.common.constant.MessageConstant;
import com.oj.common.exception.DeletionNotAllowedException;
import com.oj.common.result.PageResult;
import com.oj.problem.dto.ProblemTypeDTO;
import com.oj.problem.dto.ProblemTypeQueryDTO;
import com.oj.problem.entity.ProblemType;
import com.oj.problem.entity.ProblemTypesRel;
import com.oj.problem.mapper.*;
import com.oj.problem.service.ProblemTypeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProblemTypeServiceImpl implements ProblemTypeService {
    @Autowired
    ProblemTypeMapper problemTypeMapper;
    @Autowired
    ProblemTypeRelMapper problemTypeRel;
    @Autowired
    ProblemGroupMapper problemGroupMapper;
    @Autowired
    GroupTypeRelMapper groupTypeRelMapper;

    @Override
    public void save(ProblemTypeDTO problemTypeDTO) {
        ProblemType problemType = new ProblemType();
        BeanUtils.copyProperties(problemTypeDTO, problemType);
        problemType.setCreatedAt(LocalDateTime.now());
        problemType.setUpdatedAt(LocalDateTime.now());
        problemTypeMapper.insert(problemType);
    }

    @Override
    public PageResult pageType(ProblemTypeQueryDTO problemTypeQueryDTO) {
        String name = problemTypeQueryDTO.getName();
        Page<ProblemType> typePage = problemTypeQueryDTO.toPageDefaultSortByCreateTime("sort_order");
        LambdaQueryWrapper<ProblemType> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(name != null && !name.isEmpty(), ProblemType::getName, name);
        Page<ProblemType> problemTypePage = problemTypeMapper.selectPage(typePage, lambdaQueryWrapper);
        return new PageResult(problemTypePage.getTotal(), problemTypePage.getRecords());
    }

    @Override
    public ProblemType selectById(Long id) {
        return problemTypeMapper.selectById(id);
    }

    @Override
    public void deleteType(Long id) {
        List<Long> ids = problemTypeRel.selectTypeList(id);
        if (!ids.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.PROBLEM_TYPE);
        }
        List<Long> idsGroup = groupTypeRelMapper.selectTypeList(id);
        if (!idsGroup.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.PROBLEM_TYPE_GROUP);
        }
        problemTypeMapper.deleteById(id);
    }

    @Override
    public void updateType(ProblemTypeDTO problemTypeDTO) {
        ProblemType problemType = new ProblemType();
        BeanUtils.copyProperties(problemTypeDTO, problemType);
        problemType.setUpdatedAt(LocalDateTime.now());
        problemTypeMapper.updateById(problemType);
    }

    @Override
    public void TypeStatus(Integer status, Long id) {
        ProblemType problemType = new ProblemType();
        problemType.setIsActive(status);
        problemType.setId(Math.toIntExact(id));
        problemTypeMapper.updateById(problemType);
    }

    @Override
    public List<ProblemType> slectAll() {
        return problemTypeMapper.selectAll();
    }
}
