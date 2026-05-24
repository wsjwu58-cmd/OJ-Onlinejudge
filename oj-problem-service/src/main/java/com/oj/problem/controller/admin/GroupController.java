package com.oj.problem.controller.admin;

import com.oj.common.result.PageResult;
import com.oj.common.result.Result;
import com.oj.problem.dto.GroupDTO;
import com.oj.problem.dto.GroupQueryDTO;
import com.oj.problem.service.GroupService;
import com.oj.problem.vo.GroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/group")
@Slf4j
@Tag(name = "管理端-题单接口")
public class GroupController {
    @Autowired
    private GroupService groupService;

    @PostMapping
    @Operation(summary = "新增题单")
    public Result SaveGroup(@RequestBody GroupDTO groupDTO) {
        log.info("新增题单：{}", groupDTO);
        groupService.saveGroup(groupDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询题单")
    public Result<PageResult> PageGroup(GroupQueryDTO groupQueryDTO) {
        log.info("分页查询题单：{}", groupQueryDTO);
        PageResult pageResult = groupService.pageGroup(groupQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询题单")
    public Result<GroupVO> GroupById(@PathVariable Long id) {
        log.info("题单ID：{}", id);
        GroupVO groupVO = groupService.selectId(id);
        return Result.success(groupVO);
    }

    @PutMapping
    @Operation(summary = "编辑题单")
    public Result GroupUpdate(@RequestBody GroupDTO groupDTO) {
        log.info("编辑题单：{}", groupDTO);
        groupService.update(groupDTO);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "根据ID删除题单")
    public Result DeleteGroup(@RequestParam Long id) {
        log.info("根据ID删除题单：{}", id);
        groupService.deleteId(id);
        return Result.success();
    }

    @PostMapping("/status/{status}/{id}")
    @Operation(summary = "上架/下架题单")
    public Result problemStatus(@PathVariable Integer status, @PathVariable Long id) {
        log.info("题单状态：{}", status);
        groupService.status(status, id);
        return Result.success();
    }
}
