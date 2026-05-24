package com.oj.contest.dto;

import lombok.Data;

@Data
public class CommentDTO {
    private Long id;
    private Integer type;
    private Long problemId;
    private Long parentId;
    private Long replyToUserId;
    private String title;
    private String content;
}
