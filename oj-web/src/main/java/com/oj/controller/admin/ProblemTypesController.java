package com.oj.controller.admin;

import com.oj.dto.ProblemTypeDTO;
import com.oj.dto.ProblemTypeQueryDTO;
import com.oj.entity.ProblemType;
import com.oj.result.PageResult;
import com.oj.result.Result;
import com.oj.service.ProblemTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/admin/problem/type")
@Tag(name = "题目分类相关接口")
public class ProblemTypesController {

    @Autowired
    private ProblemTypeService problemTypeService;

    //新增分类
    @PostMapping
    @Operation(summary = "新增分类")
    public Result saveType(@RequestBody ProblemTypeDTO problemTypeDTO){
        log.info("新增分类：{}",problemTypeDTO);
        problemTypeService.save(problemTypeDTO);
        return Result.success();
    }

    //分页查询
    @GetMapping("/page")
    @Operation(summary = "分页查询分类")
    public Result<PageResult> pageType(ProblemTypeQueryDTO problemTypeQueryDTO){
        log.info("分页查询分类：{}",problemTypeQueryDTO);
        PageResult pageResult=problemTypeService.pageType(problemTypeQueryDTO);
        return Result.success(pageResult);
    }
    //根据ID查询分类
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询分类")
    public Result<ProblemType> selectId(@PathVariable Long id){
        log.info("分类ID：{}",id);
        ProblemType problemType=problemTypeService.selectById(id);
        return Result.success(problemType);
    }
    //根据ID删除分类
    @DeleteMapping
    @Operation(summary = "根据ID删除分类")
    public Result deleteType(@RequestParam Long id){
        log.info("删除ID：{}",id);
        problemTypeService.deleteType(id);
        return Result.success();
    }
    @PutMapping
    @Operation(summary = "编辑分类")
    public Result updateType(@RequestBody ProblemTypeDTO problemTypeDTO){
        log.info("编辑分类信息：{}",problemTypeDTO);
        problemTypeService.updateType(problemTypeDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}/{id}")
    @Operation(summary = "启用/停用分类")
    public Result TypeStatus(@PathVariable Integer status,@PathVariable Long id){
        log.info("用户id:{}",id);
        problemTypeService.TypeStatus(status,id);
        return Result.success();
    }

    @GetMapping("/all")
    @Operation(summary = "下拉框查询分类")
    public Result<List<ProblemType>> AllType(){
        log.info("查询全部分类");
        List<ProblemType> typeList=problemTypeService.slectAll();
        return Result.success(typeList);
    }

}
