package com.oj.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceActivityFeignDTO implements Serializable {
    private Long userId;
    private String activityType;
    private String title;
    private String description;
    private Long targetId;
    private String targetType;
}
