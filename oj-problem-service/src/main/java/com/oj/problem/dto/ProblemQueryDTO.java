package com.oj.problem.dto;

import com.oj.common.dto.PageQueryDTO;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProblemQueryDTO extends PageQueryDTO implements Serializable {
    private String title;
    private String difficulty;
    private Integer status;
    private Integer problemTypeId;
}
