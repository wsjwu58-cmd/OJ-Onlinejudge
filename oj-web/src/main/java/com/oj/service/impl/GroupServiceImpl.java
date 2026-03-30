package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.constant.MessageConstant;
import com.oj.constant.StatusConstant;
import com.oj.context.BaseContext;
import com.oj.dto.GroupDTO;
import com.oj.dto.GroupQueryDTO;
import com.oj.entity.GroupProblems;
import com.oj.entity.Problem;
import com.oj.entity.ProblemGroup;
import com.oj.exception.DeletionNotAllowedException;
import com.oj.mapper.GroupMapper;
import com.oj.mapper.GroupProblemMapper;
import com.oj.mapper.ProblemMapper;
import com.oj.result.PageResult;
import com.oj.service.GroupService;
import com.oj.vo.GroupVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class GroupServiceImpl implements GroupService {
    @Autowired
    private GroupMapper groupMapper;
    @Autowired
    private GroupProblemMapper groupProblemMapper;
    @Autowired
    private ProblemMapper problemMapper;

    @Override
    public void saveGroup(GroupDTO groupDTO) {
        //获取用户信息
        Long currentId = BaseContext.getCurrentId();
        //获取题单信息
        ProblemGroup problemGroup = new ProblemGroup();
        BeanUtils.copyProperties(groupDTO, problemGroup);
        //设置基本信息
        problemGroup.setCreatorId(currentId);
        problemGroup.setCreatedAt(LocalDateTime.now());
        problemGroup.setUpdatedAt(LocalDateTime.now());
        groupMapper.insert(problemGroup);
        //获取题目信息
        List<Problem> problemList = groupDTO.getProblemList();
        if (problemList != null && !problemList.isEmpty()) {
            List<GroupProblems> groupProblemsList = problemList.stream().map(problem -> {
                GroupProblems groupProblems = new GroupProblems();
                groupProblems.setGroupId(problemGroup.getId());
                groupProblems.setProblemId(problem.getId());
                groupProblems.setCreatedAt(LocalDateTime.now());
                groupProblems.setScore(10);
                return groupProblems;
            }).toList();
            groupProblemMapper.insert(groupProblemsList);
        }
    }

    @Override
    public PageResult pageGroup(GroupQueryDTO groupQueryDTO) {
        Page<GroupVO> groupVOPage = groupQueryDTO.ToPageDefaultSortByCreateTime("created_at");
        //构造器
        LambdaQueryWrapper<ProblemGroup> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        String title = groupQueryDTO.getTitle();
        lambdaQueryWrapper.like(title != null && !title.isEmpty(), ProblemGroup::getTitle, title);
        Page<GroupVO> page = groupMapper.selectPage(groupVOPage, lambdaQueryWrapper);
        return new PageResult(page.getTotal(), page.getRecords());
    }

    @Override
    public GroupVO selectId(Long id) {
        //根据ID查询题单数据
        ProblemGroup problemGroup = groupMapper.selectById(id);
        GroupVO groupVO = new GroupVO();
        if (problemGroup != null) {
            BeanUtils.copyProperties(problemGroup, groupVO);
        }
        List<Problem> list = groupMapper.SelectByID(id);
        groupVO.setProblemList(list);
        return groupVO;

    }

    @Override
    public void update(GroupDTO groupDTO) {
        //修改题单基本数据
        ProblemGroup problemGroup = new ProblemGroup();
        BeanUtils.copyProperties(groupDTO, problemGroup);
        groupMapper.updateById(problemGroup);
        //删除题单和题目关联数据
        List<Problem> problemList = groupDTO.getProblemList();
        groupProblemMapper.deleteByGroupId(groupDTO.getId());
        //新增关联数据
        List<GroupProblems> groupProblem = problemList.stream().map(problem -> {
            GroupProblems groupProblems = new GroupProblems();
            groupProblems.setScore(10);
            groupProblems.setGroupId(groupDTO.getId());
            groupProblems.setProblemId(problem.getId());
            groupProblems.setCreatedAt(LocalDateTime.now());
            return groupProblems;
        }).toList();
        groupProblemMapper.insert(groupProblem);
    }

    @Override
    public void deleteId(Long id) {
        //如果题单中有发布的题目，则不能删除
        List<Long> ids = groupProblemMapper.selectListAll(id);
        List<Problem> list = ids.stream().map(id1 -> {
            Problem problem = problemMapper.selectById(id1);
            if (problem.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.Context_PROBLEM);
            }
            return problem;
        }).toList();
        if (!list.isEmpty()) {
            groupMapper.deleteById(id);
            groupProblemMapper.deleteProblem(Math.toIntExact(id));
        } else {
            groupMapper.deleteById(id);
        }
    }

    @Override
    public void status(Integer status, Long id) {
        ProblemGroup problemGroup = new ProblemGroup();
        problemGroup.setStatus(status);
        problemGroup.setId(Math.toIntExact(id));
        groupMapper.updateById(problemGroup);
    }

    @Override
    public List<Problem> getGroupProblems(Long groupId, GroupQueryDTO groupQueryDTO) {
        // 先查询题单包含的所有题目ID
        List<GroupProblems> groupProblemsList = groupProblemMapper.selectByGroupId(groupId);
        if (groupProblemsList == null || groupProblemsList.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取题目ID列表
        List<Integer> problemIds = groupProblemsList.stream()
                .map(GroupProblems::getProblemId)
                .toList();


        LambdaQueryWrapper<Problem> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Problem::getId, problemIds);
        lambdaQueryWrapper.eq(Problem::getStatus, StatusConstant.ENABLE);
        List<Problem> problems = problemMapper.selectList(lambdaQueryWrapper);


        return problems;
    }
}


