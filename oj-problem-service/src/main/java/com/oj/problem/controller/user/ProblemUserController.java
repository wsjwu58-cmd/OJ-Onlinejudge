package com.oj.problem.controller.user;

import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.problem.dto.ProblemQueryDTO;
import com.oj.problem.entity.ProblemType;
import com.oj.problem.service.ProblemService;
import com.oj.problem.service.ProblemTypeService;
import com.oj.problem.vo.ProblemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/problem")
@Slf4j
@Tag(name = "用户端-题目模块")
public class ProblemUserController {
    @Autowired
    private ProblemService problemService;
    @Autowired
    private ProblemTypeService problemTypeService;

    @GetMapping("/type")
    @Operation(summary = "根据条件查询题目")
    public Result<PageResult> problemSearch(ProblemQueryDTO problemQueryDTO) {
        log.info("根据条件查询题目：{}", problemQueryDTO);
        PageResult pageResult = problemService.queryType(problemQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/alltype")
    @Operation(summary = "查询所有类别")
    public Result<List<ProblemType>> selectAllType() {
        log.info("查询所有分类");
        List<ProblemType> typeList = problemTypeService.slectAll();
        return Result.success(typeList);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询题目")
    public Result<ProblemVO> problemById(@PathVariable Long id) {
        log.info("题目ID：{}", id);
        ProblemVO problemVO = problemService.problemById(id);
        return Result.success(problemVO);
    }
}
