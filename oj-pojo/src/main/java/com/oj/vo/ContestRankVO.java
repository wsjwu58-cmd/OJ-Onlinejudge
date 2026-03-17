package com.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 比赛排行榜单行 VO（按分数排名，无罚时）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestRankVO implements Serializable {

    /** 排名 */
    private Integer rank;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 总得分 */
    private Integer score;

    /** 通过题数 */
    private Integer solvedCount;
}
