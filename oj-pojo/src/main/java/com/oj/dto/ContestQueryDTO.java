package com.oj.dto;


import lombok.Data;

@Data
public class ContestQueryDTO extends PageQueryDTO {

    /**
     * 比赛状态: Upcoming-即将开始, Running-进行中, Ended-已结束
     */
    private String status;
    /**
     * 比赛名称
     */
    private String title;
}
