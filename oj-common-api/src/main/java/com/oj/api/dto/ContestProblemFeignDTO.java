package com.oj.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ContestProblemFeignDTO implements Serializable {
    private Integer contestId;
    private Integer problemId;
    private Integer score;
    private Integer sortOrder;
}
