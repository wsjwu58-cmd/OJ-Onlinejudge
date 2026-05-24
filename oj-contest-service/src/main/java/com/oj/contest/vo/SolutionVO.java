package com.oj.contest.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SolutionVO {
    private Long id;
    private Integer type;
    private Long problemId;
    private Long userId;
    private String username;
    private Long parentId;
    private Long replyToUserId;
    private String replyToUsername;
    private String title;
    private String content;
    private String contentHtml;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
