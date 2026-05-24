package com.oj.problem.service;

import com.oj.problem.entity.TestCase;

import java.util.List;

public interface TestCaseService {
    List<TestCase> selectTest(Integer problemId);
    void createTest(TestCase testCase);
    void updateTest(TestCase testCase);
    void deleteTest(Integer id);
}
