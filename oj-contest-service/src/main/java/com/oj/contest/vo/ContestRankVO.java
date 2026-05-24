package com.oj.contest.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestRankVO implements Serializable {
    private Integer rank;
    private Long userId;
    private String username;
    private Integer score;
    private Integer solvedCount;
}
