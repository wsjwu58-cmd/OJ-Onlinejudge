package com.oj.contest.dto;

import com.oj.api.dto.ContestProblemInputDTO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContestDTO {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String type;
    private String status;
    private List<ContestProblemInputDTO> problemList;
}
