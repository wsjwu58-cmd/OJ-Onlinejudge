package com.oj.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SolutionVO {
    private Long id;
    private Integer type;
    private Long problemId;
    private Long userId;
    private String username;        // 发布者用户名
    private Long parentId;
    private Long replyToUserId;
    private String replyToUsername;  // 被回复的用户名
    private String title;
    private String content;
    private String contentHtml; //markdown修饰
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
