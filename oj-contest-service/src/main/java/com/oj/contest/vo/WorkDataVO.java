package com.oj.contest.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkDataVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer totalUsers;
    private Integer activeUsersToday;
    private Integer submissionsToday;
    private Integer totalProblems;
    private Double userChange;
    private Double activeChange;
    private Double submissionChange;
    private Double problemChange;
}
