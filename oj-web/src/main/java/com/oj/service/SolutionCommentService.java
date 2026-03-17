package com.oj.service;

import com.oj.dto.CommentDTO;
import com.oj.result.Result;
import com.oj.vo.ScrollResult;

public interface SolutionCommentService {
    void save(CommentDTO commentDTO);

    Result<ScrollResult> querySolutions(Long problemId, Long lastId, Integer offset);

    Result saveComment(CommentDTO commentDTO);

    Result<ScrollResult> queryComments(Long solutionId, Long lastId, Integer offset);

    Result getSolutionDetail(Long id);
}
