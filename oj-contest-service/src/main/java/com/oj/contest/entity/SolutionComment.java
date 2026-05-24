package com.oj.contest.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("solution_comment")
public class SolutionComment {
    private Long id;
    private Integer type;
    private Long problemId;
    private Long userId;
    private Long parentId;
    private Long replyToUserId;
    private String title;
    private String content;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
