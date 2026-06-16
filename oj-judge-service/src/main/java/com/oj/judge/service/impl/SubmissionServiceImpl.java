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

import java.util.*;
import java.util.stream.Collectors;

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

        if (!list.isEmpty()) {
            Set<Integer> problemIds = list.stream()
                    .map(JudgeResultVO::getProblemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!problemIds.isEmpty()) {
                Result<List<ProblemFeignDTO>> batchResult = problemClient.getProblemsByIds(
                        new ArrayList<>(problemIds));
                if (batchResult != null && batchResult.getData() != null) {
                    Map<Integer, String> titleMap = batchResult.getData().stream()
                            .collect(Collectors.toMap(ProblemFeignDTO::getId,
                                    p -> p.getTitle() != null ? p.getTitle() : "",
                                    (a, b) -> a));
                    list.forEach(vo -> {
                        String title = titleMap.get(vo.getProblemId());
                        if (title != null) vo.setTitle(title);
                    });
                }
            }
        }
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
            return vo;
        }).toList();

        if (!list.isEmpty()) {
            Set<Long> userIds = list.stream()
                    .map(SubmissionVO::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Set<Integer> problemIds = list.stream()
                    .map(SubmissionVO::getProblemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<Long, String> usernameMap = Collections.emptyMap();
            if (!userIds.isEmpty()) {
                Result<List<UserFeignDTO>> userResult = userClient.getUsersByIds(new ArrayList<>(userIds));
                if (userResult != null && userResult.getData() != null) {
                    usernameMap = userResult.getData().stream()
                            .collect(Collectors.toMap(UserFeignDTO::getId, UserFeignDTO::getUsername, (a, b) -> a));
                }
            }

            Map<Integer, String> titleMap = Collections.emptyMap();
            if (!problemIds.isEmpty()) {
                Result<List<ProblemFeignDTO>> problemResult = problemClient.getProblemsByIds(
                        new ArrayList<>(problemIds));
                if (problemResult != null && problemResult.getData() != null) {
                    titleMap = problemResult.getData().stream()
                            .collect(Collectors.toMap(ProblemFeignDTO::getId,
                                    p -> p.getTitle() != null ? p.getTitle() : "",
                                    (a, b) -> a));
                }
            }

            for (SubmissionVO vo : list) {
                vo.setUsername(usernameMap.get(vo.getUserId()));
                vo.setProblemTitle(titleMap.get(vo.getProblemId()));
            }
        }

        return new PageResult(submissionPage.getTotal(), list);
    }

    @Override
    public SubmissionVO getById(Long id) {
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) return null;
        SubmissionVO vo = new SubmissionVO();
        BeanUtils.copyProperties(submission, vo);

        Set<Long> userIds = Set.of(submission.getUserId());
        Set<Integer> problemIds = Set.of(submission.getProblemId());

        Result<List<UserFeignDTO>> userResult = userClient.getUsersByIds(new ArrayList<>(userIds));
        if (userResult != null && userResult.getData() != null && !userResult.getData().isEmpty()) {
            vo.setUsername(userResult.getData().get(0).getUsername());
        }

        Result<List<ProblemFeignDTO>> problemResult = problemClient.getProblemsByIds(
                new ArrayList<>(problemIds));
        if (problemResult != null && problemResult.getData() != null && !problemResult.getData().isEmpty()) {
            vo.setProblemTitle(problemResult.getData().get(0).getTitle());
        }

        return vo;
    }

    @Override
    public void deleteById(Long id) {
        submissionMapper.deleteById(id);
    }
}
