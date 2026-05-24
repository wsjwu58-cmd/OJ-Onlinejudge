package com.oj.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ProblemAcceptanceFeignDTO implements Serializable {
    private Integer id;
    private String title;
    private BigDecimal acceptance;
}
