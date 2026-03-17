package com.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecordTrendVO {
    //日期，以逗号分隔，例如：2022-10-01,2022-10-02,2022-10-03
    private String dateList;

    //提交记录，以逗号分隔
    private String turnoverList;

    //AC题目数
    private String AcProblemList;

    //通过率
    private String PercentList;
}
