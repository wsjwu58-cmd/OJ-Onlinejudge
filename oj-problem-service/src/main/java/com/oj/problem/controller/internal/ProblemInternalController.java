package com.oj.problem.controller.internal;

import com.oj.api.dto.ProblemAcceptanceFeignDTO;
import com.oj.api.dto.ProblemFeignDTO;
import com.oj.api.dto.TestCaseFeignDTO;
import com.oj.common.constant.StatusConstant;
import com.oj.common.result.Result;
import com.oj.problem.entity.Problem;
import com.oj.problem.entity.TestCase;
import com.oj.problem.mapper.ProblemMapper;
import com.oj.problem.mapper.TestCaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/internal/problem")
@Slf4j
public class ProblemInternalController {

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @GetMapping("/{id}")
    public Result<ProblemFeignDTO> getProblemById(@PathVariable Integer id) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            return Result.error("题目不存在");
        }
        ProblemFeignDTO dto = new ProblemFeignDTO();
        BeanUtils.copyProperties(problem, dto);
        return Result.success(dto);
    }

    @PostMapping("/batch")
    public Result<List<ProblemFeignDTO>> getProblemsByIds(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<Problem> problems = problemMapper.selectBatchIds(ids);
        List<ProblemFeignDTO> dtoList = problems.stream().map(p -> {
            ProblemFeignDTO dto = new ProblemFeignDTO();
            BeanUtils.copyProperties(p, dto);
            return dto;
        }).toList();
        return Result.success(dtoList);
    }

    @GetMapping("/{problemId}/test-cases")
    public Result<List<TestCaseFeignDTO>> getTestCasesByProblemId(@PathVariable Integer problemId) {
        List<TestCase> testCases = testCaseMapper.selectByProblemId(problemId);
        List<TestCaseFeignDTO> dtoList = testCases.stream().map(tc -> {
            TestCaseFeignDTO dto = new TestCaseFeignDTO();
            BeanUtils.copyProperties(tc, dto);
            return dto;
        }).toList();
        return Result.success(dtoList);
    }

    @PutMapping("/{id}/acceptance")
    public Result<Void> updateProblemAcceptance(@PathVariable Integer id) {
        Problem problem = problemMapper.selectById(id);
        if (problem != null) {
            BigDecimal acceptance = problem.getAcceptance();
            problem.setAcceptance(acceptance != null ? acceptance.add(BigDecimal.ONE) : BigDecimal.ONE);
            problemMapper.updateById(problem);
        }
        return Result.success();
    }

    @GetMapping("/count")
    public Result<Long> countProblems() {
        long count = problemMapper.selectCount(null);
        return Result.success(count);
    }

    @GetMapping("/count-by-status")
    public Result<Integer> countProblemsByStatus(@RequestParam("status") Integer status) {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Problem::getStatus, status);
        return Result.success(Math.toIntExact(problemMapper.selectCount(wrapper)));
    }

    @GetMapping("/count-by-date")
    public Result<Integer> countProblemsByDate(
            @RequestParam(value = "begin", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime begin,
            @RequestParam(value = "end", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        Map<String, Object> map = new HashMap<>();
        if (begin != null) map.put("begin", begin);
        if (end != null) map.put("end", end);
        Integer count = problemMapper.countProblem(map);
        return Result.success(count != null ? count : 0);
    }

    @GetMapping("/acceptance-top10")
    public Result<List<ProblemAcceptanceFeignDTO>> selectAcceptanceTop10() {
        List<com.oj.problem.vo.ProblemAcceptanceVO> list = problemMapper.selectCount10();
        List<ProblemAcceptanceFeignDTO> dtoList = list.stream().map(vo -> {
            ProblemAcceptanceFeignDTO dto = new ProblemAcceptanceFeignDTO();
            dto.setId(vo.getId());
            dto.setTitle(vo.getTitle());
            dto.setAcceptance(vo.getAcceptance() != null ? BigDecimal.valueOf(vo.getAcceptance()) : null);
            return dto;
        }).toList();
        return Result.success(dtoList);
    }
}
