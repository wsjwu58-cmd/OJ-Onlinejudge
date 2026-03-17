package com.oj.controller.admin;

import com.oj.entity.TestCase;
import com.oj.result.Result;
import com.oj.service.TestCaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/test")
@Slf4j
@Tag(name = "测试用例相关接口")
public class TestCaseController {
    @Autowired
    private TestCaseService testCaseService;

    @GetMapping("/{problemId}")
    @Operation(summary = "根据题目ID查询测试用例")
    public Result<List<TestCase>> selectTest(@PathVariable Integer problemId){
        log.info("题目ID：{}",problemId);
        List<TestCase> testCases=testCaseService.selectTest(problemId);
        return Result.success(testCases);
    }

    @PostMapping
    @Operation(summary = "创建测试用例")
    public Result createTest(@RequestBody TestCase testCase){
        log.info("创建测试用例：{}",testCase);
        testCaseService.createTest(testCase);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "编辑测试用例")
    public Result updateTest(@RequestBody TestCase testCase){
        log.info("更新测试用例：{}",testCase);
        testCaseService.updateTest(testCase);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除测试用例")
    public Result deleteTest(@PathVariable Integer id){
        log.info("删除测试用例ID：{}",id);
        testCaseService.deleteTest(id);
        return Result.success();
    }

}
