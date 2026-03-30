package com.oj.service;

import com.oj.dto.SubmissionQueryDTO;
import com.oj.result.PageResult;
import com.oj.vo.JudgeResultVO;
import com.oj.vo.SubmissionVO;

import java.util.List;

public interface SubmissionService {
    List<JudgeResultVO> getSubmission(Long problemId);

    PageResult pageQuery(SubmissionQueryDTO submissionQueryDTO);

    SubmissionVO getById(Long id);

    void deleteById(Long id);
}
