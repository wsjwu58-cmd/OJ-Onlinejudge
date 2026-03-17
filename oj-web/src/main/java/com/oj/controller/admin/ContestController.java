package com.oj.controller.admin;

import com.oj.dto.ContestDTO;
import com.oj.dto.ContestQueryDTO;
import com.oj.result.PageResult;
import com.oj.result.Result;
import com.oj.service.ContestService;
import com.oj.vo.ContestVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/Contest")
@Slf4j
@Tag(name = "比赛相关接口")
public class ContestController {
    @Autowired
    private ContestService contestService;

    //新增比赛
    @PostMapping
    @Operation(summary = "新增比赛")
    public Result AddContest(@RequestBody ContestDTO contestDTO){
        log.info("新增比赛：{}",contestDTO);
        contestService.saveContest(contestDTO);
        return Result.success();
    }

    //分页查询比赛
    @GetMapping("/page")
    @Operation(summary = "分页查询比赛")
    public Result<PageResult> PageContest(ContestQueryDTO contestQueryDTO){
        log.info("分类查询比赛：{}",contestQueryDTO);
        // 先更新所有比赛状态，确保查询结果准确
        contestService.updateContestStatus();
        PageResult pageResult=contestService.pageContest(contestQueryDTO);
        return Result.success(pageResult);
    }

    //根据ID查询比赛
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询比赛")
    public Result<ContestVO> ContestById(@PathVariable Long id){
        log.info("比赛ID：{}",id);
        ContestVO contest=contestService.selectId(id);
        return Result.success(contest);
    }
    //编辑比赛
    @PutMapping
    @Operation(summary = "编辑比赛")
    public Result ContestUpdate(@RequestBody ContestDTO contestDTO){
        log.info("编辑比赛：{}",contestDTO);
        contestService.update(contestDTO);
        return Result.success();
    }
    @DeleteMapping
    @Operation(summary = "根据ID删除比赛")
    public Result DeleteContest(@RequestParam Long id){
        log.info("根据ID删除比赛：{}",id);
        contestService.deleteId(id);
        return Result.success();
    }

}
