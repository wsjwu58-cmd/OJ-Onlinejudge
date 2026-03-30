package com.oj.controller.admin;

import com.oj.dto.SubmissionQueryDTO;
import com.oj.entity.Submission;
import com.oj.result.PageResult;
import com.oj.result.Result;
import com.oj.service.SubmissionService;
import com.oj.vo.SubmissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/submissions")
@Slf4j
@Tag(name = "提交记录管理")
public class SubmissionAdminController {

    @Autowired
    private SubmissionService submissionService;

    @GetMapping("/page")
    @Operation(summary = "分页查询提交记录")
    public Result<PageResult> pageQuery(SubmissionQueryDTO submissionQueryDTO) {
        log.info("分页查询提交记录：{}", submissionQueryDTO);
        PageResult pageResult = submissionService.pageQuery(submissionQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询提交记录详情")
    public Result<SubmissionVO> getById(@PathVariable Long id) {
        log.info("查询提交记录详情，id：{}", id);
        SubmissionVO submissionVO = submissionService.getById(id);
        return Result.success(submissionVO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除提交记录")
    public Result delete(@PathVariable Long id) {
        log.info("删除提交记录，id：{}", id);
        submissionService.deleteById(id);
        return Result.success();
    }
}
