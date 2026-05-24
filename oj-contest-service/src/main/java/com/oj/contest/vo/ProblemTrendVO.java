package com.oj.contest.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProblemTrendVO {
    private String dateList;
    private String turnoverList;
    private String newProblemList;
}
