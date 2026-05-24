package com.oj.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.common.constant.MessageConstant;
import com.oj.common.constant.StatusConstant;
import com.oj.common.context.BaseContext;
import com.oj.common.exception.DeletionNotAllowedException;
import com.oj.common.result.PageResult;
import com.oj.problem.dto.GroupDTO;
import com.oj.problem.dto.GroupQueryDTO;
import com.oj.problem.entity.GroupProblems;
import com.oj.problem.entity.Problem;
import com.oj.problem.entity.ProblemGroup;
import com.oj.problem.mapper.GroupMapper;
import com.oj.problem.mapper.GroupProblemMapper;
import com.oj.problem.mapper.ProblemMapper;
import com.oj.problem.service.GroupService;
import com.oj.problem.vo.GroupVO;
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
        Long currentId = BaseContext.getCurrentId();
        ProblemGroup problemGroup = new ProblemGroup();
        BeanUtils.copyProperties(groupDTO, problemGroup);
        problemGroup.setCreatorId(currentId);
        problemGroup.setCreatedAt(LocalDateTime.now());
        problemGroup.setUpdatedAt(LocalDateTime.now());
        groupMapper.insert(problemGroup);

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
        Page<GroupVO> groupVOPage = groupQueryDTO.toPageDefaultSortByCreateTime("created_at");
        LambdaQueryWrapper<ProblemGroup> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        String title = groupQueryDTO.getTitle();
        lambdaQueryWrapper.like(title != null && !title.isEmpty(), ProblemGroup::getTitle, title);
        Page<GroupVO> page = groupMapper.selectPage(groupVOPage, lambdaQueryWrapper);
        return new PageResult(page.getTotal(), page.getRecords());
    }

    @Override
    public GroupVO selectId(Long id) {
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
        ProblemGroup problemGroup = new ProblemGroup();
        BeanUtils.copyProperties(groupDTO, problemGroup);
        groupMapper.updateById(problemGroup);
        List<Problem> problemList = groupDTO.getProblemList();
        groupProblemMapper.deleteByGroupId(groupDTO.getId());
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
        List<Long> ids = groupProblemMapper.selectListAll(id);
        List<Problem> list = ids.stream().map(id1 -> {
            Problem problem = problemMapper.selectById(id1);
            if (problem.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.CONTEXT_PROBLEM);
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
        List<GroupProblems> groupProblemsList = groupProblemMapper.selectByGroupId(groupId);
        if (groupProblemsList == null || groupProblemsList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> problemIds = groupProblemsList.stream()
                .map(GroupProblems::getProblemId)
                .toList();
        LambdaQueryWrapper<Problem> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Problem::getId, problemIds);
        lambdaQueryWrapper.eq(Problem::getStatus, StatusConstant.ENABLE);
        return problemMapper.selectList(lambdaQueryWrapper);
    }
}
