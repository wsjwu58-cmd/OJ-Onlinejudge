package com.oj.problem.vo;

import com.oj.problem.entity.Problem;
import lombok.Data;

import java.util.List;

@Data
public class GroupVO {
    private Integer id;
    private String title;
    private String description;
    private Long creatorId;
    private String difficultyRange;
    private Integer estimatedDurationMinutes;
    private Boolean isPublic;
    private Integer status;
    private List<Problem> problemList;
}
