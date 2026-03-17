package com.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProblemDataVO {
    /*
    未上架题目
     */
    private Integer disSend;
    /*
    已上架题目
     */
    private Integer send;
}
