package com.oj.problem.service.impl;

import com.oj.problem.entity.TestCase;
import com.oj.problem.mapper.TestCaseMapper;
import com.oj.problem.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestCaseServiceImpl implements TestCaseService {
    @Autowired
    private TestCaseMapper testCaseMapper;

    @Override
    public List<TestCase> selectTest(Integer problemId) {
        return testCaseMapper.selectByProblemId(problemId);
    }

    @Override
    public void createTest(TestCase testCase) {
        testCase.setCreatedAt(LocalDateTime.now());
        testCase.setUpdatedAt(LocalDateTime.now());
        testCaseMapper.insert(testCase);
    }

    @Override
    public void updateTest(TestCase testCase) {
        testCase.setUpdatedAt(LocalDateTime.now());
        testCaseMapper.updateById(testCase);
    }

    @Override
    public void deleteTest(Integer id) {
        testCaseMapper.deleteById(id);
    }
}
