package com.oj.contest.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WorkSpaceVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private Long userId;
    private String username;
    private String activityType;
    private String title;
    private String description;
    private Long targetId;
    private String targetType;
    private LocalDateTime createTime;
    private String status;
}
