package com.oj.contest.controller.admin;

import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.contest.dto.ContestDTO;
import com.oj.contest.dto.ContestQueryDTO;
import com.oj.contest.service.ContestService;
import com.oj.contest.vo.ContestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/Contest")
@Slf4j
@Tag(name = "管理端-竞赛管理")
public class ContestController {

    @Autowired
    private ContestService contestService;

    @PostMapping
    @Operation(summary = "创建竞赛")
    public Result save(@RequestBody ContestDTO contestDTO) {
        contestService.saveContest(contestDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询竞赛")
    public Result<PageResult> page(ContestQueryDTO contestQueryDTO) {
        contestService.updateContestStatus();
        PageResult pageResult = contestService.pageContest(contestQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询竞赛")
    public Result<ContestVO> selectId(@PathVariable Long id) {
        ContestVO contestVO = contestService.selectId(id);
        return Result.success(contestVO);
    }

    @PutMapping
    @Operation(summary = "修改竞赛")
    public Result update(@RequestBody ContestDTO contestDTO) {
        contestService.update(contestDTO);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "删除竞赛")
    public Result delete(@RequestParam Long id) {
        contestService.deleteId(id);
        return Result.success();
    }
}
