package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.constant.MessageConstant;
import com.oj.dto.ProblemTypeDTO;
import com.oj.dto.ProblemTypeQueryDTO;
import com.oj.entity.ProblemType;
import com.oj.exception.DeletionNotAllowedException;
import com.oj.mapper.*;
import com.oj.result.PageResult;
import com.oj.service.ProblemTypeService;
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
    ProblemMapper problemMapper;
    @Autowired
    ProblemTypeRel problemTypeRel;
    @Autowired
    ProblemGroupMapper problemGroupMapper;
    @Autowired
    GroupTypeRelMapper groupTypeRelMapper;
    //新增分类
    @Override
    public void save(ProblemTypeDTO problemTypeDTO) {
        ProblemType problemType=new ProblemType();
        BeanUtils.copyProperties(problemTypeDTO,problemType);
        problemType.setCreatedAt(LocalDateTime.now());
        problemType.setUpdatedAt(LocalDateTime.now());
        problemTypeMapper.insert(problemType);
    }

    @Override
    public PageResult pageType(ProblemTypeQueryDTO problemTypeQueryDTO) {
        //查询条件
        String name=problemTypeQueryDTO.getName();
        Page<ProblemType> typePage=problemTypeQueryDTO.ToPageDefaultSortByCreateTime("sort_order");
        //构造器
        LambdaQueryWrapper<ProblemType> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(name!=null&&!name.isEmpty(),ProblemType::getName,name);
        Page<ProblemType> problemTypePage = problemTypeMapper.selectPage(typePage, lambdaQueryWrapper);
        return new PageResult(problemTypePage.getTotal(),problemTypePage.getRecords());
    }

    @Override
    public ProblemType selectById(Long id) {
        return problemTypeMapper.selectById(id);
    }

    @Override
    public void deleteType(Long id) {
        //当前分类是否关联了题目
        List<Long> ids=problemTypeRel.selectTypeList(id);
        if(!ids.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.PROBLEM_TYPE);
        }
        //当前分类是否关联了题组
        //关联的题组ID
        List<Long> idsGroup=groupTypeRelMapper.selectTypeList(id);
        if (!idsGroup.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.PROBLEM_TYPE_GROUP);
        }
        //删除分类
        problemTypeMapper.deleteById(id);
    }

    //修改分类
    @Override
    public void updateType(ProblemTypeDTO problemTypeDTO) {
        ProblemType problemType=new ProblemType();
        BeanUtils.copyProperties(problemTypeDTO,problemType);
        problemType.setUpdatedAt(LocalDateTime.now());
        problemTypeMapper.updateById(problemType);
    }

    //修改状态
    @Override
    public void TypeStatus(Integer status, Long id) {
        ProblemType problemType=new ProblemType();
        problemType.setIsActive(status);
        problemType.setId(Math.toIntExact(id));
        problemTypeMapper.updateById(problemType);
    }

    @Override
    public List<ProblemType> slectAll() {
        return problemTypeMapper.selectAll();
    }
}
