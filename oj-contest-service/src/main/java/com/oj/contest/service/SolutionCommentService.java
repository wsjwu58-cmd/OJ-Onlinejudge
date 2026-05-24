package com.oj.contest.service;

import com.oj.common.result.Result;
import com.oj.contest.dto.CommentDTO;
import com.oj.contest.vo.ScrollResult;

public interface SolutionCommentService {
    void save(CommentDTO commentDTO);
    Result<ScrollResult> querySolutions(Long problemId, Long lastId, Integer offset);
    Result saveComment(CommentDTO commentDTO);
    Result<ScrollResult> queryComments(Long problemId, Long lastId, Integer offset);
    Result getSolutionDetail(Long id);
}
