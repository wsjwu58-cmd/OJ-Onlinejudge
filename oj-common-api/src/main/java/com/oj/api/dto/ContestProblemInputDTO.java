package com.oj.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ContestProblemInputDTO implements Serializable {
    private Integer id;
    private Integer score;
}
