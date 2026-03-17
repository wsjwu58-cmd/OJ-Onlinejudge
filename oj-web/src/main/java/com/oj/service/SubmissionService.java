package com.oj.service;

import com.oj.vo.JudgeResultVO;

import java.util.List;

public interface SubmissionService {
    List<JudgeResultVO> getSubmission(Long problemId);
}
