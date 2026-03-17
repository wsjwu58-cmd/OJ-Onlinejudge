package com.oj.service.impl;

import com.oj.context.BaseContext;
import com.oj.entity.Problem;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubMissionMapper;
import com.oj.service.SubmissionService;
import com.oj.vo.JudgeResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubmissionServiceImpl implements SubmissionService {
    @Autowired
    private SubMissionMapper subMissionMapper;
    @Autowired
    private ProblemMapper problemMapper;
    @Override
    public List<JudgeResultVO> getSubmission(Long problemId) {
        //获取当前用户ID
        Long currentId = BaseContext.getCurrentId();
        //根据用户ID查询提交记录
        List<JudgeResultVO> judgeResultVOS=subMissionMapper.selectSubmission(currentId,problemId);
        List<JudgeResultVO> list = judgeResultVOS.stream().map(judgeResultVO -> {
            Problem problem = problemMapper.selectById(judgeResultVO.getProblemId());
            judgeResultVO.setTitle(problem.getTitle());
            return judgeResultVO;
        }).toList();
        return list;
    }
}
