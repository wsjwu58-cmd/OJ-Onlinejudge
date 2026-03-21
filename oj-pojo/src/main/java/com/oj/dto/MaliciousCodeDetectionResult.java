package com.oj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaliciousCodeDetectionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean safe;
    private String message;
}