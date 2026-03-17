package com.oj.dto;

import lombok.Data;

@Data
public class CommentDTO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 类型：1-题解  2-评论
     */
    private Integer type;

    /**
     * 关联题目ID
     */
    private Long problemId;
    /**
     * 父级ID（0表示顶级；评论回复时指向被回复的记录ID）
     */
    private Long parentId;

    /**
     * 被回复的用户ID（仅评论回复时有值）
     */
    private Long replyToUserId;

    /**
     * 标题（题解必填，评论为空）
     */
    private String title;

    /**
     * 正文内容（题解为富文本/Markdown，评论为纯文本）
     */
    private String content;
}
