package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.context.BaseContext;
import com.oj.dto.SubmissionQueryDTO;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.entity.User;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubMissionMapper;

import com.oj.mapper.UserMapper;
import com.oj.result.PageResult;
import com.oj.service.SubmissionService;
import com.oj.vo.JudgeResultVO;
import com.oj.vo.SubmissionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubmissionServiceImpl implements SubmissionService {
    @Autowired
    private SubMissionMapper submissionMapper;
    @Autowired
    private ProblemMapper problemMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public List<JudgeResultVO> getSubmission(Long problemId) {
        //获取当前用户ID
        Long currentId = BaseContext.getCurrentId();
        //根据用户ID查询提交记录
        List<JudgeResultVO> judgeResultVOS=submissionMapper.selectSubmission(currentId,problemId);
        List<JudgeResultVO> list = judgeResultVOS.stream().map(judgeResultVO -> {
            Problem problem = problemMapper.selectById(judgeResultVO.getProblemId());
            judgeResultVO.setTitle(problem.getTitle());
            return judgeResultVO;
        }).toList();
        return list;
    }

    @Override
    public PageResult pageQuery(SubmissionQueryDTO submissionQueryDTO) {
        Page<Submission> page = submissionQueryDTO.ToPageDefaultSortByCreateTime("submit_time");
        
        LambdaQueryWrapper<Submission> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        
        lambdaQueryWrapper
                .eq(submissionQueryDTO.getUserId() != null, Submission::getUserId, submissionQueryDTO.getUserId())
                .eq(submissionQueryDTO.getProblemId() != null, Submission::getProblemId, submissionQueryDTO.getProblemId())
                .eq(submissionQueryDTO.getStatus() != null && !submissionQueryDTO.getStatus().isEmpty(), Submission::getStatus, submissionQueryDTO.getStatus());
        
        Page<Submission> submissionPage = submissionMapper.selectPage(page, lambdaQueryWrapper);
        
        List<SubmissionVO> list = submissionPage.getRecords().stream().map(submission -> {
            SubmissionVO submissionVO = new SubmissionVO();
            BeanUtils.copyProperties(submission, submissionVO);
            
            User user = userMapper.selectById(submission.getUserId());
            if (user != null) {
                submissionVO.setUsername(user.getUsername());
            }
            
            Problem problem = problemMapper.selectById(submission.getProblemId());
            if (problem != null) {
                submissionVO.setProblemTitle(problem.getTitle());
            }
            
            return submissionVO;
        }).toList();
        
        return new PageResult(submissionPage.getTotal(), list);
    }

    @Override
    public SubmissionVO getById(Long id) {
        var submission = submissionMapper.selectById(id);
        if (submission == null) {
            return null;
        }
        SubmissionVO submissionVO = new SubmissionVO();
        BeanUtils.copyProperties(submission, submissionVO);
        
        // 查询用户名
        User user = userMapper.selectById(submission.getUserId());
        if (user != null) {
            submissionVO.setUsername(user.getUsername());
        }
        
        // 查询题目标题
        Problem problem = problemMapper.selectById(submission.getProblemId());
        if (problem != null) {
            submissionVO.setProblemTitle(problem.getTitle());
        }
        
        return submissionVO;
    }

    @Override
    public void deleteById(Long id) {
        submissionMapper.deleteById(id);
    }
}
