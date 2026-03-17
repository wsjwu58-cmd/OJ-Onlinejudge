package com.oj.vo;

import com.oj.entity.Problem;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContestVO {
    /**
     * 比赛ID
     */
    private Integer id;

    /**
     * 比赛名称
     */
    private String title;

    /**
     * 比赛描述
     */
    private String description;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 比赛类型: Weekly Contest-周赛, Biweekly Contest-双周赛, Mock Interview-模拟面试, Company Contest-企业竞赛
     */
    private String type;

    /**
     * 比赛状态: Upcoming-即将开始, Running-进行中, Ended-已结束
     */
    private String status;

    /**
     * 创建者ID
     */
    private Long createdBy;

    /*
      创建者名称
     */
    private String createdName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    private List<Problem> problemList;

    /**
     * 参赛人数
     */
    private Integer participantCount;

    /**
     * 当前用户是否已报名
     */
    private Boolean registered;

}
