package com.oj.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("solution_comment")
public class SolutionComment {
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
     * 发布者用户ID
     */
    private Long userId;

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

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数（题解下的评论总数，评论本身为0）
     */
    private Integer commentCount;

    /**
     * 浏览量（仅题解有意义）
     */
    private Integer viewCount;

    /**
     * 状态：0-隐藏/审核中  1-正常  2-置顶
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
