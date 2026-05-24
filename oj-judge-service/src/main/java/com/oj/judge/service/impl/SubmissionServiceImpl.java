package com.oj.judge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.api.ProblemClient;
import com.oj.api.UserClient;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.api.dto.UserFeignDTO;
import com.oj.common.context.BaseContext;
import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.judge.dto.SubmissionQueryDTO;
import com.oj.judge.entity.Submission;
import com.oj.judge.mapper.SubmissionMapper;
import com.oj.judge.service.SubmissionService;
import com.oj.judge.vo.JudgeResultVO;
import com.oj.judge.vo.SubmissionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SubmissionServiceImpl implements SubmissionService {

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private ProblemClient problemClient;

    @Autowired
    private UserClient userClient;

    @Override
    public List<JudgeResultVO> getSubmission(Long problemId) {
        Long currentId = BaseContext.getCurrentId();
        List<JudgeResultVO> list = submissionMapper.selectSubmission(currentId, problemId);
        list.forEach(vo -> {
            Result<ProblemFeignDTO> result = problemClient.getProblemById(vo.getProblemId());
            if (result != null && result.getData() != null) {
                vo.setTitle(result.getData().getTitle());
            }
        });
        return list;
    }

    @Override
    public PageResult pageQuery(SubmissionQueryDTO submissionQueryDTO) {
        Page<Submission> page = submissionQueryDTO.toPageDefaultSortByCreateTime("submit_time");
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(submissionQueryDTO.getUserId() != null, Submission::getUserId, submissionQueryDTO.getUserId())
                .eq(submissionQueryDTO.getProblemId() != null, Submission::getProblemId, submissionQueryDTO.getProblemId())
                .eq(submissionQueryDTO.getStatus() != null && !submissionQueryDTO.getStatus().isEmpty(),
                        Submission::getStatus, submissionQueryDTO.getStatus());
        Page<Submission> submissionPage = submissionMapper.selectPage(page, wrapper);

        List<SubmissionVO> list = submissionPage.getRecords().stream().map(submission -> {
            SubmissionVO vo = new SubmissionVO();
            BeanUtils.copyProperties(submission, vo);
            Result<UserFeignDTO> userResult = userClient.getUserById(submission.getUserId());
            if (userResult != null && userResult.getData() != null) {
                vo.setUsername(userResult.getData().getUsername());
            }
            Result<ProblemFeignDTO> problemResult = problemClient.getProblemById(submission.getProblemId());
            if (problemResult != null && problemResult.getData() != null) {
                vo.setProblemTitle(problemResult.getData().getTitle());
            }
            return vo;
        }).toList();

        return new PageResult(submissionPage.getTotal(), list);
    }

    @Override
    public SubmissionVO getById(Long id) {
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) return null;
        SubmissionVO vo = new SubmissionVO();
        BeanUtils.copyProperties(submission, vo);
        Result<UserFeignDTO> userResult = userClient.getUserById(submission.getUserId());
        if (userResult != null && userResult.getData() != null) {
            vo.setUsername(userResult.getData().getUsername());
        }
        Result<ProblemFeignDTO> problemResult = problemClient.getProblemById(submission.getProblemId());
        if (problemResult != null && problemResult.getData() != null) {
            vo.setProblemTitle(problemResult.getData().getTitle());
        }
        return vo;
    }

    @Override
    public void deleteById(Long id) {
        submissionMapper.deleteById(id);
    }
}
