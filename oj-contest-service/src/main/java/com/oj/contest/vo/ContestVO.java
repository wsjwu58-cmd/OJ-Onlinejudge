package com.oj.contest.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContestVO {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String type;
    private String status;
    private Long createdBy;
    private String createdName;
    private LocalDateTime createdAt;
    private List<ContestProblemVO> problemList;
    private Integer participantCount;
    private Boolean registered;

    @Data
    public static class ContestProblemVO {
        private Integer id;
        private String title;
        private String difficulty;
        private BigDecimal acceptance;
        private Integer score;
        private Integer status;
    }
}
