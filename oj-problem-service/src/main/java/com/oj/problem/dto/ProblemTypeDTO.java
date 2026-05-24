package com.oj.problem.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProblemTypeDTO implements Serializable {
    private Integer id;
    private String name;
    private String description;
    private Boolean isActive;
    private Integer sortOrder;
}
