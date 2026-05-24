package com.oj.contest.controller.user;

import com.oj.common.result.Result;
import com.oj.contest.dto.CommentDTO;
import com.oj.contest.service.SolutionCommentService;
import com.oj.contest.vo.ScrollResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/comment")
@Slf4j
@Tag(name = "用户端-题解评论")
public class SolutionCommentController {

    @Autowired
    private SolutionCommentService solutionCommentService;

    @PostMapping("/solution")
    @Operation(summary = "发布题解")
    public Result saveSolution(@RequestBody CommentDTO commentDTO) {
        solutionCommentService.save(commentDTO);
        return Result.success();
    }

    @GetMapping("/solution/{problemId}")
    @Operation(summary = "查询题解列表（滚动分页）")
    public Result<ScrollResult> querySolutions(
            @PathVariable Long problemId,
            @RequestParam(defaultValue = "9999999999999") Long lastId,
            @RequestParam(defaultValue = "0") Integer offset) {
        return solutionCommentService.querySolutions(problemId, lastId, offset);
    }

    @PostMapping("/comment")
    @Operation(summary = "发布评论")
    public Result saveComment(@RequestBody CommentDTO commentDTO) {
        return solutionCommentService.saveComment(commentDTO);
    }

    @GetMapping("/comment/{problemId}")
    @Operation(summary = "查询评论列表（滚动分页）")
    public Result<ScrollResult> queryComments(
            @PathVariable Long problemId,
            @RequestParam(defaultValue = "9999999999999") Long lastId,
            @RequestParam(defaultValue = "0") Integer offset) {
        return solutionCommentService.queryComments(problemId, lastId, offset);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取题解详情")
    public Result getSolutionDetail(@PathVariable Long id) {
        return solutionCommentService.getSolutionDetail(id);
    }
}
