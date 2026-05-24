package com.oj.problem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProblemAcceptanceVO {
    private Integer id;
    private String title;
    private Double acceptance;
}
