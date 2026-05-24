package com.oj.problem.controller.user;

import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.problem.dto.GroupQueryDTO;
import com.oj.problem.entity.Problem;
import com.oj.problem.service.GroupService;
import com.oj.problem.vo.GroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/group")
@Slf4j
@Tag(name = "用户端-题单接口")
public class GroupUserController {
    @Autowired
    private GroupService groupService;

    @GetMapping("/page")
    @Operation(summary = "分页查询题单列表")
    public Result<PageResult> pageGroup(GroupQueryDTO groupQueryDTO) {
        log.info("分页查询题单列表：{}", groupQueryDTO);
        PageResult pageResult = groupService.pageGroup(groupQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取题单详情")
    public Result<GroupVO> getGroupDetail(@PathVariable Integer id) {
        log.info("获取题单详情：{}", id);
        GroupVO groupVO = groupService.selectId(Long.valueOf(id));
        return Result.success(groupVO);
    }

    @GetMapping("/{id}/problems")
    @Operation(summary = "获取题单中的题目列表")
    public Result<List<Problem>> getGroupProblems(@PathVariable Integer id, GroupQueryDTO groupQueryDTO) {
        log.info("获取题单中的题目列表：{}, {}", id, groupQueryDTO);
        List<Problem> groupProblems = groupService.getGroupProblems(Long.valueOf(id), groupQueryDTO);
        return Result.success(groupProblems);
    }
}
