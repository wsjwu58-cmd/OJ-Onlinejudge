package com.oj.contest.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContestDataVO {
    private Integer disSend;
    private Integer send;
    private Integer finalSend;
}
