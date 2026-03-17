package com.oj.service.impl;

import com.oj.entity.TestCase;
import com.oj.mapper.TestCaseMapper;
import com.oj.service.TestCaseService;
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
        // 设置创建和更新时间
        testCase.setCreatedAt(LocalDateTime.now());
        testCase.setUpdatedAt(LocalDateTime.now());
        // 插入测试用例
        testCaseMapper.insert(testCase);
    }
    
    @Override
    public void updateTest(TestCase testCase) {
        // 设置更新时间
        testCase.setUpdatedAt(LocalDateTime.now());
        // 更新测试用例
        testCaseMapper.updateById(testCase);
    }
    
    @Override
    public void deleteTest(Integer id) {
        // 删除测试用例
        testCaseMapper.deleteById(id);
    }
}
