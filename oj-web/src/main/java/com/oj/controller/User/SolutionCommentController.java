package com.oj.controller.User;

import com.oj.dto.CommentDTO;
import com.oj.result.Result;
import com.oj.service.SolutionCommentService;
import com.oj.vo.ScrollResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user/comment")
@Tag(name = "题解和评论")
public class SolutionCommentController {
    @Autowired
    private SolutionCommentService solutionCommentService;

    @PostMapping
    @Operation(summary = "添加题解/评论")
    public Result saveComment(@RequestBody CommentDTO commentDTO){
        log.info("添加题解/评论:{}",commentDTO);
        solutionCommentService.save(commentDTO);
        return Result.success();
    }
    @GetMapping("/list")
    @Operation(summary = "滚动分页查询题解")
    public Result<ScrollResult> querySolutions(
            @RequestParam Long problemId,
            @RequestParam Long lastId,
            @RequestParam(defaultValue = "0") Integer offset) {
        log.info("查询题解：problemId={}, lastId={}, offset={}", problemId, lastId, offset);
        return solutionCommentService.querySolutions(problemId, lastId, offset);
    }

    @PostMapping("/newcomment")
    @Operation(summary = "添加评论")
    public Result saveNewComment(@RequestBody CommentDTO commentDTO){
        log.info("添加评论：{}",commentDTO);
        return  solutionCommentService.saveComment(commentDTO);

    }

    @GetMapping("/comments")
    @Operation(summary = "滚动分页查询评论")
    public Result<ScrollResult> queryComments(
            @RequestParam Long problemId,
            @RequestParam Long lastId,
            @RequestParam(defaultValue = "0") Integer offset) {
        log.info("查询评论：solutionId={}, lastId={}, offset={}", problemId, lastId, offset);
        return solutionCommentService.queryComments(problemId, lastId, offset);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询题解详情")
    public Result getSolutionDetail(@PathVariable Long id) {
        log.info("查询题解详情：{}", id);
        return solutionCommentService.getSolutionDetail(id);
    }

}
