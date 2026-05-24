package com.oj.contest.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecordTrendVO {
    private String dateList;
    private String turnoverList;
    private String AcProblemList;
    private String PercentList;
}
