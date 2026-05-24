package com.oj.problem.dto;

import com.oj.common.dto.PageQueryDTO;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProblemTypeQueryDTO extends PageQueryDTO implements Serializable {
    private String name;
}
