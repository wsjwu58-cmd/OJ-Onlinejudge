package com.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContestDataVO {
    /*
    未开始比赛
     */
    private Integer disSend;
    /*
    进行中比赛
     */
    private Integer send;
    /*
    结束的比赛
     */
    private Integer finalSend;
}
