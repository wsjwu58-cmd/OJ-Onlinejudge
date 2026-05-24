package com.oj.judge.service;

import com.oj.common.result.PageResult;
import com.oj.judge.dto.SubmissionQueryDTO;
import com.oj.judge.vo.JudgeResultVO;
import com.oj.judge.vo.SubmissionVO;
import java.util.List;

public interface SubmissionService {
    List<JudgeResultVO> getSubmission(Long problemId);
    PageResult pageQuery(SubmissionQueryDTO submissionQueryDTO);
    SubmissionVO getById(Long id);
    void deleteById(Long id);
}
