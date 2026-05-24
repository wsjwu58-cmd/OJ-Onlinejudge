package com.oj.api.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SubmissionCountDTO implements Serializable {
    private Long count;
    private String status;
}
