package com.oj.controller.admin;

import com.oj.dto.ProblemDTO;
import com.oj.dto.ProblemQueryDTO;
import com.oj.entity.Problem;
import com.oj.result.PageResult;
import com.oj.result.Result;
import com.oj.service.ProblemService;
import com.oj.vo.ProblemVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/admin/problem")
@Tag(name = "题目管理接口")
public class ProblemController {
    @Autowired
    private ProblemService problemService;
    @PostMapping
    @Operation(summary = "添加题目")
    public Result problemSave(@RequestBody ProblemDTO problemDTO){
        log.info("添加题目：{}",problemDTO);
        problemService.problemSave(problemDTO);
        return Result.success();
    }
    @GetMapping("/page/of")
    @Operation(summary = "分页查询题目")
    public Result<PageResult> problemPage(ProblemQueryDTO problemQueryDTO){
        log.info("分页查询题目：{}",problemQueryDTO);
        PageResult pageResult=problemService.pageQuery(problemQueryDTO);
        return Result.success(pageResult);
    }
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询题目")
    public Result<ProblemVO> problemById(@PathVariable Long id){
        log.info("题目ID：{}",id);
        ProblemVO problemVO=problemService.problemById(id);
        return Result.success(problemVO);
    }

    @PostMapping("/status/{status}/{id}")
    @Operation(summary = "上架/下架题目")
    public Result problemStatus(@PathVariable Integer status,@PathVariable Long id){
        log.info("用户id:{}",id);
        problemService.problemStatus(status,id);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "根据ID删除题目")
    public Result problemDelete(@RequestParam Long id){
        log.info("删除题目ID：{}",id);
        problemService.deleteProblem(id);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "编辑题目")
    public Result updateType(@RequestBody ProblemDTO problemDTO){
        log.info("编辑题目信息：{}",problemDTO);
        problemService.updateProblem(problemDTO);
        return Result.success();
    }

    @GetMapping("/all")
    @Operation(summary = "查询所有题目")
    public Result<List<Problem>> selectAllProblem(){
        log.info("查询所有题目");
        List<Problem> problemList=problemService.selectAll();
        return Result.success(problemList);
    }

    @DeleteMapping("/deleteAll")
    @Operation(summary = "批量删除题目")
    public Result DeleteProblem(@RequestParam List<Long> ids){
        log.info("批量删除题目：{}",ids);
        problemService.deleteAll(ids);
        return Result.success();
    }


}
